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

package gedi.riboseq.cleavage;

import gedi.core.data.annotation.Transcript;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ConditionMappedAlignedReadsData;
import gedi.core.data.reads.ContrastMapping;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.riboseq.utils.RiboUtils;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.math.stat.RandomNumbers;
import gedi.util.r.R;
import gedi.util.r.RConnect;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.rosuda.REngine.REngineException;

public class CleavageModelEstimator {
	
	
	Progress progress = new NoProgress();
	
	private GenomicRegionStorage<Transcript> annotation;
	private GenomicRegionStorage<AlignedReadsData> reads;
	private Predicate<ReferenceGenomicRegion<AlignedReadsData>> filter;
	
	private int maxMulti = 1;
	private int maxLength = 50;
	
	int obsMinLength = -1;
	int obsMaxLength = -1;
	
	// length, frame, lm, condition
	double[][][][] data;

	private ContrastMapping mapping;
	
	
	private double[] generatedLeft;
	private double[] generatedRight;
	
	
	private double[] bestPl;
	private double[] bestPr;

	private int repeats = 100000;
	int maxiter = 100;

	double deltaCutoff = 1E-5;

	private double bestU;

	private int bestC;

	private int maxPos = 12;
	
	
	public CleavageModelEstimator(GenomicRegionStorage<Transcript> annotation,
			GenomicRegionStorage<AlignedReadsData> reads, Predicate<ReferenceGenomicRegion<AlignedReadsData>> filter) {
		this.annotation = annotation;
		this.reads = reads;
		this.filter = filter;
		setMerge(false);
		
	}

	public void setProgress(Progress progress) {
		this.progress = progress;
	}

	public void setRepeats(int repeats) {
		this.repeats = repeats;
	}
	
	public void setMaxiter(int maxiter) {
		this.maxiter = maxiter;
	}
	
	public void setMaxPos(int maxPos) {
		this.maxPos = maxPos;
	}
	
	public void setMerge(boolean merge){
		if (merge) {
			mapping = new ContrastMapping();
			int c = reads.getRandomRecord().getNumConditions();
			for (int i=0; i<c; i++)
				mapping.addMapping(i, 0, reads.getName());
		} else {
			mapping = new ContrastMapping();
			int c = reads.getRandomRecord().getNumConditions();
			for (int i=0; i<c; i++)
				if (!reads.getMetaData().isNull())
					mapping.addMapping(i, i, reads.getMetaData().getEntry("conditions").asArray()[i].getEntry("name").asString());
				else
					mapping.addMapping(i, i, i+"");
		}
	}
	public void setMappingByRegex(String...patterns) {
		Pattern[] p = new Pattern[patterns.length];
		for (int i=0; i<p.length; i++)
			p[i] = Pattern.compile(patterns[i]);
		
		DynamicObject[] cond = reads.getMetaData().getEntry("conditions").asArray();
		
		mapping = new ContrastMapping();
		for (int i=0; i<cond.length; i++) {
			String name = cond[i].getEntry("name").asString();
			int t = -1;
			for (int pi=0; pi<p.length; pi++) {
				if (p[pi].matcher(name).find()) {
					if (t==-1) t = pi;
					else throw new RuntimeException(name+" is matched by multiple patterns!");
				}
			}
			if (t>=0)
				mapping.addMapping(i, t);
		}
	}
	
