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

import gedi.core.data.reads.AlignedReadsDataFactory;
import gedi.core.data.reads.DefaultAlignedReadsData;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.util.ArrayUtils;
import gedi.util.SequenceUtils;
import gedi.util.algorithm.rmq.SuccinctRmq;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.functions.IntCharFunction;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.randomaccess.serialization.BinarySerializable;
import gedi.util.math.function.PiecewiseLinearFunction;
import gedi.util.math.stat.DoubleRanking;
import gedi.util.math.stat.RandomNumbers;
import gedi.util.math.stat.kernel.GaussianKernel;
import gedi.util.math.stat.kernel.PreparedIntKernel;
import gedi.util.math.stat.testing.PValueCombiner;
import gedi.util.orm.OrmSerializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

import jdistlib.Beta;
import jdistlib.Binomial;
import jdistlib.Normal;
import jdistlib.math.PolyGamma;
import libsvm.Svm;
import libsvm.Svm_model;
import libsvm.Svm_node;


public class RiboModel implements BinarySerializable {

	public static final String OMIT_READ_ERROR_OPTION = "OMIT_READ_ERROR_OPTION";
	
	private HashMap<Integer, double[]> probs = new HashMap<Integer, double[]>();
	private double u;
	private double[] Pl;
	private double[] Pr;
	
	private SimpleCodonModel simple;


	private double[] internalQuantiles;
	private PiecewiseLinearFunction[] internalFrame1Quantiles;
	private PiecewiseLinearFunction[] internalFrame2Quantiles;

	private double trim;
	private double threshold;

//	private PiecewiseLinearFunction gapMean;
//	private PiecewiseLinearFunction gapVar;
	private PiecewiseLinearFunction gap99;
	private PiecewiseLinearFunction gof99;
	private PiecewiseLinearFunction gofCod;
	
	private Svm_model startSvm;
	private double[] startSvmSum;
	private int[] startSvmFlank;
	private int[] startSvmConditions;
	private double startSvmProbCutoff;

	private CodonFeature[] features;
	private double[] tmSums;
	
	private double changepointMean;
	private double changepointSd;
	private int changepointBandwidth;

	
	private int minReadLength = -1;
	private int maxReadLength = -1;

	
	private double codonProbInBackGround;

	private double inferenceLamdba;

	
	public static class LfcParameters {
		Svm_model lfcModel;
		int[][] lfcPairs;
		double lfcCutoff;
		double lfcPseudo;
		int lfcStart;
		int lfcEnd;
		public LfcParameters(){}
		public LfcParameters(Svm_model lfcModel, int[][] lfcPairs,
				double lfcCutoff, double lfcPseudo, int lfcStart, int lfcEnd) {
			this.lfcModel = lfcModel;
			this.lfcPairs = lfcPairs;
			this.lfcCutoff = lfcCutoff;
			this.lfcPseudo = lfcPseudo;
			this.lfcStart = lfcStart;
			this.lfcEnd = lfcEnd;
		}
		
		public double[][] prepareLfc(double[][] data) {
			double[][] re = new double[lfcPairs.length][data[0].length];
			for (int i=0; i<re.length; i++) {
				int h = lfcPairs[i][0];
				int c = lfcPairs[i][1];

				double a = 0;
				double b = 0;
				for (int p=data[0].length-1; p>=0; p--) {
					double posLfc = PolyGamma.digamma(data[h][p]+1+lfcPseudo+a)-PolyGamma.digamma(data[c][p]+1+lfcPseudo+b);
					double downstreamLfc = PolyGamma.digamma(1+lfcPseudo+a)-PolyGamma.digamma(1+lfcPseudo+b);
					re[i][p] = (posLfc-downstreamLfc)/Math.log(2);
					
					if (p>=data[0].length-lfcStart) {
						// add the first for the reference irrespective of the desired start position 
						double d = Math.max(data[h][p],data[c][p]);
						if (d>0) {
							a+=data[h][p]/d;
							b+=data[c][p]/d;
						}
					} else if (p<data[0].length-2*lfcStart) {
						double d = Math.max(data[h][p+lfcStart],data[c][p+lfcStart]);
						if (d>0) {
							a+=data[h][p+lfcStart]/d;
							b+=data[c][p+lfcStart]/d;
						}
					}
					if (p<data[0].length-lfcEnd) {
						double d = Math.max(data[h][p+lfcEnd],data[c][p+lfcEnd]);
						if (d>0) {
							a-=data[h][p+lfcEnd]/d;
							b-=data[c][p+lfcEnd]/d;
						}
					}
				}
//				re[i] = Utilities.rank(re[i]);
				
				// normalize to max outside of enrichment region
				double[] neg = re[i].clone();
				ArrayUtils.mult(neg, -1);
				SuccinctRmq rmq = new SuccinctRmq(NumericArray.wrap(neg)); // range minimum queries!
//				double[] lmax = re[i].clone();
//				for (int e=1; e<lmax.length; e++) lmax[e] = Math.max(lmax[e],lmax[e-1]); 
//				double[] rmax = re[i].clone();
//				for (int e=rmax.length-2; e>=0; e--) rmax[e] = Math.max(rmax[e],rmax[e+1]);
				
				for (int e=0; e<re[i].length; e++) {
					double l = -neg[rmq.query(0, e)];
					double r = e+lfcStart<re[i].length?-neg[rmq.query(e+lfcStart, Math.min(e+lfcStart,re[i].length-1))]:l;
					re[i][e] /= Math.max(l,r);
					if (Double.isNaN(re[i][e]))
						re[i][e] = 0;
					re[i][e] = Math.max(0,re[i][e]);
				}
				
//				ArrayUtils.mult(re[i], 1.0/ArrayUtils.max(re[i]));
			}
			return re;
		}
	}
	
