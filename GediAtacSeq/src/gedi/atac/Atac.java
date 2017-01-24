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

package gedi.atac;

import gedi.core.data.annotation.Transcript;
import gedi.core.data.numeric.GenomicNumericProvider.PositionNumericIterator;
import gedi.core.data.numeric.diskrmq.DiskGenomicNumericBuilder;
import gedi.core.data.numeric.diskrmq.DiskGenomicNumericProvider;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ContrastMapping;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.processing.GenomicRegionProcessor;
import gedi.core.processing.ProcessorContext;
import gedi.core.processing.sources.ProcessorSource;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.ReferenceSequenceConversion;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegionPosition;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.util.ArrayUtils;
import gedi.util.FunctorUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.text.LineIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.mutable.MutableMonad;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.Progress;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.special.Gamma;

public class Atac {

	
	public static void analyzeNfkb(DiskGenomicNumericProvider rmq, 
			GenomicRegionStorage<?> tfbs) throws Exception {
		
		int size = tfbs.getRandomEntry().getRegion().getTotalLength();
		double[] buff = new double[size+200];
		
		
		
		tfbs.iterateReferenceGenomicRegions().forEachRemaining(rgr->{
			if (rgr.getRegion().getTotalLength()!=size || rgr.getRegion().getNumParts()!=1) throw new RuntimeException();
			
			ArrayGenomicRegion reg = rgr.getRegion().extendAll(100,100);
			PositionNumericIterator it = rmq.iterateValues(rgr.getReference().toPlusStrand(), reg);
			while (it.hasNext()) buff[it.nextInt()-reg.getStart()]+=it.getValue(0);
			it = rmq.iterateValues(rgr.getReference().toMinusStrand(), reg);
			while (it.hasNext()) buff[it.nextInt()-reg.getStart()]+=it.getValue(0);
			
			System.out.printf("%s:%s",rgr.getReference().toStrandIndependent(),rgr.getRegion().toRegionString());
			for (int i=0; i<buff.length; i++) 
				System.out.printf("\t%.0f",buff[i]);
			System.out.println();
			Arrays.fill(buff, 0);
		});
		
	}
	
	
	public static void analyzePromotors(GenomicRegionStorage<AlignedReadsData> storage, 
			GenomicRegionStorage<String> promotors) throws Exception {
		
		int offset = 4;
		int numCond = storage.getRandomRecord().getNumConditions();
		
		ProcessorSource<String> p = new ProcessorSource<String>();
		p.setProgress(new ConsoleProgress());
		p.process(storage,ReferenceSequenceConversion.none,promotors,new GenomicRegionProcessor() {
			
			int[][] tn5;
			LineOrientedFile[] out = new LineOrientedFile[numCond];
			
			@Override
			public void begin(ProcessorContext context) throws Exception {
				for (int i=0; i<out.length; i++) {
					out[i] = new LineOrientedFile("promotor."+storage.getMetaData().get("conditions").getEntry(i).getEntry("name").asString()+".csv");
					out[i].startWriting();
				}
			}
			
			@Override
			public void beginRegion(MutableReferenceGenomicRegion<?> region,
					ProcessorContext context) throws Exception {
				if (tn5==null || tn5[0].length!=region.getRegion().getTotalLength())
					tn5 = new int[numCond][region.getRegion().getTotalLength()];
				else
					for (int i=0; i<tn5.length; i++)
						Arrays.fill(tn5[i],0);
			}
			
			@Override
			public void read(MutableReferenceGenomicRegion<?> region,
					MutableReferenceGenomicRegion<AlignedReadsData> read,
					ProcessorContext context) throws Exception {
				
				for (int c=0; c<numCond; c++) {
					int v = read.getData().getTotalCountForConditionInt(c, ReadCountMode.All);
					addValue(region,GenomicRegionPosition.Start.position(read.getReference(), read.getRegion(), offset), c, v);
					addValue(region,GenomicRegionPosition.Stop.position(read.getReference(), read.getRegion(), -offset), c, v);
				}
			}
			
			private void addValue(MutableReferenceGenomicRegion<?> region, int position, int condition, int value) {
				if (region.getRegion().contains(position)) {
					position = region.getRegion().induce(position);
					if (region.getReference().getStrand()==Strand.Minus)
						position = region.getRegion().getTotalLength()-1-position;
					tn5[condition][position]+=value;
				}
			}

			@Override
			public void endRegion(MutableReferenceGenomicRegion<?> region,
					ProcessorContext context) throws Exception {
				
				for (int i=0; i<out.length; i++) {
					out[i].writef(region.getData().toString());
					for (int p=0; p<tn5[i].length; p++) 
						out[i].writef("\t%d",tn5[i][p]);
					out[i].writeLine();
				}
				
				
			}
			
			@Override
			public void end(ProcessorContext context) throws Exception {
				for (int i=0; i<out.length; i++) {
					out[i].finishWriting();
				}
			}
		});
		
		
		
	}
	