	public void setMapping(ContrastMapping mapping) {
		this.mapping = mapping;
	}


//	public void collectMismatchData(LineOrientedFile summaryFile) throws IOException {
//		
//		HashSet<ReferenceSequence> refs = new HashSet<ReferenceSequence>();
//		refs.addAll(annotation.getReferenceSequences());
//		refs.retainAll(reads.getReferenceSequences());
//		
//		int count = 0;
//		for (ReferenceSequence r : refs)
//			count+=reads.size(r);
//		
//		
//		
//		progress.init();
//		progress.setDescription("Collecting read information");
//		progress.setCount(count);
//		
//		mismatchData = new HashMap<Integer, double[]>();
//
//		ArrayList<ImmutableReferenceGenomicRegion<Transcript>> buff = new ArrayList<ImmutableReferenceGenomicRegion<Transcript>>();
//		
//		// how many reads without (=0) or with (=1) leading mismatches
//		HashMap<Integer,double[][]> counter = new HashMap<Integer, double[][]>();
//		
//		for (ReferenceSequence r : refs)
//		reads.iterateMutableReferenceGenomicRegions(r).forEachRemaining(rgr->{
//			
//			progress.incrementProgress();
//			progress.setDescription(()->rgr.toLocationString());
//			
//			GenomicRegion read = rgr.getRegion();
//			boolean valid = false;
//			
//			buff.clear();
//			annotation.getReferenceRegionsIntersecting(rgr.getReference(), rgr.getRegion(),buff);
//			
//			for (ImmutableReferenceGenomicRegion<Transcript> transRgr : buff) {
//				GenomicRegion trans = transRgr.getRegion();
//				
//				if (transRgr.getData().isCoding() && trans.containsUnspliced(read)) {
//					valid = true;
//					break;
//				}
//			}
//			
//			if (valid) {
//				ConditionMappedAlignedReadsData mard = new ConditionMappedAlignedReadsData(rgr.getData(), mapping);
//	
//				for (int d=0; d<mard.getDistinctSequences(); d++) {
//						
//					if (mard.getMultiplicity(d)<=maxMulti) {
//						boolean leading = RiboUtils.hasLeadingMismatch(rgr.getData(), d);
//						mard.addCount(d, counter.computeIfAbsent(read.getTotalLength(), k->new double[2][mapping.getNumMergedConditions()])[leading?1:0],true);
//					}
//				}
//			}
//		});
//		
//		progress.finish();
//		
//	
//		summaryFile.startWriting();
//		summaryFile.writeLine("Length\tCondition\tLM count\tno LM count");
//
//		ArrayList<Integer> lengths = new ArrayList<Integer>(counter.keySet());
//		Collections.sort(lengths);
//		
//		for (int c=0; c<mapping.getNumMergedConditions(); c++) {
//			for (Integer l : lengths)
//				summaryFile.writef("%d\t%s\t%.2f\t%.2f\n",l,mapping.getMappedName(c),counter.get(l)[1][c],counter.get(l)[0][c]);
//		}
//		summaryFile.finishWriting();
//		
//	}

	

	private enum ReadPositionType {
		NO_CODING,MULTI,START,STOP,STARTSTOP,INTERNAL
	}
	