	private LfcParameters lfcParam;

	
	public RiboModel() {
	}

	public RiboModel(double[] Pl, double[] Pr, double u) {
		if (Pl.length!=Pr.length) throw new IllegalArgumentException();

		this.Pl = Pl;
		this.Pr = Pr;
		this.u = u;

		for (int l=2; l<=Pl.length; l++) {

			// with leading mismatch
			double[] model = new double[l-2];
			for (int i=1; i<model.length; i++) 
				model[i] = getPosterior(true, l, i);
			double sum = 0;
			for (double x : model) sum+=x;
			if (sum!=0) {
				for (int i=0; i<model.length; i++) model[i]/=sum;
				setModel(true,l, model);
			}

			// without leading mismatch
			model = new double[l-2];
			for (int i=0; i<model.length; i++) 
				model[i] = getPosterior(false, l, i);
			sum = 0;
			for (double x : model) sum+=x;
			if (sum!=0) {
				for (int i=0; i<model.length; i++) model[i]/=sum;
				setModel(false,l, model);
			}

		}
	}
	
	public int getMinReadLength() {
		if (minReadLength==-1) computeReadLength();
		return minReadLength;
	}
	
	public int getMaxReadLength() {
		if (minReadLength==-1) computeReadLength();
		return maxReadLength;
	}
	
	public double[] getPl() {
		return Pl;
	}
	public double[] getPr() {
		return Pr;
	}
	
	public void setSimple(SimpleCodonModel simple) {
		this.simple = simple;
	}
	
	public double getInferenceLamdba() {
		return inferenceLamdba;
	}
	public void setInferenceLamdba(double inferenceLamdba) {
		this.inferenceLamdba = inferenceLamdba;
	}
	
	public SimpleCodonModel getSimple() {
		return simple;
	}
	
	public boolean isValidReadLength(int l) {
		if (minReadLength==-1) computeReadLength();
		return l>=minReadLength && l<=maxReadLength;
	}
	public double[] computeReadLength() {
		double[] ltoTotalPosterior = EI.seq(0,getObservedMaxLength()+1).mapToDouble(l->EI.seq(0,l).mapToDouble(p->getPosterior(true,l,p)+getPosterior(false,l,p)).sum()).toDoubleArray();

		DoubleRanking rank = new DoubleRanking(ltoTotalPosterior);
		rank.sort(false);
		minReadLength = maxReadLength = rank.getOriginalIndex(0);
		int ind=0;
		for (double sum = 0; sum<0.99 && ind<ltoTotalPosterior.length; sum+=rank.getValue(ind++)) {
			minReadLength = Math.min(minReadLength,rank.getOriginalIndex(ind));
			maxReadLength = Math.max(maxReadLength,rank.getOriginalIndex(ind));
		}

		rank.restore();
		return ltoTotalPosterior;
	}

	public boolean isErrorSet() {
		boolean re = gap99!=null;
		if (re!=(internalQuantiles!=null)) throw new RuntimeException("Either set all or nothing!");
		return re;
	}


	public void setRolling(double trim, double threshold) {
		this.trim = trim;
		this.threshold = threshold;
	}

//	public void setGap(PiecewiseLinearFunction mean, PiecewiseLinearFunction var) {
//		this.gapMean = mean;
//		this.gapVar = var;
//	}
	
	public void setGap(PiecewiseLinearFunction gap99) {
		this.gap99 = gap99;
	}
	
	public void setGof(PiecewiseLinearFunction gof99, PiecewiseLinearFunction gofCod) {
		this.gof99 = gof99;
		this.gofCod = gofCod;
	}
	
