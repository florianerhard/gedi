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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.NavigableMap;

import org.apache.commons.math3.stat.descriptive.rank.Median;

import gedi.core.data.annotation.ScoreAnnotation;
import gedi.core.data.numeric.diskrmq.DiskGenomicNumericBuilder;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionPart;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.datastructure.tree.redblacktree.Interval;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.datastructure.tree.redblacktree.IntervalTree.GroupIterator;
import gedi.util.datastructure.tree.redblacktree.SimpleInterval;
import gedi.util.genomic.Coverage;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.math.stat.kernel.GaussianKernel;
import gedi.util.math.stat.kernel.PreparedIntKernel;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;

public class PeakCalling {

	private GenomicRegionStorage<AlignedReadsData> storage;
	private double factor = 2;
	private double peakFactor = 3;
	private Progress progress = new NoProgress();
	private double bandwidth = 10;
	

	public PeakCalling(GenomicRegionStorage<AlignedReadsData> storage) {
		this.storage = storage;
	}
	
	
	public PeakCalling setProgress(Progress progress) {
		this.progress = progress;
		return this;
	}
	
	public MemoryIntervalTreeStorage<ScoreAnnotation> call(Collection<ReferenceSequence> chromosomes) throws IOException {
		MemoryIntervalTreeStorage<ScoreAnnotation> re = new MemoryIntervalTreeStorage<ScoreAnnotation>(ScoreAnnotation.class);
		for (ReferenceSequence ref : chromosomes){
			processChromosome(ref, re);
		}
		return re;
	}
	
	
	public void setFactor(double factor) {
		this.factor = factor;
	}
	
	public void setPeakFactor(double peakFactor) {
		this.peakFactor = peakFactor;
	}
	
	public void setBandwidth(double bandwidth) {
		this.bandwidth = bandwidth;
	}
	