	public void collectEstimateData(LineOrientedFile summaryFile) throws IOException {
		
		HashSet<ReferenceSequence> refs = new HashSet<ReferenceSequence>();
		refs.addAll(annotation.getReferenceSequences());
		refs.retainAll(reads.getReferenceSequences());
//		Iterator<ReferenceSequence> it = refs.iterator();
//		while (it.hasNext()) {
//			if (it.next().getStrand()==Strand.Plus)
//				it.remove();
//		}
		
		int count = 0;
//		for (ReferenceSequence r : refs)
//			count+=reads.size(r);
		
		obsMaxLength = -1;
		obsMinLength = -1;
		
		progress.init();
		progress.setDescription("Collecting read information");
//		progress.setCount(count);
		
		data = new double[maxLength+1][3][2][mapping.getNumMergedConditions()];
		
		double[][] summary = new double[ReadPositionType.values().length][mapping.getNumMergedConditions()];
		ArrayList<ImmutableReferenceGenomicRegion<Transcript>> buff = new ArrayList<ImmutableReferenceGenomicRegion<Transcript>>();
		
		for (ReferenceSequence r : refs) {
			ExtendedIterator<ImmutableReferenceGenomicRegion<AlignedReadsData>> it = reads.ei(r);
			if (filter!=null) 
				it = it.filter(filter);
			while (it.hasNext()) {
				ImmutableReferenceGenomicRegion<AlignedReadsData> rgr = it.next();
				
				progress.incrementProgress();
				progress.setDescription(()->rgr.toLocationString());
				
				GenomicRegion read = rgr.getRegion();
				int first = -1;
				int last = -1;
				
				buff.clear();
				annotation.getReferenceRegionsIntersecting(rgr.getReference(), rgr.getRegion(),buff);
				
				for (ImmutableReferenceGenomicRegion<Transcript> transRgr : buff) {
					GenomicRegion trans = transRgr.getRegion();
					
					if (transRgr.getData().isCoding() && trans.containsUnspliced(read)) {
						
						int startIn = trans.induce(transRgr.getData().getCodingStart());
						int endIn = trans.induce(transRgr.getData().getCodingEnd()-1)+1;
						int cdslen = endIn-startIn;
						
						if (cdslen%3!=0)
							continue;
						
						GenomicRegion mread = trans.induce(read);
						
						if (mread.getEnd()>=startIn+3 && mread.getStart()<=endIn-3) {
						
							mread = mread.translate(-startIn);
						
							int firstCand,lastCand;
							
							firstCand = mread.getStart()<=0 ? -mread.getStart() : (3-mread.getStart()%3)%3 ;
							lastCand = mread.getEnd()>=cdslen?cdslen-mread.getStart()-3:firstCand+((mread.getTotalLength()-firstCand)/3-1)*3;
							if (lastCand<firstCand || (lastCand-firstCand)%3!=0
									|| (firstCand>2 && lastCand<mread.getTotalLength()-5)
									|| firstCand<0 || firstCand>mread.getTotalLength()-3 
									|| lastCand<0 || lastCand>mread.getTotalLength()-3)
								throw new RuntimeException(mread.getTotalLength()+": "+firstCand+"-"+lastCand);
							
							if (transRgr.getReference().getStrand()==Strand.Minus) {
								int tmp = firstCand;
								firstCand = mread.getTotalLength()-1-(lastCand+2);
								lastCand = mread.getTotalLength()-1-(tmp+2);
							}
								
							
							if (first==-1) {
								first = firstCand;
								last = lastCand;
							} else if (first!=firstCand || last!=lastCand) {
								first = last = -2;
							}
						}
					}
				}
				
				ConditionMappedAlignedReadsData mard = new ConditionMappedAlignedReadsData(rgr.getData(), mapping);
				
				ReadPositionType type = ReadPositionType.INTERNAL;
				if (first == -1) type = ReadPositionType.NO_CODING;
				else if (first == -2) type = ReadPositionType.MULTI;
				else if (first>2 && last<read.getTotalLength()-5) type = ReadPositionType.STARTSTOP;
				else if (first>2) type = ReadPositionType.START;
				else if (last<read.getTotalLength()-5) type = ReadPositionType.STOP;
				
				int l = read.getTotalLength();
				if (l>maxLength) continue;
				
				if (obsMaxLength==-1) {
					obsMaxLength = l;
					obsMinLength = l;
				} else {
					obsMaxLength = Math.max(l,obsMaxLength);
					obsMinLength = Math.min(l,obsMinLength);
				}
				
	//			double[] mmc = new double[5];
				for (int d=0; d<mard.getDistinctSequences(); d++) {
					
					if (mard.getMultiplicity(d)<=maxMulti) {
	//					mard.addCount(d,summary[type.ordinal()],true);
						mard.addCountsForDistinct(d, summary[type.ordinal()], ReadCountMode.Weight);
						
						if (type==ReadPositionType.INTERNAL){		
	//						mard.addCount(d, data[l][first%3][RiboUtils.hasLeadingMismatch(mard, d)?1:0],true);
						
							if (RiboUtils.hasLeadingMismatch(mard, d)) {
								if (RiboUtils.isLeadingMismatchInsideGenomicRegion(mard, d)) 
									mard.addCountsForDistinct(d, data[l][first%3][1], ReadCountMode.Weight);
								else // if it were inside, the read would be one bp longer and the codon position would one farther away
									mard.addCountsForDistinct(d, data[l+1][(first+1)%3][1], ReadCountMode.Weight);
							} else
								mard.addCountsForDistinct(d, data[l][first%3][0], ReadCountMode.Weight);
	//						if (RiboUtils.hasLeadingMismatch(mard, d))
	//							mmc[SequenceUtils.inv_nucleotides[RiboUtils.getLeadingMismatch(mard, d)]]+=mard.getTotalCountForDistinct(d, ReadCountMode.Weight);// mard.getSumCount(d);
	//						else 
	//							mmc[4]+=mard.getTotalCountForDistinct(d, ReadCountMode.Weight);//mard.getSumCount(d);
						}
						
					}
				}
				
	//			if (ArrayUtils.sum(mmc)>100)
	//				System.out.printf("%.0f\t%.0f\t%.0f\t%.0f\t%.0f\n",mmc[0],mmc[1],mmc[2],mmc[3],mmc[4]);
				
			}
		}
		
		progress.finish();
		
		
		summaryFile.startWriting();
		summaryFile.write("Type");
		for (int i=0; i<mapping.getNumMergedConditions(); i++)
			summaryFile.writef("\t%s",mapping.getMappedName(i));
		summaryFile.writeLine();
		
		for (int i=0; i<summary.length; i++)  
			summaryFile.writeLine(ReadPositionType.values()[i]+"\t"+StringUtils.concat("\t", summary[i]));
		summaryFile.finishWriting();
		
	}
	