	public void setStartSvm(Svm_model startSvm, double[] startSvmSum, int[] startSvmConditions, int upstream, int downstream, double probCutoff) {
		this.startSvm = startSvm;
		this.startSvmSum = startSvmSum;
		this.startSvmFlank = new int[] {upstream,downstream};
		this.startSvmProbCutoff = probCutoff;
		this.startSvmConditions = startSvmConditions;
	}
	
	public void setLfcSvm(LfcParameters lfcParam) {
		this.lfcParam = lfcParam;
	}	


	
	public double getCodonGofThreshold(double activity) {
		return Math.pow(10,gofCod.applyAsDouble(Math.log10(activity)))*activity;
	}

	public double getOrfGofThreshold(double activity) {
		return Math.pow(10,gof99.applyAsDouble(Math.log10(activity)))*activity;
	}



	public void setStartCodon(CodonFeature[] features, double[] tmSums) {
		this.features = features;
		this.tmSums = tmSums;
	}

	public void setChangePoint(double changepointMean, double changepointSd,
			int changepointBandwidth) {
		this.changepointMean = changepointMean;
		this.changepointSd = changepointSd;
		this.changepointBandwidth = changepointBandwidth;
	}
	

	public void setInternal(double[] counts, PiecewiseLinearFunction[] frame1, PiecewiseLinearFunction[] frame2) {
		this.internalQuantiles = counts;
		this.internalFrame1Quantiles = frame1;
		this.internalFrame2Quantiles = frame2;
	}
	
	public void setCodonProbInBackground(double codonProbInBackGround) {
		this.codonProbInBackGround = codonProbInBackGround;
	}

	public int getObservedMaxLength() {
		return Pl.length-1;
	}

	public void setModel(boolean leading, int length, double[] model) {
		probs.put(leading?-length:length, model);
	}

	public double[] getModel(boolean leading, int length) {
		return probs.get(leading?-length:length);
	}


	public double getPosterior(boolean leading, int l, int p) {
		if (leading) 
			return p>0&&l-3-p>=0?Pl[p-1]*Pr[l-3-p]/2:0;

			double prop = (u/(4-3*u));
			double re = 0;
			if (p>0 && l-3-p>=0) re=Pl[p-1]*Pr[l-3-p]*prop;
			if (p>=0 && l-3-p>=0) re+=Pl[p]*Pr[l-3-p]*(1-prop);
			return re/2;
	}

	public double[] getFrameCountProbabilities(boolean leading, int l) {
		double[] fp = new double[3];
		for (int i=0; i<l; i++) {
			fp[i%3]+=getPosterior(leading, l, i);
		}
		ArrayUtils.normalize(fp);
		return fp;
	}

	public double Pl(int p) {
		return Pl[p];
	}

	public double Pr(int p) {
		return Pr[p];
	}

	public double getU() {
		return u;
	}

	private transient RandomNumbers rnd;
	private transient double[] cumuPl;
	private transient double[] cumuPr;
	public boolean generateRead(IntervalTree<GenomicRegion,DefaultAlignedReadsData> reads, int codonStart, IntCharFunction sequence) {
		if (rnd==null) rnd = new RandomNumbers();
		if (cumuPl==null) cumuPl = ArrayUtils.cumSumAndNormalize(Pl, 1);
		if (cumuPr==null) cumuPr = ArrayUtils.cumSumAndNormalize(Pr, 1);
		int start = codonStart-rnd.getCategorial(cumuPl);
		int end = codonStart+3+rnd.getCategorial(cumuPr);
		boolean add = rnd.getBool(u);
		if (add) start--;
		if (end-start>getObservedMaxLength()) return false;

		ArrayGenomicRegion reg = new ArrayGenomicRegion(start, end);

		char[] mmpair = null;
		if (add) {
			char mm = SequenceUtils.nucleotides[rnd.getUnif(0, 4)];
			char g = sequence.applyAsInt(start);
			if (mm!=g)
				mmpair = new char[] {g,mm}; 
		}

		AlignedReadsDataFactory fac = new AlignedReadsDataFactory(1).start();

		boolean takenCare = false;
		DefaultAlignedReadsData pres = reads.get(reg);
		if (pres!=null) {
			for (int d=0; d<pres.getDistinctSequences(); d++) {
				fac.add(pres, d);
				if (mmpair!=null && pres.getVariationCount(d)==1 && pres.isMismatch(d, 0) && pres.getMismatchGenomic(d, 0).charAt(0)==mmpair[0] && pres.getMismatchRead(d, 0).charAt(0)==mmpair[1]) {
					fac.incrementCount(0, 1);
					takenCare=true;
				}
				else if (mmpair==null && pres.getVariationCount(d)==0) {
					fac.incrementCount(0, 1);
					takenCare=true;
				}

			}
		}

		if (!takenCare) {
			fac.newDistinctSequence();
			fac.setCount(0, 1);
			fac.setWeight(1);
			fac.setMultiplicity(1);
			if (mmpair!=null) {
				fac.addMismatch(0, mmpair[0],mmpair[1],false); 
			}
		}

		reads.put(reg, fac.create());
		return true;
	}


