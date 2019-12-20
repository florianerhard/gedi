/**
 * 
 *    Copyright 2017 Florian Erhard
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */
package gedi.macoco.javapipeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.function.Function;
import java.util.function.Supplier;

import gedi.core.data.annotation.NameAnnotation;
import gedi.core.data.annotation.Transcript;
import gedi.core.data.numeric.diskrmq.DiskGenomicNumericBuilder;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.genomic.Genomic;
import gedi.core.reference.Strandness;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.feature.special.Downsampling;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.lfc.localtest.LocalCoverageTest;
import gedi.util.ArrayUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.functions.ParallelizedIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.math.stat.RandomNumbers;
import gedi.util.math.stat.counting.Counter;
import gedi.util.mutable.MutableTriple;
import gedi.util.program.GediProgram;
import gedi.util.program.GediProgramContext;

public class CountEquivalenceClasses extends GediProgram {


	
	public CountEquivalenceClasses(MacocoParameterSet params) {
		addInput(params.nthreads);
		addInput(params.genomic);
		addInput(params.reads);
		addInput(params.mrnas);
		addInput(params.strandness);
		addInput(params.seed);
		addInput(params.down);
		
		addInput(params.prefix);
		
		addOutput(params.countTable);
		addOutput(params.lenDistTable);
	}
	
	
	