	public double[] getTotal() {
		double[] re = new double[mapping.getNumMergedConditions()];
		for (int c=0; c<mapping.getNumMergedConditions(); c++) {
			for (int l = obsMinLength; l<=obsMaxLength; l++) {
				for (int lm=0; lm<2; lm++)
					for (int f=0; f<3; f++)
						re[c]+=data[l][f][lm][c];
			}
		}
		return re;
	}
	
	
	public void writeEstimateData(LineOrientedFile out) throws IOException {
		out.startWriting();
		
		
		out.writeLine("Condition\tLength\tF0L0\tF1L0\tF2L0\tF0L1\tF1L1\tF2L1");
		
		for (int c=0; c<mapping.getNumMergedConditions(); c++) {
			for (int l = obsMinLength; l<=obsMaxLength; l++) {
				out.writef("%s\t%d", mapping.getMappedName(c), l);
				for (int lm=0; lm<2; lm++)
					for (int f=0; f<3; f++)
						out.writef("\t%.2f",data[l][f][lm][c]);
				out.writeLine();
			}
		}
		
		
		out.finishWriting();
	}
	public void readEstimateData(LineOrientedFile in) throws IOException {
		
		String[] lines = in.lineIterator().toArray(new String[0]);
		
		String[] cond = EI.wrap(lines,1,lines.length).map(s->StringUtils.splitField(s, '\t', 0)).unique(true).toArray(new String[0]);
		obsMinLength = EI.wrap(lines,1,lines.length).map(s->Integer.parseInt(StringUtils.splitField(s, '\t', 1))).reduce((BinaryOperator<Integer>)Math::min);
		obsMaxLength = EI.wrap(lines,1,lines.length).map(s->Integer.parseInt(StringUtils.splitField(s, '\t', 1))).reduce((BinaryOperator<Integer>)Math::max);
		
		mapping = new ContrastMapping();
		for (int i=0; i<cond.length; i++)
			mapping.addMapping(i, i, cond[i]);
		
		
		data = new double[maxLength+1][3][2][mapping.getNumMergedConditions()];
		
		
		
		for (int i=1; i<lines.length; i++) {
			
			String[] fi = StringUtils.split(lines[i], '\t');
			
			int c = mapping.getMappedIndex(fi[0]);
			int l = Integer.parseInt(fi[1]);
			
			int index = 2;
			for (int lm=0; lm<2; lm++)
				for (int f=0; f<3; f++)
					data[l][f][lm][c] = Double.parseDouble(fi[index++]);
			
		}
	}
	
	
	public double generateEstimateData() throws IOException, REngineException {
		String[] cond = {"0"};
		obsMinLength = 10+10+3;
		obsMaxLength = 14+14+3;
		
		mapping = new ContrastMapping();
		for (int i=0; i<cond.length; i++)
			mapping.addMapping(i, i, cond[i]);
		
		
		data = new double[maxLength+1][3][2][mapping.getNumMergedConditions()];
		
		
		double[] lv = {0,1,20,1,0.5,0.3}; ArrayUtils.normalize(lv); ArrayUtils.cumSumInPlace(lv, 1);
		double[] rv = {0.5,11,9,3,1}; ArrayUtils.normalize(rv); ArrayUtils.cumSumInPlace(rv, 1);
//		double[] lv = {0,0.1,1,0.3,0}; ArrayUtils.normalize(lv); ArrayUtils.cumSumInPlace(lv, 1);
//		double[] rv = {0,0,1,1,0}; ArrayUtils.normalize(rv); ArrayUtils.cumSumInPlace(rv, 1);
		double uv = 0.2;
		
		int N = 18_000_000;
		
		int[] ql0 = new int[lv.length];
		int[] qr0 = new int[rv.length];
		int[] ql1 = new int[lv.length];
		int[] qr1 = new int[rv.length];
		
		RandomNumbers rnd = new RandomNumbers();
		for (int i=0; i<N; i++) {
			int l = 10+rnd.getCategorial(lv);
			int r = 10+rnd.getCategorial(rv);
			int u = rnd.getBool(uv)?1:0;
			int lm = u==0?0:(rnd.getBool(0.75)?1:0);
			if (l-10+u<5) {
				if (lm==1) {
					ql1[l-10+u]++; qr1[r-10]++;
				}
				else {
					ql0[l-10+u]++; qr0[r-10]++;
				}
			}
			data[l+r+3+u][(l+u)%3][lm][0]++;
		}

		ArrayUtils.decumSumInPlace(lv, 1);
		ArrayUtils.decumSumInPlace(rv, 1);

//		System.out.println(StringUtils.concat("\t", lv)+"\t"+StringUtils.concat("\t", rv));

		generatedLeft = lv;
		generatedRight = rv;
		
//		System.out.println("Generated:");
//		System.out.println("ql0="+Arrays.toString(ql0));
//		System.out.println("qr0="+Arrays.toString(qr0));
//		System.out.println("ql1="+Arrays.toString(ql1));
//		System.out.println("qr1="+Arrays.toString(qr1));
		
		
		double[] Pl =ArrayUtils.concat(ArrayUtils.repeat(0, 10),generatedLeft,ArrayUtils.repeat(0, obsMaxLength+1-generatedLeft.length-10));
		double[] Pr = ArrayUtils.concat(ArrayUtils.repeat(0, 10),generatedRight,ArrayUtils.repeat(0, obsMaxLength+1-generatedRight.length-10));

		double ll = 0;
		for (int l=obsMinLength; l<=obsMaxLength; l++)
			for (int f=0; f<3; f++) {
				int f1 = (f+3-1)%3; // shift frame due to untemplated addition
				
				int lm = 1;
				double n = data[l][f][lm][0];
				
				if (n>0) {
					double p = 0;
					for (int i=f1; i<Pl.length && l-i-3-1>=0; i+=3)
						p+=Pl[i]*Pr[l-i-3-1];
				
					ll += n*Math.log(p*uv);
				}
				
				lm = 0;
				n = data[l][f][lm][0];
				
				if (n>0) {
					double p = 0;
					for (int i=f1; i<Pl.length && l-i-3-1>=0; i+=3)
						p+=Pl[i]*Pr[l-i-3-1]*uv;
					for (int i=f; i<Pl.length && l-i-3>=0; i+=3)
						p+=Pl[i]*Pr[l-i-3]*(1-uv);
					
					ll += n*Math.log(p*uv);
				}
			}
		return ll;
	}
	
	
	