	/**
	 * there must be at least one entry before start and one after it; 
	 * @param codons
	 * @param start
	 * @param end
	 * @param frame 1 if start is at position 1,4,7,... of an orf, 2 if at 2,5,8,...
	 * @return
	 */
	public double computeErrorOrf(double[] codons, int start, int end) {
		double frame1 = 0;
		double frame2 = 0;
		for (int c=start; c<end; c+=3) {
			frame1+=codons[c-1];
			frame2+=codons[c+1];
		}

		int frame = frame1>frame2?1:2;

		double[] p = new double[internalQuantiles.length];

		PValueCombiner pc = new PValueCombiner();

		for (int c=start; c<end; c+=3) {
			double sum = codons[c-1]+codons[c]+codons[c+1];
			if (sum>0) {
				for (int s=0; s<internalQuantiles.length; s++)
					p[s] = (frame==1?internalFrame1Quantiles[s]:internalFrame2Quantiles[s]).applyAsDouble(sum);
				correctIncreasing(p);
				int s = Arrays.binarySearch(p, codons[c]/sum);
				if (s<0) s=-s-2;
				else for (;s+1<internalQuantiles.length && p[s+1]<=codons[c]/sum; s++);

				double pval = s<0?1:1-internalQuantiles[s];
				pc.add(pval);
			}
		}

		return pc.combineFisher();
	}


	private void correctIncreasing(double[] p) {
		double max = p[0];
		for (int i=1; i<p.length; i++) {
			if (p[i]>max) max = p[i];
			else p[i] = max;
		}
	}

	public double getTrim() {
		return trim;
	}

	public double getThreshold() {
		return threshold;
	}

	public double getPresentPvalue(int length, int present) {
		return Binomial.cumulative(present, length, codonProbInBackGround, false, false);
	}
	
	public double getGapPvalue(int aalength, double meancov, int gaps) {
		return Beta.cumulative(gap99.applyAsDouble(Math.log10(meancov)), gaps+1,aalength-gaps+1,true,false);
	}
	public double getGapThreshold(double meancov) {
		return gap99.applyAsDouble(Math.log10(meancov));
	}
	
//	/**
//	 * Gets the likelihood of the beta function for the given number of gaps.
//	 * @param aalength length of the ORF in amino acids (w/o stop codon!!)
//	 * @param tmean trimmed mean of codon counts (according to {@link #getGapTrim()} and excluding all gaps!
//	 * @param gaps number of empty codons (i.e. less or equal to {@link #getGapThreshold()}
//	 * @return
//	 */
//	public double computeGapLikelihood(int aalength, double tmean, int gaps) {
//		return dbeta(gapMean.applyAsDouble(tmean), gapVar.applyAsDouble(tmean), gaps/(double)aalength);
//	}
//
//	/**
//	 * Gets the likelihood of the beta function for the given number of gaps. 
//	 * @param codons the whole ORFwithout the stop codon
//	 * @param start
//	 * @param end
//	 * @return
//	 */
//	public double computeGapLikelihood(double[] codons, int start, int end) {
//		double[] ccc = ArrayUtils.slice(codons, start,end);
//		Arrays.sort(ccc);
//		int ngaps = 0;
//		for (;ngaps<ccc.length && codons[ngaps]<=threshold; ngaps++);
//		double tmCov = NumericArrayFunction.trimmedMean(trim).applyAsDouble(NumericArray.wrap(ccc,ngaps,ccc.length));
//		return dbeta(gapMean.applyAsDouble(tmCov), gapVar.applyAsDouble(tmCov), ngaps/(double)codons.length);
//	}
//
//
//	/**
//	 * Gets the quantile of the beta function for the given number of gaps. Large values indicate that there are too many gaps.
//	 * @param aalength length of the ORF in amino acids (w/o stop codon!!)
//	 * @param tmean trimmed mean of codon counts (according to {@link #getGapTrim()} and excluding all gaps!
//	 * @param gaps number of empty codons (i.e. less or equal to {@link #getGapThreshold()}
//	 * @return
//	 */
//	public double computeGapQuantile(int aalength, double tmean, int gaps) {
//		return pbeta(gapMean.applyAsDouble(tmean), gapVar.applyAsDouble(tmean), gaps/(double)aalength);
//	}
//
//	/**
//	 * Gets the upper quantile of the beta function for the given number of gaps. Large values indicate that there are too many gaps.
//	 * @param aalength length of the ORF in amino acids (w/o stop codon!!)
//	 * @param tmean trimmed mean of codon counts (according to {@link #getGapTrim()} and excluding all gaps!
//	 * @param gaps number of empty codons (i.e. less or equal to {@link #getGapThreshold()}
//	 * @return
//	 */
//	public double computeGapPval(int aalength, double tmean, int gaps) {
//		if (gaps==0) return 1;
//		double re= pbetapval(gapMean.applyAsDouble(tmean), gapVar.applyAsDouble(tmean), gaps/(double)aalength);
//		return re;
//	}
//
//
//	/**
//	 * Gets the quantile of the beta function for the given number of gaps. Large values indicate that there are too many gaps.
//	 * @param codons the whole ORFwithout the stop codon
//	 * @param start
//	 * @param end
//	 * @return
//	 */
//	public double computeGapQuantile(double[] codons, int start, int end) {
//		double[] ccc = ArrayUtils.slice(codons, start,end);
//		Arrays.sort(ccc);
//		int ngaps = 0;
//		for (;ngaps<ccc.length && codons[ngaps]<=threshold; ngaps++);
//		double tmCov = NumericArrayFunction.trimmedMean(trim).applyAsDouble(NumericArray.wrap(ccc,ngaps,ccc.length));
//		return pbeta(gapMean.applyAsDouble(tmCov), gapVar.applyAsDouble(tmCov), ngaps/(double)codons.length);
//	}