	private void processChromosome(ReferenceSequence ref, MemoryIntervalTreeStorage<ScoreAnnotation> re) throws IOException {
		
		progress.init();
		progress.setDescription("Computing coverage for reference "+ref);
		
		int[] stat = new int[3];
		Coverage cut = new Coverage(260_000_000);
		storage.iterateMutableReferenceGenomicRegions(ref.toStrandIndependent()).forEachRemaining(rgr->{
			cut.add(rgr.getRegion().getStart()+4, rgr.getData().getTotalCountOverallFloor(ReadCountMode.Weight));	
			cut.add(rgr.getRegion().getStop()-4, rgr.getData().getTotalCountOverallFloor(ReadCountMode.Weight));
			stat[0]+=rgr.getData().getTotalCountOverallFloor(ReadCountMode.Weight);
		});
		storage.iterateMutableReferenceGenomicRegions(ref.toPlusStrand()).forEachRemaining(rgr->{
			cut.add(rgr.getRegion().getStart()+4, rgr.getData().getTotalCountOverallFloor(ReadCountMode.Weight));	
			cut.add(rgr.getRegion().getStop()-4, rgr.getData().getTotalCountOverallFloor(ReadCountMode.Weight));
			stat[1]+=rgr.getData().getTotalCountOverallFloor(ReadCountMode.Weight);
		});
		storage.iterateMutableReferenceGenomicRegions(ref.toMinusStrand()).forEachRemaining(rgr->{
			cut.add(rgr.getRegion().getStart()+4, rgr.getData().getTotalCountOverallFloor(ReadCountMode.Weight));	
			cut.add(rgr.getRegion().getStop()-4, rgr.getData().getTotalCountOverallFloor(ReadCountMode.Weight));
			stat[2]+=rgr.getData().getTotalCountOverallFloor(ReadCountMode.Weight);
		});
		
		double[] dens = cut.getCoverageAsDouble();
		
		new GaussianKernel(bandwidth ).prepare().processInPlace(dens, 0, dens.length);
		cut.clear();
		progress.setDescriptionf("Reads (Strand independent/Plus strand/Minus Strand): %d/%d/%d",stat[0],stat[1],stat[2]);
		progress.finish();
		
//		DiskGenomicNumericBuilder d  = new DiskGenomicNumericBuilder("dens.rmq");
//		for (int i=0; i<dens.length; i++)
//			d.addValue(ref, i, dens[i]);
//		d.build(false, true);
		

		double median = new Median().evaluate(dens);
		double cutoff = median*peakFactor;
		double cutoff2 = median*factor;

//		System.out.println("cutoff: "+cutoff);
//		System.out.println("cutoff2: "+cutoff2);
//		System.out.println("bandwidth: "+bandwidth);
		
		
		progress.init();
		progress.setDescription("Calling peaks for reference "+ref);
		

		
		int n = 0;
		for (int i=0; i<dens.length; i++) {
			if (dens[i]>cutoff) {
				
				// find left border of peak
				int start;
				for (start=i-1; start>=0 && dens[start]>cutoff2; start--);
				start++;
				
				// find right border of peak
				for (; i<dens.length && dens[i]>cutoff2; i++);
				
				re.add(new ImmutableReferenceGenomicRegion<ScoreAnnotation>(ref, new ArrayGenomicRegion(start,i),new ScoreAnnotation(ArrayUtils.max(dens, start, i)/median)));
				n++;
			}
		}
		
		progress.setDescription("Calling peaks for reference "+ref+": "+n+" peaks");
		progress.finish();
		
//		
//		GenomicRegion startPeaks;
//		GenomicRegion bg;
//		
//		GenomicRegion peaks = new ArrayGenomicRegion();
//		
//		int jump = 20;
//		int iter = 0;
//		do {
//			
//			progress.init();
//			progress.setCount(c.length/jump);
//			
//			startPeaks = peaks;
//			bg = peaks.invert(0,c.length);
//			
//			int start = -1;
//			double avgInBg = 0;
//			
//			for (int p=1; p<c.length; p+=jump) {
//				avgInBg = computeAverageInBackground(c,bg,p,halfWin);
//				boolean inPeak = c[p]-c[p-1]>factor *avgInBg;
//				if (inPeak) {
//					// in range
//					if (start==-1) start = p;
//				}
//				else {
//					// not in range
//					if (start>-1) {
//						for (; start-2>=0 && c[start-1]-c[start-2]>avgInBg*factor; start--);
//						for (; p+1<c.length && c[p+1]-c[p]>avgInBg*factor; p++);
//						
//						
//						avgInBg = computeAverageInBackground(c2,bg,(start+p)/2,halfWin);
//						if (ArrayUtils.max(c2,start,p)>avgInBg*factor) 
//							peaks = peaks.union(new ArrayGenomicRegion(start,p));
//						
//						start = -1;
//					}
//				}
//				
//				progress.setProgress(p/jump).setDescriptionf("Calling peaks for reference %s; Iteration %d, Peaks: %d, A=%.2f, Window %d-%d",ref,iter,peaks.getNumParts(),avgInBg,p-halfWin,p+halfWin);
//				
//			}
//			
//			if (start>-1) {
//				for (; start-1>=0 && c[start-1]>avgInBg*factor; start--);
//				peaks = peaks.union(new ArrayGenomicRegion(start,c.length));
//				start = -1;
//			}
//			progress.finish();
//			
////			try {
////				LineOrientedFile out = new LineOrientedFile("iter"+iter+".bed");
////				out.startWriting();
////				for (int p=0; p<peaks.getNumParts(); p++) {
////					out.writef("%s\t%d\t%d\n",ref.getName(),peaks.getStart(p),peaks.getEnd(p));
////				}
////				
////				out.finishWriting();
////			} catch (IOException e) {}
//			
//			iter++;
//		} while (!startPeaks.equals(peaks));
//		bg = peaks.invert(0,c.length);
//		
//		
//		progress.init();
//		progress.setDescription("Merging peaks for reference "+ref);
//		IntervalTree<GenomicRegionPart, Void> itree = new IntervalTree<GenomicRegionPart, Void>();
//		for (int p=0; p<peaks.getNumParts(); p++) {
//			itree.add(peaks.getPart(p));
//		}
//		
//		IntervalTree<GenomicRegionPart, Void> otree = new IntervalTree<GenomicRegionPart, Void>();
//		storage.iterateMutableReferenceGenomicRegions(ref).forEachRemaining(rgr->{
//			if (itree.hasIntervalsIntersecting(rgr.getRegion().getStart(),rgr.getRegion().getStop())) 
//				otree.add(rgr.getRegion().getPart(0));
//		});
//		
//		otree.putAll(itree);
//		
//		progress.finish();
//	
////		try {
////			LineOrientedFile out = new LineOrientedFile("final.bed");
////			out.startWriting();
//			IntervalTree<GenomicRegionPart, Void>.GroupIterator git = otree.groupIterator();
//			while (git.hasNext()) {
//				NavigableMap<GenomicRegionPart, Void> g = git.next();
//				int start = g.keySet().stream().map(x->x.getStart()).min(Integer::compare).get();
//				int stop = g.keySet().stream().map(x->x.getStop()).max(Integer::compare).get();
//				
////				out.writef("%s\t%d\t%d\n",ref.getName(),start,stop+1);
//				
//				double f = (c[stop]-c[Math.max(start-1, 0)]) / computeAverageInBackground(c, bg, (start+stop)/2, halfWin);
//				re.add(new ImmutableReferenceGenomicRegion<ScoreAnnotation>(ref, new ArrayGenomicRegion(start,stop+1),new ScoreAnnotation(f)));
//			}
////			out.finishWriting();
////		} catch (IOException e) {}
//			
//			
////		for (int p=0; p<peaks.getNumParts(); p++) {
////			double f = (c[peaks.getStop(p)]-c[Math.max(peaks.getStart(p)-1, 0)]) / computeAverageInBackground(c, bg, (peaks.getStart(p)+peaks.getStop(p))/2, halfWin);
////			re.add(new ImmutableReferenceGenomicRegion<Double>(ref, new ArrayGenomicRegion(peaks.getStart(p),peaks.getEnd(p)),f));
////		}
	}
	
	

	private static double computeAverageInBackground(long[] c, GenomicRegion bg,
			int p, int halfWin) {

		int startPart = bg.getEnclosingPartIndex(p-halfWin);
		if (startPart<0) startPart = -startPart-1;
		int endPart = bg.getEnclosingPartIndex(p+halfWin);
		if (endPart<0) endPart = -endPart-2;
		
		long sum = 0;
		double totalLength = 0;
		
		for (int pi=startPart; pi<=endPart; pi++) {
			int start = Math.max(bg.getStart(pi),p-halfWin);
			int end = Math.min(c.length-1,Math.min(bg.getStop(pi),p+halfWin));
			sum+=c[end]-c[start-1>0?start-1:0];
			totalLength+=end-(start-1>0?start-1:0);
		}
		
		
		return sum/totalLength;
	}
	
	
}