	public static void buildFseqBed(String path, GenomicRegionStorage<? extends AlignedReadsData> storage) throws IOException {

		int offset = 4;
		LineOrientedFile out = new LineOrientedFile(path);
		out.startWriting();
		ConsoleProgress progress = new ConsoleProgress();
		progress.init();
		
		storage.iterateMutableReferenceGenomicRegions(Chromosome.obtain("chr9")).forEachRemaining(new Consumer<MutableReferenceGenomicRegion<? extends AlignedReadsData>>() {

			@Override
			public void accept(
					MutableReferenceGenomicRegion<? extends AlignedReadsData> mrgr) {
				try {
					int v = mrgr.getData().getTotalCountOverallInt(ReadCountMode.All);
					if (v!=0) {
						writeBed(out,mrgr.getReference(),GenomicRegionPosition.Start.position(mrgr.getReference(), mrgr.getRegion(), offset), "+", v);
						writeBed(out,mrgr.getReference(),GenomicRegionPosition.Stop.position(mrgr.getReference(), mrgr.getRegion(), -offset), "-", v);
					}
					progress.setDescription(mrgr.getReference()+":"+mrgr.getRegion());
					progress.incrementProgress();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}				
			}
			
		});

		progress.finish();
		out.finishWriting();
	}
	
	private static void writeBed(LineOrientedFile out, ReferenceSequence ref, int pos, String strand, int c) throws IOException {
		for (int i=0; i<c; i++)
			out.writef("%s\t%d\t%d\t.\t.\t%s\n", ref.getName(),pos,pos+25,strand);
	}
	
	public static void buildInsertionIndices(String prefix, GenomicRegionStorage<? extends AlignedReadsData> storage) throws IOException {

		int ncond = storage.getRandomRecord().getNumConditions();
		for (int i=0; i<ncond; i++) {
			String name = storage.getMetaData().get("conditions").getEntry(i).getEntry("name").asString();
			if (name==null || name.length()==0) name = i+"";
			
			String path = prefix+"."+name+".rmq";
			if (!new File(path).exists()) {
				System.out.println("Building "+name+" into: "+path);
				buildInsertionIndex(path, storage, i, true, true);
			} else {
				System.out.println("Skip "+name+", file already exists: "+path);
			}
			
		}
	}