	/**
	 * 
	 * @param codons the whole ORFwithout the stop codon
	 * @param start
	 * @param end
	 * @return
	 */
	public double computeStopScore(double[][] codons, int start, int end) {
		double re = 0;
		double[] tmCov = new double[codons.length];
		double[] codon = new double[codons.length];
		
		for (int c=0; c<codons.length; c++) {
			codon[c] = codons[c][end-1];
			
			double[] ccc = ArrayUtils.slice(codons[c], start,end);
			Arrays.sort(ccc);
//			int ngaps = 0;
//			for (;ngaps<ccc.length && codons[c][ngaps]<=threshold; ngaps++);
			tmCov[c] = NumericArrayFunction.trimmedMean(trim).applyAsDouble(NumericArray.wrap(ccc));//,ngaps,ccc.length));
		}

		for (CodonFeature f : features)
			if (!f.start)
				re+=f.compute(codon, tmCov, tmSums, threshold);
		
		return re;
	}

	public double computeStartScore(double[][] codons, int start, int end, boolean useSingle) {
		double[] tmCov = new double[codons.length];
		double[] codon = new double[codons.length];
		
		for (int c=0; c<codons.length; c++) {
			codon[c] = codons[c][start];
			
			double[] ccc = ArrayUtils.slice(codons[c], start,end);
//			Arrays.sort(ccc);
//			int ngaps = 0;
//			for (;ngaps<ccc.length && codons[c][ngaps]<=threshold; ngaps++);
			tmCov[c] = NumericArrayFunction.trimmedMean(trim).applyAsDouble(NumericArray.wrap(ccc));//,ngaps,ccc.length));
		}
		
		double re = 0;
		for (CodonFeature f : features)
			if (f.start && (useSingle || f.a!=f.b)) {
				double sc = f.compute(codon, tmCov, tmSums, threshold);
				re+=sc;
//				re = Math.max(re,sc);
//				System.out.println(f+"\t"+sc);
			}
		return re;
	}
	
	public double[] computeSvmLfcProbabilities(double[][] codons) {
		
		double[][] allProbs = lfcParam.prepareLfc(codons);
		double[] probs = new double[2];
		
		Svm_node[] x = new Svm_node[lfcParam.lfcPairs.length];
		double[] re = new double[codons[0].length];
		
		int lind = ArrayUtils.find(lfcParam.lfcModel.label, 1);
		
		for (int i=0; i<re.length; i++) {
			for (int k=0; k<x.length; k++) {
				x[k] = new Svm_node();
				x[k].index=k;
				x[k].value = allProbs[k][i];
			}
			Svm.svm_predict_probability(lfcParam.lfcModel, x, probs);
			
			re[i] = probs[lind];
		}
		return re;
	}
	
	public boolean hasLfc() {
		return lfcParam!=null;
	}
	
	
	public double computeSvmStartProbability(double[][] codons, int start, int end) {
		
		double[] probs = new double[2];
		
		
		ArrayList<Svm_node> x = buildFeatureVector(codons,start-startSvmFlank[0],start+startSvmFlank[1],startSvmConditions,startSvmSum);
		if (x.size()==0) return 0;
		
		
		Svm.svm_predict_probability(startSvm, x.toArray(new Svm_node[0]), probs);
		int lind = ArrayUtils.find(startSvm.label, 1);
		return probs[lind];
	}
	