	public String execute(GediProgramContext context) throws IOException {
		
		int nthreads = getIntParameter(0);
		Genomic genomic = getParameter(1);
		GenomicRegionStorage<AlignedReadsData> reads = getParameter(2);
		GenomicRegionStorage<NameAnnotation> mRNAs = getParameter(3);
		Strandness strand = getParameter(4);
		long seed = getLongParameter(5);
		Downsampling down = getParameter(6);
		
		MemoryIntervalTreeStorage<Transcript> trans;
		if (mRNAs==null) {
			trans = genomic.getTranscripts();
			
		} else {
			trans = new MemoryIntervalTreeStorage<>(Transcript.class);
			trans.fill(mRNAs.ei().map(r->new ImmutableReferenceGenomicRegion<>(r.getReference(), r.getRegion(),new Transcript(r.getData().getName(), r.getData().getName(), -1, -1))));
		}
		HashMap<String, Integer> map = trans.ei().indexPosition(tr->tr.getData().getTranscriptId());
		
		String[] rmap = new String[map.size()];
		for (String s : map.keySet())
			rmap[map.get(s)] = s;
		
		Function<ImmutableReferenceGenomicRegion<AlignedReadsData>,ExtendedIterator<ImmutableReferenceGenomicRegion<Transcript>>> transSupp;
		
		switch (strand) {
		case Unspecific:
			transSupp= read->trans.ei(read).chain(trans.ei(read.toMutable().toOppositeStrand()));
			break;
		case Sense:
			transSupp= read->trans.ei(read);
			break;
		case Antisense:
			transSupp= read->trans.ei(read.toMutable().toOppositeStrand());
			break;
		default:
			throw new RuntimeException("Illegal Strandness!");
		}
		
		context.getLog().info("Counting reads for equivalence classes (first pass)...");
		
		// first pass: count potentially wrong ecs (internal exon not sequenced in between read pair)
		ParallelizedIterator<ImmutableReferenceGenomicRegion<AlignedReadsData>, Integer, EquivalenceClassCounter> para = reads.ei()
					.progress(context.getProgress(), (int)reads.size(), r->r.toLocationString())
					.parallelized(nthreads, 1024, 
							()->new EquivalenceClassCounter(map),(ei,count)->ei.map(read->{
			for (int d=0; d<read.getData().getDistinctSequences(); d++) {
				for (ImmutableReferenceGenomicRegion<Transcript> tr : transSupp.apply(read).loop()) {
			
					if (read.getData().isConsistentlyContained(read, tr, d)) {
						count.found(tr);
						count.length(tr.induce(read.getRegion().getStart()),tr.induce(read.getRegion().getStop()),read.getData(),d);
					}
				}
				count.finish(read.getData(),d);
			}
			return 1;
		}));
		para.drain();
		
		EquivalenceClassCounter firstpass = para.getState(0);
		for (int i=1; i<para.getNthreads(); i++)
			firstpass.merge(para.getState(i));
		
		
		context.getLog().info("Counting reads for equivalence classes (second pass)...");
		
		// second pass: assign each read to a random transcript (according to implied length and expression value)
		ParallelizedIterator<ImmutableReferenceGenomicRegion<AlignedReadsData>, Integer, MutableTriple<EquivalenceClassCounter, RandomNumbers, DoubleArrayList>> para2 = reads.ei()
				.progress(context.getProgress(), (int)reads.size(), r->r.toLocationString())
				.parallelized(nthreads, 1024, 
						(index)->new MutableTriple<>(
								new EquivalenceClassCounter(map),
								null,
								new DoubleArrayList(1000)),
						(b,triple)->{triple.Item2=new RandomNumbers(b*13+seed);},
						(ei,triple)->ei.map(read->{
			EquivalenceClassCounter count = triple.Item1;
			RandomNumbers rnd = triple.Item2;
			DoubleArrayList buffer = triple.Item3;
			
			NumericArray na = EI.seq(0, read.getData().getNumConditions()).mapToDouble(c->rnd.getBinom(read.getData().getTotalCountForConditionInt(c, ReadCountMode.All), read.getData().getWeight(0))).toNumericArray(); 
			for (int c=0; c<read.getData().getNumConditions(); c++) {
				if (na.sum()>0) {
					na = down.downsample(na);
					
					// probabilistically select transcript
					for (ImmutableReferenceGenomicRegion<Transcript> tr : transSupp.apply(read).loop()) {
						if (read.getData().isConsistentlyContained(read, tr, 0)) {
							double lp = firstpass.getLengthFreq(Math.abs(tr.induce(read.getRegion().getStart())-tr.induce(read.getRegion().getStop()))+1,c);
							double ep = firstpass.getExpression(tr,c);
							buffer.add(lp*ep);
						}
					}
					if (buffer.size()>0) {
						buffer.cumSum(1);
						int selected = rnd.getCategorial(buffer);
						GenomicRegion selReg = null;
						for (ImmutableReferenceGenomicRegion<Transcript> tr : transSupp.apply(read).loop()) {
							if (read.getData().isConsistentlyContained(read, tr, 0) && selected--==0) {
								selReg = tr.getRegion().intersect(read.getRegion().removeIntrons());
								break;							
							}
						}
						count.length(selReg.getTotalLength(),na);							
						for (ImmutableReferenceGenomicRegion<Transcript> tr : transSupp.apply(read).loop()) {
							if (tr.getRegion().containsUnspliced(selReg)) {
								count.found(tr);
							}
						}
						count.finish(na);
						buffer.clear();
					}
				}
				
			}
			return 1;
		}));
		para2.drain();
		
		EquivalenceClassCounter c = para2.getState(0).Item1;
		for (int i=1; i<para2.getNthreads(); i++)
			c.merge(para2.getState(i).Item1);
		
		
		
		
		context.getLog().info("Writing tables...");
		
		LineWriter writer = new LineOrientedFile(getOutputFile(0).getPath()).write();
		writer.write("Equivalence class");
		for (String cond : reads.getMetaDataConditions())
			writer.writef("\t%s",cond);
		writer.writeLine();
		
		for (IntArrayList k : c.counter.keySet()) {
			if (k.size()>0) {
				writer.write(rmap[k.getInt(0)]);
				for (int i=1; i<k.size(); i++)
					writer.writef(",%s",rmap[k.getInt(i)]);
				NumericArray a = c.counter.get(k);
				for (int i=0; i<a.length(); i++) 
					writer.writef("\t%.1f", a.getDouble(i));
				writer.writeLine();
			}
		}
		
		writer.close();
		
		
		writer = new LineOrientedFile(getOutputFile(1).getPath()).write();
		writer.write("Length");
		for (String cond : reads.getMetaDataConditions())
			writer.writef("\t%s",cond);
		writer.writeLine();
		
		ArrayList<Integer> lens = new ArrayList<>(c.lengths.keySet());
		Collections.sort(lens);
		for (Integer k : lens) {
			writer.writef("%d",k);
			NumericArray a = c.lengths.get(k);
			for (int i=0; i<a.length(); i++) 
				writer.writef("\t%.1f", a.getDouble(i));
			writer.writeLine();
		}
		
		writer.close();
		
		
		
		return null;
	}


