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
package gedi.riboseq.javapipeline.analyze;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.NavigableMap;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ToDoubleFunction;

import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.data.annotation.ScoreNameAnnotation;
import gedi.core.data.annotation.Transcript;
import gedi.core.genomic.Genomic;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.feature.special.Downsampling;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.riboseq.analysis.MajorIsoform;
import gedi.riboseq.inference.orf.PriceOrf;
import gedi.riboseq.javapipeline.PriceParameterSet;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.SequenceUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.array.SparseMemoryFloatArray;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.functions.IterateIntoSink;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.math.stat.testing.DirichletLikelihoodRatioTest;
import gedi.util.mutable.MutableDouble;
import gedi.util.mutable.MutablePair;
import gedi.util.program.GediProgram;
import gedi.util.program.GediProgramContext;
import gedi.util.userInteraction.progress.Progress;
import jdistlib.Beta;
import jdistlib.math.MathFunctions;

public class PriceIdentifyMajorIsoform extends GediProgram {

	public PriceIdentifyMajorIsoform(PriceParameterSet params) {
		addInput(params.genomic);
		addInput(params.orfs);
		addInput(params.minRpc);
		
		
		addInput(params.prefix);
		
		addOutput(params.majorIsoformCit);
	}
	
	public String execute(GediProgramContext context) throws IOException, InterruptedException {
		
		Genomic g = getParameter(0);
		File orffile = getParameter(1);
		double minRpc = getDoubleParameter(2);
		
		
		PriceParameterSet params = (PriceParameterSet) parameterSet;
		File cfile = params.optcodons.get();
		if (cfile==null) cfile = params.indices.get();
		if (cfile==null) throw new RuntimeException("No codons found!");
		
		CenteredDiskIntervalTreeStorage<PriceOrf> orfs = new CenteredDiskIntervalTreeStorage<>(orffile.getAbsolutePath());
		
		CenteredDiskIntervalTreeStorage<SparseMemoryFloatArray> codons = new CenteredDiskIntervalTreeStorage<>(cfile.getAbsolutePath());
		MemoryIntervalTreeStorage<Transcript> trans = new MemoryIntervalTreeStorage<>(Transcript.class);
		
		trans.fill(g.getTranscripts().ei().filter(r->SequenceUtils.checkCompleteCodingTranscript(g, r)));
		
		context.getLog().info("Identifying major isoforms with all ORFs");
		Progress progress = context.getProgress();
		progress.init();
		int n =0;
		
		CenteredDiskIntervalTreeStorage<MajorIsoform> cit = new CenteredDiskIntervalTreeStorage<>(getOutputFile(0).getPath(),MajorIsoform.class);
		IterateIntoSink<ImmutableReferenceGenomicRegion<MajorIsoform>> sink = new IterateIntoSink<ImmutableReferenceGenomicRegion<MajorIsoform>>(cit::fill);
		
		for (ReferenceSequence ref : trans.getReferenceSequences()) {//Arrays.asList(Chromosome.obtain("14+"))){//cds.getReferenceSequences()) {
			progress.setDescription("Processing gene clusters on "+ref.toString()+" found: "+n);
			
			IntervalTree<GenomicRegion, Transcript> tree = trans.getTree(ref);
			IntervalTree<GenomicRegion, Transcript>.GroupIterator git = tree.groupIterator();
			while (git.hasNext()) {
				IntervalTree<GenomicRegion,Transcript> group = new IntervalTree<>(ref);
				NavigableMap<GenomicRegion, Transcript> gr = git.next();
				for (GenomicRegion r : gr.keySet()) {
					if (SequenceUtils.checkCompleteCodingTranscript(g, new ImmutableReferenceGenomicRegion<>(ref, r,gr.get(r)),10,10,true))
						group.put(r, gr.get(r));
				}
				
				if (!group.isEmpty()) {
			
					// load data into structure
					HashMap<GenomicRegion,MajorIsoform> map = loadData(g,ref,group,codons, orfs);
					
					// identify the maximal amount of reads
					double maxReads = find(map,a->a.getSum(0).sum(),false);
					double thresh = maxReads*0.9;
					
					find(map,a->a.getSum(0).sum()>=thresh?1:0,true);
					double fraction = find(map,a->a.getReadsPerCodon(0).sum(),true);
					find(map,a->a.getSum(0).sum(),true);
					
					
					if (map.size()>0 && fraction>=minRpc){
						GenomicRegion region = map.keySet().iterator().next();
						MajorIsoform isof = map.get(region);
						n++;
						sink.put(new ImmutableReferenceGenomicRegion<>(ref, region, isof));
					}
				}
				
				progress.incrementProgress();
			}
		}
		sink.finish();
		progress.finish();
		cit.setMetaData(orfs.getMetaData());
		
		return null;
	}


	private <K,T> double find(HashMap<K, MajorIsoform> map, ToDoubleFunction<MajorIsoform> fun, boolean removeNonMaximal) {
		HashMap<K,MajorIsoform> re = new HashMap<>();
		double max = Double.NEGATIVE_INFINITY;
		
		for (K k : map.keySet()) {
			double val = fun.applyAsDouble(map.get(k));
			if (val>max) {
				re.clear();
				max = val;
			}
			if (val>=max) re.put(k, map.get(k));
		}
		if (removeNonMaximal) {
			map.clear();
			map.putAll(re);
		}
		return max;
	}

	private HashMap<GenomicRegion, MajorIsoform> loadData(Genomic g, ReferenceSequence ref, IntervalTree<GenomicRegion, Transcript> group, CenteredDiskIntervalTreeStorage<SparseMemoryFloatArray> codons, CenteredDiskIntervalTreeStorage<PriceOrf> orfs) {
		HashMap<GenomicRegion, MajorIsoform> re = new HashMap<>();
		
		MutableReferenceGenomicRegion<Void> trans = new MutableReferenceGenomicRegion<Void>().setReference(ref);
		
		for (ReferenceGenomicRegion<SparseMemoryFloatArray> c : codons.ei(ref,new ArrayGenomicRegion(group.getStart(),group.getEnd())).loop()) {
			for (GenomicRegion tr : group.keys(c.getRegion().getStart(),c.getRegion().getStop()).filter(tr->tr.containsUnspliced(c.getRegion())).loop()) {
				int pos = trans.setRegion(tr).induce(c.getRegion()).getStart();
				MajorIsoform profile = re.computeIfAbsent(tr, x->new MajorIsoform(g,
						new ImmutableReferenceGenomicRegion<>(ref, tr,group.get(tr)),
						orfs.ei(trans).filter(o->trans.getRegion().containsUnspliced(o.getRegion()))
						));
				profile.set(pos,c.getData());
			}
		}
		return re;
	}

	
	

}