	public static ArrayList<Svm_node> buildFeatureVector(double[][] codons,
			int start, int stop, int[] conditionVector, double[] sizeFactors) {
		ArrayList<Svm_node> x = new ArrayList<Svm_node>();
		double s = 0;
		int ind = 0;
		
		for (int p=start; p<=stop; p++) {
			for (int c=0; c<codons.length; c++)
				if (p>=0 && p<codons[c].length)
					s+=codons[c][p]/sizeFactors[c];
			
			for (int c : conditionVector) {
				ind++;
				if (p>=0 && p<codons[c].length && codons[c][p]>0) {
					Svm_node n = new Svm_node();
					n.index = ind;
					n.value = codons[c][p]/sizeFactors[c];
					
					x.add(n);
				}
					
			}
		}
		for (Svm_node n : x)
			n.value /= s;
		return x;
	}

	public double getSvmStartCutoff() {
		return startSvmProbCutoff;
	}
	
	public double getSvmLfcCutoff() {
		return lfcParam.lfcCutoff;
	}
	
	private PreparedIntKernel changePointKernel;

	
	
	public double computeChangePointScore(double[] codons, int start, int end) {
		if (changePointKernel==null) changePointKernel = new GaussianKernel(changepointBandwidth).prepare();
		
		double chpLeft = 0;
		double chpRight = 0;
		for (int p=Math.max(0, start-changePointKernel.getMaxAffectedIndex(0)); p<start; p++)
			chpLeft+=codons[p]*changePointKernel.applyAsDouble(start-p+1);
		for (int p=start; p<Math.min(end,start+changePointKernel.getMaxAffectedIndex(0)); p++)
			chpRight+=codons[p]*changePointKernel.applyAsDouble(p-start);
		
		double pmean = (PolyGamma.digamma(chpRight+threshold+1)-PolyGamma.digamma(chpLeft+threshold+1))/Math.log(2);
		double psd = Math.sqrt((PolyGamma.trigamma(chpRight+threshold+1)+PolyGamma.trigamma(chpLeft+threshold+1))/Math.log(2)/Math.log(2));
		
		// compute likelihoods as integral f(x)g(x) dx, where f(x) is the codon feature distribution and g(x) is the approximated posterior distribution
		// f(x)g(x) of two gaussians is a scaled gaussian, therefore the integral reduces to the scaling factor, which is the density at mean_f for Normal(mean=mean_g,sd=sqrt(sd_f+sd(g)) 
		double H1 = Normal.density(changepointMean, pmean, Math.sqrt(changepointSd+psd), true);
		double H0 = Normal.density(0, pmean, Math.sqrt(changepointSd+psd), true);
		return H1-H0;
	}


	//	/**
	//	 * Returns [ngaps,tmean,gapPval,startPval,stopPval]
	//	 * @param codons
	//	 * @param start
	//	 * @param end
	//	 * @return
	//	 */
	//	public double[] computeInfo(double[] codons, int start, int end) {
	//		double[] ccc = ArrayUtils.slice(codons, start,end);
	//		Arrays.sort(ccc);
	//		int ngaps = 0;
	//		for (;ngaps<ccc.length && ccc[ngaps]<=threshold; ngaps++);
	//		double tmCov = NumericArrayFunction.trimmedMean(trim).applyAsDouble(NumericArray.wrap(ccc,ngaps,ccc.length));
	//		return new double[] {ngaps,tmCov,computeGapPval(end-start, tmCov, ngaps),computeStartPval(tmCov, codons[start]), computeStopPval(tmCov, codons[end-1])};
	//	}

	private static double dbeta(double mean, double var, double x) {
		double c = mean*(1-mean)/var-1;
		double alpha = mean*c;
		double beta = (1-mean)*c;
		return Beta.density(x, alpha, beta, false);
	}
	private static double pbeta(double mean, double var, double x) {
		mean = Math.max(mean,0);
		double c = mean*(1-mean)/var-1;
		double alpha = mean*c;
		double beta = (1-mean)*c;
		return Beta.cumulative(x, alpha, beta, true, false);
	}

	private static double pbetapval(double mean, double var, double x) {
		mean = Math.max(mean,0);
		double c = mean*(1-mean)/var-1;
		double alpha = mean*c;
		double beta = (1-mean)*c;
		return Beta.cumulative(x, alpha, beta, false, false);
	}


	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putDouble(u);
		out.putCInt(Pl.length);
		for (int i=0; i<Pl.length; i++)
			out.putDouble(Pl[i]);
		for (int i=0; i<Pl.length; i++)
			out.putDouble(Pr[i]);

		out.putCInt(probs.size());
		for (Integer k : probs.keySet()) {
			out.putInt(k);
			double[] a = probs.get(k);
			out.putCInt(a.length);
			for (double d : a)
				out.putDouble(d);
		}