	private static class EquivalenceClassCounter {

		private HashMap<String,Integer> toIndex;
		private IntArrayList found = new IntArrayList();
		private HashMap<IntArrayList,NumericArray> counter = new HashMap<>();
		
		private HashMap<Integer,NumericArray> perTransCounter = null;
		private HashMap<Integer,NumericArray> lengths = new HashMap<>();
		
		public EquivalenceClassCounter(HashMap<String,Integer> toIndex) {
			this.toIndex = toIndex;
		}

		public double getExpression(ImmutableReferenceGenomicRegion<Transcript> tr, int c) {
			if (perTransCounter==null) {
				perTransCounter = new HashMap<Integer, NumericArray>();
				for (IntArrayList e : counter.keySet()) {
					NumericArray na = counter.get(e);
					e.iterator().forEachRemaining(tri->{
						perTransCounter.computeIfAbsent(tri, x->NumericArray.createMemory(na.length(), na.getType())).add(na);
					});
				}
			}
			NumericArray n = perTransCounter.get(toIndex.get(tr.getData().getTranscriptId()));
			if (n==null) return 0;
			return n.getDouble(c);
		}

		public double getLengthFreq(int len, int cond) {
			NumericArray n = lengths.get(len);
			if (n==null) return 0;
			return n.getDouble(cond);
		}

		public void length(int s1, int s2, AlignedReadsData read, int d) {
			int l = Math.abs(s1-s2)+1;
			
			NumericArray a = lengths.get(l);
			if (a==null) {
				a = NumericArray.createMemory(read.getNumConditions(), NumericArrayType.Double);
				lengths.put(l, a);
			}
			read.addCountsForDistinct(d, a, ReadCountMode.Weight);
		}
		
		public void length(int l, NumericArray na) {
			NumericArray a = lengths.get(l);
			if (a==null) {
				a = NumericArray.createMemory(na.length(), NumericArrayType.Double);
				lengths.put(l, a);
			}
			a.add(na);
		}

		public void finish(AlignedReadsData read, int d) {
			found.sort();
			NumericArray a = counter.get(found);
			if (a==null) {
				a = NumericArray.createMemory(read.getNumConditions(), NumericArrayType.Double);
				counter.put(found.clone(), a);
			}
			read.addCountsForDistinct(d, a, ReadCountMode.Weight);
			found.clear();
		}
		
		public void finish(NumericArray na) {
			found.sort();
			NumericArray a = counter.get(found);
			if (a==null) {
				a = NumericArray.createMemory(na.length(), NumericArrayType.Double);
				counter.put(found.clone(), a);
			}
			a.add(na);
			found.clear();
		}
		
		public void found(ImmutableReferenceGenomicRegion<Transcript> tr) {
			found.add(toIndex.get(tr.getData().getTranscriptId()));
		}
		
		public void merge(EquivalenceClassCounter other) {
			for (Integer k : other.lengths.keySet()) {
				NumericArray a = lengths.get(k);
				if (a==null) 
					lengths.put(k, other.lengths.get(k));
				else
					a.add(other.lengths.get(k));
			}
			for (IntArrayList k : other.counter.keySet()) {
				NumericArray a = counter.get(k);
				if (a==null) 
					counter.put(k, other.counter.get(k));
				else
					a.add(other.counter.get(k));
			}
				
		}

		
	}
}