	// f=0 means that codon starts at 0,3,6,9,...
	// f=1 means that codon starts at 1,4,7,10,...
	// f=2 means that codon starts at 2,5,8,11,...
	
	
	public BufferedImage plotProbabilities(String title, String plotFile) throws REngineException, IOException {
		R r = RConnect.getInstance().get();
		r.assign("col", bestPl);
		r.assign("row", bestPr);
		if (generatedLeft!=null) {
			r.assign("gl", ArrayUtils.concat(ArrayUtils.repeat(0, 10),generatedLeft,ArrayUtils.repeat(0, bestPl.length-generatedLeft.length-10)));
			r.assign("gr", ArrayUtils.concat(ArrayUtils.repeat(0, 10),generatedRight,ArrayUtils.repeat(0, bestPr.length-generatedRight.length-10)));
			r.startPlots(800, 800).eval("barplot(rbind(gl, col, gr, row),beside=T)");
		}
		else {
			String t = title!=null?title:FileUtils.getNameWithoutExtension(plotFile);
			r.startPlots(800, 800).eval("barplot(c(rev(col[-1]),NA,NA,NA,row),beside=T,main='"+t+"', names.args=c())");
		}
		
		BufferedImage img = r.finishPlot();
		
		if (plotFile!=null)
			ImageIO.write(img, "png", new File(plotFile));
		return img;
	}
	