		if (isErrorSet()) {
			long pos = out.position();
			out.putLong(0);
			out.putDouble(trim);
			out.putDouble(threshold);
			out.putDouble(changepointMean);
			out.putDouble(changepointSd);
			out.putInt(changepointBandwidth);
			out.putDouble(codonProbInBackGround);
			out.putDouble(startSvmProbCutoff);
			out.putDouble(inferenceLamdba);
			OrmSerializer seri = new OrmSerializer(false,true);
			
			String[] simpleSpec = simple!=null?simple.getSpec():new String[0];
			
			seri.serializeAll(out, EI.wrap(gap99,gof99,gofCod,internalQuantiles,internalFrame1Quantiles,internalFrame2Quantiles, 
					features, tmSums, startSvm, startSvmSum, startSvmFlank, startSvmConditions,simpleSpec, lfcParam));
			out.putLong(pos, out.position());
		}
		else
			out.putLong(-1);

	}


	@Override
	public void deserialize(BinaryReader in) throws IOException {
		u = in.getDouble();
		Pl = new double[in.getCInt()];
		for (int i=0; i<Pl.length; i++) 
			Pl[i] = in.getDouble();
		Pr = new double[Pl.length];
		for (int i=0; i<Pl.length; i++) 
			Pr[i] = in.getDouble();

		int size = in.getCInt();
		for (int i=0; i<size; i++) {
			int k = in.getInt();
			double[] a = new double[in.getCInt()];
			for (int j = 0; j < a.length; j++) {
				a[j] = in.getDouble();
			}
			probs.put(k,a);
		}

		long posafter = in.getLong();
		if (posafter!=-1 && !in.getContext().getGlobalInfo().getEntry(OMIT_READ_ERROR_OPTION).asBoolean()) {
			
			trim = in.getDouble();
			threshold = in.getDouble();
			changepointMean = in.getDouble();
			changepointSd = in.getDouble();
			changepointBandwidth = in.getInt();
			codonProbInBackGround = in.getDouble();
			startSvmProbCutoff = in.getDouble();
			inferenceLamdba = in.getDouble();
			
			OrmSerializer seri = new OrmSerializer(false,true);
			ExtendedIterator<?> it = seri.deserializeAll(in);
//			gapMean = (PiecewiseLinearFunction) it.next();
//			gapVar = (PiecewiseLinearFunction) it.next();
			gap99 = (PiecewiseLinearFunction) it.next();
			gof99 = (PiecewiseLinearFunction) it.next();
			gofCod = (PiecewiseLinearFunction) it.next();
			internalQuantiles = (double[]) it.next();
			internalFrame1Quantiles = (PiecewiseLinearFunction[]) it.next();
			internalFrame2Quantiles = (PiecewiseLinearFunction[]) it.next();
			features = (CodonFeature[]) it.next();
			tmSums = (double[]) it.next();
			startSvm = (Svm_model) it.next();
			startSvmSum = (double[]) it.next();
			startSvmFlank = (int[]) it.next();
			startSvmConditions = (int[]) it.next();
			String[] simpleSpec = (String[]) it.next();
			if (simpleSpec.length>0)
				simple = new SimpleCodonModel(simpleSpec);
			else
				simple = null;
			lfcParam = (LfcParameters) it.next();
			
		}
		if (posafter!=-1)
			in.position(posafter);

	}

	@Override
	public String toString() {
		return String.format(Locale.US,"U=%.3f\nPl=%s\nPr=%s",u,Arrays.toString(Pl),Arrays.toString(Pr));
	}

	public String toTable() {
		StringBuilder sb = new StringBuilder();
		sb.append("Read length\tLeading mismatch\tPosition\tProbability\n");
		for (Integer k : probs.keySet()) {
			double[] a = probs.get(k);
			for (int i = 0; i < a.length; i++) 
				sb.append(Math.abs(k)).append("\t").append(k<0?"true":"false").append("\t").append(i).append("\t").append(a[i]).append("\n");
		}
		return sb.toString();
	}


	public static void main(String[] args) throws IOException {
		if (args.length!=1)System.exit(1);
		PageFile p = new PageFile(args[0]);
		while (!p.eof()) {
			RiboModel m = new RiboModel();
			m.deserialize(p);
			System.out.println(m.toString());
		}
		p.close();
	}

	/**
	 * The first contains the error estimates (if any)
	 * @param file
	 * @param readError
	 * @return
	 * @throws IOException
	 */
	public static RiboModel[] fromFile(String file, boolean readError) throws IOException {
		ArrayList<RiboModel> re = new ArrayList<RiboModel>();
		PageFile pf = new PageFile(file);
		if (!readError)
			pf.getContext().setGlobalInfo(DynamicObject.parseJson("{\"OMIT_READ_ERROR_OPTION\": true}"));
		
		while (!pf.eof()) {
			RiboModel model = new RiboModel();
			model.deserialize(pf);
			pf.getContext().setGlobalInfo(DynamicObject.parseJson("{\"OMIT_READ_ERROR_OPTION\": true}"));
			re.add(model);
		}
		
		pf.close();
		return re.toArray(new RiboModel[0]);
	}




	public static class CodonFeature {
		public int a;
		public int b;

		public double mean;
		public double sd;
		
		public boolean start;
		
		public CodonFeature(int a, int b, double mean, double sd, boolean start) {
			this.a = a;
			this.b = b;
			this.mean = mean;
			this.sd = sd;
			this.start = start;
		}


		public double compute(double[] codon, double[] tmCov, double[] tmSums, double threshold) {
			if (codon[a]<threshold && codon[b]<threshold)
				return 0;
			
			if (a==b) {
				if (tmCov[a]==0 || Double.isNaN(tmCov[a])) return 0;
				// double enr = Math.log((codon[a]+threshold)/tmCov[a]);
				// return (Normal.density(enr/Math.log(2), mean, sd, true)-Normal.density(enr/Math.log(2), 0, sd, true))*weights[a];
				// posterior distribution of lfc codon / coverage
				double pmean = (PolyGamma.digamma(codon[a]+threshold+1)-PolyGamma.digamma(tmCov[a]+threshold+1))/Math.log(2);
				double psd = Math.sqrt((PolyGamma.trigamma(codon[a]+threshold+1)+PolyGamma.trigamma(tmCov[a]+threshold+1))/Math.log(2)/Math.log(2));
				
				// compute likelihoods as integral f(x)g(x) dx, where f(x) is the codon feature distribution and g(x) is the approximated posterior distribution
				// f(x)g(x) of two gaussians is a scaled gaussian, therefore the integral reduces to the scaling factor, which is the density at mean_f for Normal(mean=mean_g,sd=sqrt(sd_f+sd(g)) 
				double H1 = Normal.density(mean, pmean, Math.sqrt(sd+psd), true);
				double H0 = Normal.density(0, pmean, Math.sqrt(sd+psd), true);
				
				return H1-H0;
			}
			
//			double enr = Math.log(codon[a]+threshold)-Math.log(codon[b]+threshold)-Math.log(tmSums[a]+threshold)+Math.log(tmSums[b]+threshold);
//			return (Normal.density(enr/Math.log(2), mean, sd, true)-Normal.density(enr/Math.log(2), 0, sd, true))*(weights[a]+weights[b]);
			
			// set priors to null model!
			double pseudoa = tmSums[a];
			double pseudob = tmSums[b];
			double f = 1/Math.exp((Math.log(pseudoa)+Math.log(pseudob))/2);
			
			// treat the tmSums as constants, not as random variables (they are large enough to do so!)
			double pmean = (PolyGamma.digamma(codon[a]+pseudoa*f+1)-PolyGamma.digamma(codon[b]+pseudob*f+1) -Math.log(tmSums[a]+threshold)+Math.log(tmSums[b]+threshold))/Math.log(2);
			double psd = Math.sqrt((PolyGamma.trigamma(codon[a]+pseudoa*f+1)+PolyGamma.trigamma(codon[b]+pseudob*f+1))/Math.log(2)/Math.log(2));
			
			// compute likelihoods as integral f(x)g(x) dx, where f(x) is the codon feature distribution and g(x) is the approximated posterior distribution
			// f(x)g(x) of two gaussians is a scaled gaussian, therefore the integral reduces to the scaling factor, which is the density at mean_f for Normal(mean=mean_g,sd=sqrt(sd_f+sd(g)) 
			double H1 = Normal.density(mean, pmean, Math.sqrt(sd+psd), true);
			double H0 = Normal.density(0, pmean, Math.sqrt(sd+psd), true);
			
			
			
			return H1-H0;
		}


		@Override
		public String toString() {
			return "CodonFeature [a=" + a + ", b=" + b + ", mean=" + mean
					+ ", sd=" + sd + ", start=" + start + "]";
		}

	}




//	public boolean hasStartPairs() {
//		for (CodonFeature f : features)
//			if (f.start && f.a!=f.b)
//				return true;
//		return false;
//	}

	public static RiboModel merge(RiboModel[] models, double[] weights) {
		double sw = ArrayUtils.sum(weights);
		
		double u = 0;
		double[] Pl = new double[models[0].Pl.length];
		double[] Pr = new double[models[0].Pr.length];
		
		for (int i=0; i<models.length; i++) {
			RiboModel m = models[i];
			double w = weights[i]/sw;
			u+=m.u*w;
			for (int j=0; j<Pl.length; j++)
				Pl[j]+=m.Pl[j]*w;
			for (int j=0; j<Pr.length; j++)
				Pr[j]+=m.Pr[j]*w;
		}
		
		return new RiboModel(Pl, Pr, u);
	}


	

}