	public static void buildInsertionIndex(String path, GenomicRegionStorage<? extends AlignedReadsData> storage, int condition, boolean start, boolean stop) throws IOException {

		DiskGenomicNumericBuilder build = new DiskGenomicNumericBuilder(path);
		int offset = 4;
		build.setReferenceSorted(true);
		
		storage.ei().progress(new ConsoleProgress(),(int)storage.size(),r->r.toLocationStringRemovedIntrons()).forEachRemaining(new Consumer<ImmutableReferenceGenomicRegion<? extends AlignedReadsData>>() {

			@Override
			public void accept(
					ImmutableReferenceGenomicRegion<? extends AlignedReadsData> mrgr) {
				try {
					int v = mrgr.getData().getTotalCountForConditionInt(condition, ReadCountMode.All);
					if (v!=0) {
						if (start) 
							build.addValue(mrgr.getReference(), GenomicRegionPosition.Start.position(mrgr.getReference(), mrgr.getRegion(), offset), v);
						if (stop) 
							build.addValue(mrgr.getReference(), GenomicRegionPosition.Stop.position(mrgr.getReference(), mrgr.getRegion(), -offset), v);
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}				
			}
			
		});

		build.build();
	}
	
	public static void buildInsertionIndex(String path, GenomicRegionStorage<? extends AlignedReadsData> storage) throws IOException {

		DiskGenomicNumericBuilder build = new DiskGenomicNumericBuilder(path);
		int offset = 4;
		build.setReferenceSorted(true);
		
		TreeSet<String> refs = new TreeSet<String>();
		storage.getReferenceSequences().forEach(r->refs.add(r.getName()));
		ConsoleProgress p = new ConsoleProgress();
		
		Consumer<MutableReferenceGenomicRegion<? extends AlignedReadsData>> adder = new Consumer<MutableReferenceGenomicRegion<? extends AlignedReadsData>>() {

			@Override
			public void accept(
					MutableReferenceGenomicRegion<? extends AlignedReadsData> mrgr) {
				try {
					int v = mrgr.getData().getTotalCountOverallInt(ReadCountMode.All);
					if (v>0) {
						build.addValue(mrgr.getReference().toStrandIndependent(), GenomicRegionPosition.Start.position(mrgr.getReference(), mrgr.getRegion(), offset), v);
						build.addValue(mrgr.getReference().toStrandIndependent(), GenomicRegionPosition.Stop.position(mrgr.getReference(), mrgr.getRegion(), -offset), v);
					}
					p.incrementProgress();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}				
			}
			
		};
		
		for (String n : refs) {
			p.init();
			p.setDescription(n+"+");
			storage.iterateMutableReferenceGenomicRegions(Chromosome.obtain(n, Strand.Plus)).forEachRemaining(adder);
			p.setDescription(n+"-");
			storage.iterateMutableReferenceGenomicRegions(Chromosome.obtain(n, Strand.Minus)).forEachRemaining(adder);
			p.finish();
		}

		build.build();
	}

	
	public static void perChromosomeAndLengthStatistics(GenomicRegionStorage<? extends AlignedReadsData> storage, String chrPath, String lenPath) throws IOException {

		LineOrientedFile chr = new LineOrientedFile(chrPath);
		LineOrientedFile len = new LineOrientedFile(lenPath);
		chr.startWriting();
		len.startWriting();
		
		chr.write("chromosome");
		len.write("Length");
		for (DynamicObject cond:storage.getMetaData().get("conditions").asArray()) {
			chr.writef("\t%s", cond.getEntry("name").asString());
			len.writef("\t%s", cond.getEntry("name").asString());
		}
		chr.writeLine();
		len.writeLine();

		IntArrayList[] lenHisto = new IntArrayList[storage.getRandomRecord().getNumConditions()];
		for (int i=0; i<lenHisto.length; i++)
			lenHisto[i] = new IntArrayList();
		
		for (ReferenceSequence ref : storage.getReferenceSequences()) {
			
			MutableMonad<long[]> count = new MutableMonad<long[]>();
			
			storage.iterateMutableReferenceGenomicRegions(ref).forEachRemaining(new Consumer<MutableReferenceGenomicRegion<? extends AlignedReadsData>>() {
	
				@Override
				public void accept(
						MutableReferenceGenomicRegion<? extends AlignedReadsData> mrgr) {
					if (count.Item==null) count.Item = new long[mrgr.getData().getNumConditions()];
					for (int d=0; d<mrgr.getData().getDistinctSequences(); d++)
//						if (mrgr.getData().getMultiplicity(d)==1)
							for (int i=0; i<count.Item.length; i++) {
								count.Item[i] += mrgr.getData().getCount(d,i)>0?1:0;
								lenHisto[i].increment(mrgr.getRegion().getTotalLength(),mrgr.getData().getCount(d,i)>0?1:0);
							}
				}
				
			});

			if (count.Item!=null) {
				chr.write(ref.toString());
				for (int i=0; i<count.Item.length; i++) 
					chr.writef("\t%s",count.Item[i]);
				chr.writeLine();
			}
			
		}
		
		int maxLength = 0;
		for (int i=0; i<lenHisto.length; i++)
			maxLength = Math.max(maxLength,lenHisto[i].size()-1);
		
		for (int l=1; l<=maxLength; l++) {
			len.writef("%d",l);
			for (int i=0; i<lenHisto.length; i++) 
				len.writef("\t%d",lenHisto[i].getInt(l));
			len.writeLine();
		}
		chr.finishWriting();
		len.finishWriting();
	}
	
	
	public static void lengthPerTypeStatistics(GenomicRegionStorage<? extends AlignedReadsData> storage, String out, String aggOut, String... typePattern) throws IOException {

		
		IntArrayList[][] lenHisto = new IntArrayList[typePattern.length][storage.getRandomRecord().getNumConditions()];
		for (int t=0; t<lenHisto.length; t++)
			for (int i=0; i<lenHisto[t].length; i++)
				lenHisto[t][i] = new IntArrayList();
		
		Pattern[] types = new Pattern[typePattern.length];
		for (int i=0; i<types.length; i++)
			types[i] = Pattern.compile(typePattern[i]);
		
		
		for (ReferenceSequence ref : storage.getReferenceSequences()) {
			
			int ty = 0;
			for (; ty<types.length && !types[ty].matcher(ref.toPlusMinusString()).find(); ty++);
			
			if (ty<types.length)
				System.out.println(ref+" -> "+types[ty]);
			else
				System.out.println("Skipping "+ref);
			MutableMonad<long[]> count = new MutableMonad<long[]>();
			
			int tyind = ty;
			if (ty<types.length)
				storage.iterateMutableReferenceGenomicRegions(ref).forEachRemaining(new Consumer<MutableReferenceGenomicRegion<? extends AlignedReadsData>>() {
	
					@Override
					public void accept(
							MutableReferenceGenomicRegion<? extends AlignedReadsData> mrgr) {
						if (count.Item==null) count.Item = new long[mrgr.getData().getNumConditions()];
					
						for (int i=0; i<count.Item.length; i++) {
							count.Item[i] += mrgr.getData().getTotalCountForConditionInt(i, ReadCountMode.All);
							lenHisto[tyind][i].increment(mrgr.getRegion().getTotalLength(),mrgr.getData().getTotalCountForConditionInt(i, ReadCountMode.All));
						}
					}
					
				});

			
		}
		
		LineOrientedFile o = new LineOrientedFile(out);
		o.startWriting();
		
		o.write("Type\tLength");
		for (DynamicObject cond:storage.getMetaData().get("conditions").asArray()) {
			o.writef("\t%s", cond.getEntry("name").asString());
		}
		o.writeLine();

		int maxLength = 0;
		for (int t=0; t<lenHisto.length; t++)
			for (int i=0; i<lenHisto.length; i++)
				maxLength = Math.max(maxLength,lenHisto[t][i].size()-1);
		
		for (int t=0; t<lenHisto.length; t++)
			for (int l=1; l<=maxLength; l++) {
				o.writef("%s\t%d",typePattern[t],l);
				for (int i=0; i<lenHisto[t].length; i++) 
					o.writef("\t%d",lenHisto[t][i].getInt(l));
				o.writeLine();
			}
		o.finishWriting();
		
		
		o = new LineOrientedFile(aggOut);
		o.startWriting();
		
		o.write("Type");
		for (DynamicObject cond:storage.getMetaData().get("conditions").asArray()) {
			o.writef("\t%s", cond.getEntry("name").asString());
		}
		o.writeLine();

		for (int t=0; t<lenHisto.length; t++) {
			o.writef("%s",typePattern[t]);
			for (int i=0; i<lenHisto[t].length; i++) {
				long sum = 0;
				for (int l=1; l<=maxLength; l++)
					sum+=lenHisto[t][i].getInt(l);
				o.writef("\t%d",sum);
			}
			o.writeLine();
		}
		o.finishWriting();
	}

	
	
	public static void normalizationFactors(GenomicRegionStorage<? extends AlignedReadsData> storage, GenomicRegionStorage<?> peaks, String out,String peakout, String detailout, String... typePattern) throws IOException {

		int cond = storage.getRandomRecord().getNumConditions();
		int[][] allCounts = new int[typePattern.length][cond];
		int[][] peakCounts = new int[typePattern.length][cond];
		
		Pattern[] types = new Pattern[typePattern.length];
		for (int i=0; i<types.length; i++)
			types[i] = Pattern.compile(typePattern[i]);
		
		
		new LineOrientedFile(detailout).delete();
		
		
		Set<ReferenceSequence> refs = new TreeSet<ReferenceSequence>();
		for (ReferenceSequence ref : storage.getReferenceSequences())
			refs.add(ref.toStrandIndependent());
		
		for (ReferenceSequence ref : refs) {
			
			int ty = 0;
			for (; ty<types.length && !types[ty].matcher(ref.toPlusMinusString()).find(); ty++);
			
			if (ty<types.length)
				System.out.println(ref+" -> "+types[ty]);
			else
				System.out.println("Skipping "+ref);
			
			HashMap<ImmutableReferenceGenomicRegion<?>,int[]> detail = new HashMap<ImmutableReferenceGenomicRegion<?>,int[]>();
			
			
			int tyind = ty;
			Consumer<MutableReferenceGenomicRegion<? extends AlignedReadsData>> adder = new Consumer<MutableReferenceGenomicRegion<? extends AlignedReadsData>>() {
				
				@Override
				public void accept(
						MutableReferenceGenomicRegion<? extends AlignedReadsData> mrgr) {
					
					int f = GenomicRegionPosition.Start.position(ref,mrgr.getRegion(),4);
					int b = GenomicRegionPosition.Stop.position(ref,mrgr.getRegion(),-4);
					
					int inpeak = 0;
					if (StreamSupport.stream(
							peaks.iterateIntersectingMutableReferenceGenomicRegions(
									ref.toStrandIndependent(), 
									f,f+1)
									,false)
							.peek(peak->{
								int[] c = detail.computeIfAbsent(peak.toImmutable(), x->new int[cond]);
								for (int i=0; i<c.length; i++)
									c[i] += mrgr.getData().getTotalCountForConditionInt(i, ReadCountMode.All);
							})
							.count()>0) inpeak++;
					
					if (StreamSupport.stream(
							peaks.iterateIntersectingMutableReferenceGenomicRegions(
									ref.toStrandIndependent(), 
									b,b+1)
									,false)
							.peek(peak->{
								int[] c = detail.computeIfAbsent(peak.toImmutable(), x->new int[cond]);
								for (int i=0; i<c.length; i++)
									c[i] += mrgr.getData().getTotalCountForConditionInt(i, ReadCountMode.All);
							})
							.count()>0) inpeak++;
					
					for (int i=0; i<allCounts[tyind].length; i++) {
						allCounts[tyind][i] += mrgr.getData().getTotalCountForConditionInt(i, ReadCountMode.All);
						if (inpeak>0)
							peakCounts[tyind][i] += mrgr.getData().getTotalCountForConditionInt(i, ReadCountMode.All)*inpeak;
					}
				}
				
			};
			if (ty<types.length) {
				storage.iterateMutableReferenceGenomicRegions(ref).forEachRemaining(adder);
				storage.iterateMutableReferenceGenomicRegions(ref.toPlusStrand()).forEachRemaining(adder);
				storage.iterateMutableReferenceGenomicRegions(ref.toMinusStrand()).forEachRemaining(adder);
			}
			
			LineOrientedFile d = new LineOrientedFile(detailout);
			if (d.exists())
				d.startAppending();
			else {
				d.startWriting();
				d.write("Peak\tType");
				for (int i=0; i<cond; i++)
					d.writef("\t%d",i);
				d.writeLine();
			}
			
			for (ImmutableReferenceGenomicRegion<?> peak : detail.keySet()) {
				int[] count = detail.get(peak);
				d.writef("%s\t%s", peak.toLocationString(),typePattern[ty]);
				for (int c=0; c<cond; c++) 
					d.writef("\t%d",count[c]);
				d.writeLine();
			}
			d.finishWriting();
			
		}
		
		
		LineOrientedFile o = new LineOrientedFile(out);
		o.startWriting();
		o.write("Type\tCondition Index\tCount\n");
		for (int i=0; i<types.length; i++) {
			for (int c=0; c<allCounts[i].length; c++) {
				o.writef("%s\t%d\t%d\n", typePattern[i], c, allCounts[i][c]);
			}
		}
		o.finishWriting();
		
		
		o = new LineOrientedFile(peakout);
		o.startWriting();
		o.write("Type\tCondition Index\tCount\n");
		for (int i=0; i<types.length; i++) {
			for (int c=0; c<allCounts[i].length; c++) {
				o.writef("%s\t%d\t%d\n", typePattern[i], c, peakCounts[i][c]);
			}
		}
		o.finishWriting();
	}

	private enum PeakAnnotation {
		Intergenic, GeneBody, Promotor, BidirectionalPromotor
	}
	public static void annotatePeaks(GenomicRegionStorage<Transcript> genes,GenomicRegionStorage<Transcript> trans, String peakFile, String out) throws IOException {
		LineIterator it = new LineOrientedFile(peakFile).lineIterator();
		LineOrientedFile o = new LineOrientedFile(out);
		o.startWriting();
		o.writef("%s\tType\tGene\n",it.next());
		
		while (it.hasNext()) {
			String line = it.next();
			ImmutableReferenceGenomicRegion<Object> rgr = ImmutableReferenceGenomicRegion.parse(StringUtils.splitField(line, '\t', 0));
			
			
			ArrayList<ImmutableReferenceGenomicRegion<Transcript>> inter = new ArrayList<ImmutableReferenceGenomicRegion<Transcript>>();
			genes.getReferenceRegionsIntersecting(rgr.getReference().toPlusStrand(), rgr.getRegion(), inter);
			genes.getReferenceRegionsIntersecting(rgr.getReference().toMinusStrand(), rgr.getRegion(), inter);
			trans.getReferenceRegionsIntersecting(rgr.getReference().toPlusStrand(), rgr.getRegion(), inter);
			trans.getReferenceRegionsIntersecting(rgr.getReference().toMinusStrand(), rgr.getRegion(), inter);
			
			PeakAnnotation a = inter.size()>0?PeakAnnotation.GeneBody:PeakAnnotation.Intergenic;
			Transcript tp = null;
			Transcript tm = null;
			
			for (ImmutableReferenceGenomicRegion<Transcript> tr : inter) {
				if (rgr.getRegion().contains(GenomicRegionPosition.FivePrime.position(tr))) {
					if (a==PeakAnnotation.GeneBody)
						tp = tm = null;
					
					a = PeakAnnotation.Promotor;
					if (tr.getReference().getStrand()==Strand.Plus)
						tp = tr.getData();
					else
						tm = tr.getData();
				}
				else if (a==PeakAnnotation.GeneBody){
					if (tr.getReference().getStrand()==Strand.Plus)
						tp = tr.getData();
					else
						tm = tr.getData();
				}
			}
			
			if (tp!=null && tm!=null && a==PeakAnnotation.Promotor)
				a = PeakAnnotation.BidirectionalPromotor;
			StringBuilder gene = new StringBuilder();
			if (tp!=null) gene.append(tp.getGeneId());
			if (tm!=null && tp==null) gene.append(tm.getGeneId());
			else if (tm!=null && tp!=null && a==PeakAnnotation.BidirectionalPromotor) gene.append(", ").append(tm.getGeneId());
			
			
			o.writef("%s\t%s\t%s\n", line, a.toString(), gene);
		}
		
		
		o.finishWriting();
	}
	
	
	public static void testInPeaks(GenomicRegionStorage<? extends AlignedReadsData> storage, 
			String contrasts, String peakFile, String rmq, String compOut,
			String bicOut, String out, boolean randomizeContrasts) throws IOException {
		
		DiskGenomicNumericBuilder clusterRmq = new DiskGenomicNumericBuilder(rmq);
		LineIterator it = new LineOrientedFile(peakFile).lineIterator();
		LineOrientedFile o = new LineOrientedFile(out);
		o.startWriting();
		o.writef("%s\tComponents\tp.value\n",it.next());
		int offset = 4;
		
		
		ContrastMapping contr = new ContrastMapping();
		ExtendedIterator<String> coit = new LineOrientedFile(contrasts).lineIterator();
		if (randomizeContrasts) {
			String[] ca = coit.toArray(new String[0]);
			ArrayUtils.shuffleSlice(ca, 0, ca.length);
			coit = FunctorUtils.arrayIterator(ca);
		}
		coit.forEachRemaining(l->contr.addMapping(contr.getNumOriginalConditions(), contr.getMappedIndexOrNext(l), l));

		LineOrientedFile co = new LineOrientedFile(compOut);
		co.startWriting();
		co.writef("Peak\tComponent");
		for (int i=0; i<contr.getNumMergedConditions(); i++)
			co.writef("\t%s", contr.getMappedName(i));
		co.writeLine();
		
		
		LineOrientedFile bico = new LineOrientedFile(bicOut);
		bico.startWriting();
		bico.writef("Peak\tk\tBIC\n");
		
		Progress pr = new ConsoleProgress();
		pr.init();
		int peakCount = (int) new LineOrientedFile(peakFile).lineIterator().count()-1;
		pr.setCount(peakCount);
		
		while (it.hasNext()) {
			String line = it.next();
			ImmutableReferenceGenomicRegion<Object> peak = ImmutableReferenceGenomicRegion.parse(StringUtils.splitField(line, '\t', 0));
			
			pr.setDescription(peak.toString());
			pr.incrementProgress();
			
			HashMap<FixedDoublePoint, Integer> pToPos = new HashMap<FixedDoublePoint, Integer>(); 
			FixedDoublePoint[] m = new FixedDoublePoint[peak.getRegion().getTotalLength()];
			for (int i=0; i<m.length; i++) {
					m[i] = new FixedDoublePoint(new double[contr.getNumMergedConditions()]);
					pToPos.put(m[i], peak.getRegion().map(i));
			}
			
			Consumer<MutableReferenceGenomicRegion<? extends AlignedReadsData>> adder = new Consumer<MutableReferenceGenomicRegion<? extends AlignedReadsData>>() {

				@Override
				public void accept(
						MutableReferenceGenomicRegion<? extends AlignedReadsData> mrgr) {
					try {
						
						int start = GenomicRegionPosition.Start.position(mrgr.getReference(), mrgr.getRegion(), offset);
						if (peak.getRegion().contains(start)) 
							addDownsampled(contr,m[peak.getRegion().induce(start)].getPoint(), mrgr.getData().getTotalCountsForConditions(ReadCountMode.All));
						
						int stop = GenomicRegionPosition.Stop.position(mrgr.getReference(), mrgr.getRegion(), -offset);
						if (peak.getRegion().contains(stop))
							addDownsampled(contr,m[peak.getRegion().induce(stop)].getPoint(), mrgr.getData().getTotalCountsForConditions(ReadCountMode.All));
					} catch (Exception e) {
						throw new RuntimeException(e);
					}				
				}

				private void addDownsampled(ContrastMapping contr, double[] re,
						double[] c) {
					double max = ArrayUtils.max(c);
					if (max>0)
						ArrayUtils.mult(c, 1/max);
					for (int i=0; i<c.length; i++)
						if (contr.getMappedIndex(i)>-1)
							re[contr.getMappedIndex(i)]+=c[i];
				}
			};
			storage.iterateIntersectingMutableReferenceGenomicRegions(peak.getReference().toPlusStrand(), peak.getRegion()).forEachRemaining(adder);
			storage.iterateIntersectingMutableReferenceGenomicRegions(peak.getReference().toMinusStrand(), peak.getRegion()).forEachRemaining(adder);
			
			
//			double[] total = new double[cond];
//			for (int i=0; i<m.length; i++) 
//				for (int j=0; j<cond; j++)
//					total[j]+=m[i].getPoint()[j];
//			ArrayUtils.normalize(total);
//			
//			double ll = 0;
//			for (int i=0; i<m.length; i++)
//				ll+=ddirichlet1(m[i].getPoint(), total);
//			
			
			DoubleArrayList ll = new DoubleArrayList();
			ll.add(0);
			DoubleArrayList bic = new DoubleArrayList();
			bic.add(0);
			
			ArrayList<FixedDoublePoint> list = new ArrayList<FixedDoublePoint>();
			for (FixedDoublePoint p : m)
				if (ArrayUtils.sum(p.getPoint())>0)
					list.add(p);
			
			List<CentroidCluster<FixedDoublePoint>> ocl = null;
			double op = 0;
			
			for (int k=1; k<Math.min(list.size(),50); k++) {
			
				KMeansPlusPlusClusterer<FixedDoublePoint> kmeans = new KMeansPlusPlusClusterer<FixedDoublePoint>(k);
				List<CentroidCluster<FixedDoublePoint>> cl = kmeans.cluster(list);
				
				double cll = 0;
				for (CentroidCluster<FixedDoublePoint> c : cl) {
					double[] total = new double[contr.getNumMergedConditions()];
					Arrays.fill(total, 1);
					for (FixedDoublePoint p : c.getPoints()) 
						for (int j=0; j<contr.getNumMergedConditions(); j++)
							total[j]+=p.getPoint()[j];
					ArrayUtils.normalize(total);
					
					
					for (FixedDoublePoint p : c.getPoints()) 
						cll+=ddirichlet1(p.getPoint(), total);	
				}
				
				// LLR test
				double LLR = 2*cll - 2*ll.getLastDouble();
				double p = 1-new ChiSquaredDistribution(contr.getNumMergedConditions()-1).cumulativeProbability(LLR);
			
				bic.add(-2*cll+2*(contr.getNumMergedConditions()-1)*k);
				bico.writef("%s\t%d\t%.1f\n", peak.toLocationString(), k, bic.getLastDouble());
				
				// bonferroni correction
				p = p*peakCount;
				
				if (p>0.01) {
					if (ocl.size()>1) {
						for (int i=0; i<ocl.size(); i++) {
							co.writef("%s\t%d", peak.toLocationString(), i);
							double[] total = new double[contr.getNumMergedConditions()];
							Arrays.fill(total, 1);
							for (FixedDoublePoint pp : ocl.get(i).getPoints()) {
								clusterRmq.addValue(peak.getReference(), pToPos.get(pp).intValue(), (byte)i);
								for (int j=0; j<contr.getNumMergedConditions(); j++)
									total[j]+=pp.getPoint()[j];
							}
							ArrayUtils.normalize(total);
							for (int c=0; c<contr.getNumMergedConditions(); c++)
								co.writef("\t%.4f", total[c]);
							co.writeLine();
							
						}
					}
					break;
				}
				
				ll.add(cll);
				ocl = cl;
				op = p;
			}
			
			o.writef("%s\t%d\t%.4g\n", line, ll.size()-1, ll.size()==2?Double.NaN:op);
		}
		
		
		pr.finish();
		o.finishWriting();
		co.finishWriting();

		clusterRmq.build();
	}
	

	private static double ddirichlet1(double[] alpha1, double[] p) {
		double re = 0;
		double asum = 0;
		double gsum = 0;
		for (int i=0; i<p.length; i++) {
			re+=Math.log(p[i])*(alpha1[i]);
			asum+=alpha1[i]+1;
			gsum+=Gamma.logGamma(alpha1[i]+1);
		}
		return re+Gamma.logGamma(asum)-gsum;
	}
	
	private static class FixedDoublePoint implements Clusterable, Serializable {

	    /** Serializable version identifier. */
	    private static final long serialVersionUID = 3946024775784901369L;

	    /** Point coordinates. */
	    private final double[] point;

	    /**
	     * Build an instance wrapping an double array.
	     * <p>
	     * The wrapped array is referenced, it is <em>not</em> copied.
	     *
	     * @param point the n-dimensional point in double space
	     */
	    public FixedDoublePoint(final double[] point) {
	        this.point = point;
	    }

	    /**
	     * Build an instance wrapping an integer array.
	     * <p>
	     * The wrapped array is copied to an internal double array.
	     *
	     * @param point the n-dimensional point in integer space
	     */
	    public FixedDoublePoint(final int[] point) {
	        this.point = new double[point.length];
	        for ( int i = 0; i < point.length; i++) {
	            this.point[i] = point[i];
	        }
	    }

	    /** {@inheritDoc} */
	    public double[] getPoint() {
	        return point;
	    }


	    /** {@inheritDoc} */
	    @Override
	    public String toString() {
	        return Arrays.toString(point);
	    }

	}
}