	public double estimateBoth(int c, int nthreads) {
		CleavageModelEstimatorThread[] threads = new CleavageModelEstimatorThread[Math.max(1, nthreads)];
		int best;
		
		progress.init();
		if (nthreads==0) {
			threads[0] = new CleavageModelEstimatorThread(this, c, maxPos, repeats);
			threads[0].run();
			best = 0;
		} else {
			for (int i=0; i<nthreads; i++) {
				threads[i] = new CleavageModelEstimatorThread(this, c, maxPos, repeats/nthreads);
				threads[i].start();
			}
			
			for (int i=0; i<nthreads; i++)
				try {
					threads[i].join();
				} catch (InterruptedException e) {
					throw new RuntimeException("Interrupted",e);
				}
	
			best = 0;
			for (int i=1; i<nthreads; i++)
				if (threads[i].getBestLL()>threads[best].getBestLL())
					best = i;
		}
		
		
		bestC = c;
		bestPl = threads[best].getBestPl();
		bestPr = threads[best].getBestPr();
		bestU = threads[best].getBestU();
		
		progress.finish();
		
		correctMaxPos();
		
		return threads[best].getBestLL();
	}
	
	private void correctMaxPos() {
		int shift;
		int cmax = ArrayUtils.argmax(bestPl);
		if (maxPos<0) {
			int dmax = ArrayUtils.argmax(bestPr);
			
			shift = (dmax-cmax)/2;
		} else {
			shift = maxPos-cmax;
		}
		shift = (int)(Math.round(shift/3.0)*3);
		if (shift!=0)
			System.err.println("Correcting from "+cmax+" to "+(cmax+shift)+" (desired: "+maxPos+")");
		double[] pl = new double[bestPl.length];
		for (int i=0; i<pl.length; i++) 
			pl[i] = (i-shift>=0 && i-shift<pl.length)?bestPl[i-shift]:0;
		double[] pr = new double[bestPr.length];
		for (int i=0; i<pr.length; i++) 
			pr[i] = (i+shift>=0 && i+shift<pl.length)?bestPr[i+shift]:0;
		bestPl = pl;
		bestPr = pr;
		ArrayUtils.normalize(bestPl);
		ArrayUtils.normalize(bestPr);
	}

	void initBySimpleModel(int c, double[] Pl, double[] Pr) {
		int[] mostLikely = {12,13,11};
		Arrays.fill(Pl, 1);
		Arrays.fill(Pr, 1);
		
		for (int l=0; l<data.length; l++) {
			for (int f=0; f<data[l].length; f++) {
				for (int lm=0; lm<data[l][f].length; lm++) {
					int p = mostLikely[lm==1?(f+2)%3:f];
					if (p>=0 && l-p-3-(lm==1?1:0)>=0) {
						Pl[p]+=data[l][f][lm][c];
						Pr[l-p-3-(lm==1?1:0)]+=data[l][f][lm][c];
					}
				}
			}	
		}
	}

	public boolean isMonotonous() {
		return isMonotonous(bestPl) && isMonotonous(bestPr);
	}
	
	private boolean isMonotonous(double[] m) {
		int am = ArrayUtils.argmax(m);
		for (int i=1; i<m.length-1; i++) {
			if (i!=am) {
				if (Math.min(m[i-1],Math.min(m[i],m[i+1]))==m[i] || Math.max(m[i-1],Math.max(m[i],m[i+1]))==m[i])
					return false;
			}
		}
		return true;
	}
	
	public double smoothModel(double frac) {
		return smoothModel(frac, false);
	}
	public double smoothModel(double frac, boolean onlyNonMono) {
		
		double unconstrainedAkaike = 2*(bestPl.length+bestPr.length-2+1)-2*computeLL(bestC,bestPl,bestPr,bestU);
		
		
		for (double[] m : Arrays.asList(bestPl,bestPr)) {
			
			int am = ArrayUtils.argmax(m);
			double[] diff = new double[m.length];
			IntArrayList nonmono = new IntArrayList();
			for (int i=1; i<m.length-1; i++) {
				if (i!=am) {
					if (!onlyNonMono || (Math.min(m[i-1],Math.min(m[i],m[i+1]))==m[i] || Math.max(m[i-1],Math.max(m[i],m[i+1]))==m[i]))
						nonmono.add(i);
				}
			}
			int[] adapt = nonmono.toIntArray();
			
			
			for (int iter=1; iter<=1/frac*10; iter++) {
				for (int i : adapt) {
					double e = Math.exp((Math.log(m[i-1])+Math.log(m[i+1]))/2);
					diff[i] = e-m[i];
				}
				for (int i : adapt) {
					m[i]+=diff[i]*frac;
				}
				
				ArrayUtils.normalize(m);
				
			}
			
		}

		double akaike = 2*(7)-2*computeLL(bestC,bestPl,bestPr,bestU);
		System.out.println(unconstrainedAkaike+" "+akaike);
		
		return computeLL(bestC,bestPl,bestPr,bestU);
	}
	
	
	public double computeLL(int c, double[] Pl, double[] Pr, double u) {
		double ll = 0;
		
		for (int l=obsMinLength; l<=obsMaxLength; l++)
			for (int f=0; f<3; f++) {
				int f1 = (f+3-1)%3; // shift frame due to untemplated addition
				
				int lm = 1;
				double n = data[l][f][lm][c];
				
				if (n>0) {
					double p = 0;
					for (int i=f1; i<Pl.length && l-i-3-1>=0; i+=3)
						p+=Pl[i]*Pr[l-i-3-1];
				
					ll += n*Math.log(p*u);
				}
				
				lm = 0;
				n = data[l][f][lm][c];
				
				if (n>0) {
					double p = 0;
					for (int i=f1; i<Pl.length && l-i-3-1>=0; i+=3)
						p+=Pl[i]*Pr[l-i-3-1]*u;
					for (int i=f; i<Pl.length && l-i-3>=0; i+=3)
						p+=Pl[i]*Pr[l-i-3]*(1-u);
					
					ll += n*Math.log(p*u);
				}
			}
		return ll;
	}
	
	public RiboModel getModel() {
		return new RiboModel(bestPl,bestPr,bestU);
	}
	
	
}

