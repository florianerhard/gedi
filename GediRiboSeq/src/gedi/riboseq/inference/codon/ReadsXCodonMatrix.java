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

package gedi.riboseq.inference.codon;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.riboseq.cleavage.RiboModel;
import gedi.riboseq.utils.RiboUtils;
import gedi.util.ArrayUtils;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.functions.EI;
import gedi.util.mutable.MutableDouble;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;



/**
 * sparse reads x codons matrix, each entry can hold two doubles
 * slot 0 contains normalized probabilities (such that the sum over all reads for one codon is 1)
 * also contains a vector of observed count per read 
 * and a vector for the codon activities (initially all 1) 
 * @author erhard
 *
 */
public class ReadsXCodonMatrix {

	
	private HashMap<Codon,HashMap<Read,double[]>> M = new HashMap<Codon, HashMap<Read,double[]>>();
	private HashMap<Read,HashMap<Codon,double[]>> I = new HashMap<Read, HashMap<Codon,double[]>>();
	private HashMap<Read,Read> readProto = new HashMap<Read,Read>();
	private HashMap<Codon,Codon> codonProto = new HashMap<Codon,Codon>();
	
	private double total = 0;
	
	// lm,len,vector of positions
	private int[][][] probableCodonPositionsPerLength;
	private double[][][] probableCodonPositionProbabilitiesPerLength;
	
	private double[] posteriorHashed;
	
	
	private RiboModel model;
	private int condition;
	
	public ReadsXCodonMatrix(RiboModel model, int condition) {
		this(model,condition, 1E-3);
	}
	public ReadsXCodonMatrix(RiboModel model, int condition, double min) {
		this.model = model;
		this.condition = condition;
		probableCodonPositionsPerLength = new int[2][model.getObservedMaxLength()+1][];
		probableCodonPositionProbabilitiesPerLength = new double[2][model.getObservedMaxLength()+1][];
		
		posteriorHashed = new double[maxHash()];
		
		for (int lm=0; lm<2; lm++) {
			for (int l=0; l<probableCodonPositionProbabilitiesPerLength[lm].length; l++) {
				
				IntArrayList pos = new IntArrayList();
				DoubleArrayList prob = new DoubleArrayList();
				
				double[] mo = model.getModel(lm==1, l);
				if (mo!=null)
					for (int i=0; i<mo.length; i++) {
						if (mo[i]>=min) {
							pos.add(i);
							double post = model.getPosterior(lm==1, l, i);
							prob.add(post);
							posteriorHashed[computeHash(lm, l, i)] = post;
						}
					}
				
				probableCodonPositionsPerLength[lm][l] = pos.toIntArray();
				probableCodonPositionProbabilitiesPerLength[lm][l] = prob.toDoubleArray();
				ArrayUtils.mult(probableCodonPositionProbabilitiesPerLength[lm][l], 1/ArrayUtils.sum(probableCodonPositionProbabilitiesPerLength[lm][l]));
			}
		}
		
		
		// probableCodonPositionProbabilitiesPerLength sums (almost) to 1
	}
	
	
	public int addAll(
			Iterator<? extends ReferenceGenomicRegion<AlignedReadsData>> reads) {
		ReferenceSequence ref = null;
		int re = 0;
		while (reads.hasNext()) {
			ReferenceGenomicRegion<AlignedReadsData> n = reads.next();
			
			if (ref==null) ref = n.getReference();
			else if (!ref.equals(n.getReference())) throw new RuntimeException("Dont mix chromosomes!");
			addRead(n);
			re++;
			
		}
			
		return re;
	}
	
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		Codon[] cod = codonProto.keySet().toArray(new Codon[0]);
		Arrays.sort(cod);
		for (Codon c : cod) {
			sb.append(c.toRegionString()+":");
			Read[] rea = M.get(c).keySet().toArray(new Read[0]);
			Arrays.sort(rea);
			for (Read r : rea) {
				sb.append(" ").append(r.region.toRegionString()).append("->").append(String.format(Locale.US, "%.2f|%.1f",M.get(c).get(r)[0],M.get(c).get(r)[1]));
			}
			sb.append("\n");
		}
		return sb.toString();
	}
	
	
	boolean plusStrand;
	
	public void addRead(ReferenceGenomicRegion<AlignedReadsData> rgr) {
		
		if (!model.isValidReadLength(rgr.getRegion().getTotalLength())) return;
		
		plusStrand = rgr.getReference().getStrand()==Strand.Plus;
		
		if (rgr.getRegion().getTotalLength()>=probableCodonPositionsPerLength[0].length)
			return;
		for (int d=0; d<rgr.getData().getDistinctSequences(); d++) {
			Read pr = new Read(rgr, d);
			if (pr.region.getTotalLength()>=probableCodonPositionsPerLength[0].length)
				continue;
			
			Read r = readProto.get(pr);
			if (r==null) { 
				readProto.put(pr, pr);
				r=pr;
				
				int lm = r.leadingMismatch?1:0;
				int l = r.region.getTotalLength();
				int[] pos = probableCodonPositionsPerLength[lm][l];
				double[] probs = probableCodonPositionProbabilitiesPerLength[lm][l];

				for (int i=0; i<pos.length; i++) {
					ArrayGenomicRegion cp = plusStrand?
							new ArrayGenomicRegion(pos[i],pos[i]+3)
							:new ArrayGenomicRegion(r.region.getTotalLength()-pos[i]-3,r.region.getTotalLength()-pos[i]);
					Codon c = codonProto.computeIfAbsent(new Codon(r.region.map(cp),1), a->a);

					double[] s = {probs[i],0,0};
					M.computeIfAbsent(c, a->new HashMap<Read, double[]>()).put(r,s);
					I.computeIfAbsent(r, a->new HashMap<Codon, double[]>()).put(c,s);
				}
				
			}
			
			if (condition==-1) {
				double c = rgr.getData().getTotalCountForDistinct(d, ReadCountMode.Weight);// getSumCount(d,true);
				r.totalCount+=c;
				r.count = rgr.getData().addCountsForDistinct(d, r.count, ReadCountMode.Weight);
				total+=c;
			}
			else {
				double c = rgr.getData().getCount(d,condition, ReadCountMode.Weight);// getSumCount(d,true);
				r.totalCount+=c;
				r.count = rgr.getData().addCountsForDistinct(d, r.count, ReadCountMode.Weight);
				total+=c;
			}
		}
	}
	
	private int computeHash(int lm, int l, int p) {
		int m = probableCodonPositionsPerLength[0].length;
		return lm*m*m+l*m+p;
	}
	
	private int maxHash() {
		int m = probableCodonPositionsPerLength[0].length;
		return 1*m*m+m*m+m;
	}

	
//	private double[] first;
//	private HashMap<Codon,Integer> c2i = new HashMap<>();
//	private HashMap<Integer,ArrayList<Codon>> startMap = new HashMap<>();
//	private HashMap<Integer,ArrayList<Codon>> endMap = new HashMap<>();
	
	
	public void finishReads() {
//		first = new double[codonProto.size()];
//		int ind = 0;
//		for (Codon c : codonProto.keySet()) {
//			for (Read r : M.get(c).keySet())
//				if (M.get(c).get(r)[0]>0)
//					first[ind]+=r.totalCount*M.get(c).get(r)[0];
//			c2i.put(c, ind);
//			startMap.computeIfAbsent(c.getStart(), x->new ArrayList<>()).add(c);
//			endMap.computeIfAbsent(c.getEnd(), x->new ArrayList<>()).add(c);
//			ind++;
//		}
		
//		for (Codon c : codonProto.keySet()) {
//			codonLL.put(c, new MutableDouble(Double.NaN));
//		}
//		for (Codon c : codonProto.keySet()) {
//			double sum = 0;
//			for (double[] s : M.get(c).values())
//				sum+=s[0];
//			R.put(c, 1-sum);
//			for (double[] s : M.get(c).values())
//				s[0]/=sum;
//		}
		
	}
	
	/**
	 * Checks whether all reads have the same number of conditions (and returns this number); returns -1 otherwise;
	 * Returns -2, if no reads were added
	 * @return
	 */
	public int checkConditions() {
		int re = -2;
		for (Read r : readProto.keySet()) {
			if (re==-2)
				re = r.count.length;
			else if (re>=0 && re!=r.count.length)
				re = -1;
		}
		
		if (re>=0)
			for (Codon c : codonProto.keySet()) 
				c.activity = new double[re];
		return re;
	}
	
	
	public void copySlots(int from, int to) {
		for (Codon c : codonProto.keySet()) {
			for (double[] s : M.get(c).values())
				s[to] = s[from];
		}
	}
	
	
	public void copySlotsCodons(Set<Codon> affected, int from, int to) {
		for (Codon c : affected) {
			for (double[] s : M.get(c).values())
				s[to] = s[from];
		}
	}
	
	public void copySlotsReads(Set<Read> affected, int from, int to) {
		for (Read r : affected) {
			for (double[] s : I.get(r).values())
				s[to] = s[from];
		}
	}
	
	public void copySlotsCodons(Read fromRead, int from, int to) {
		for (Codon c : I.get(fromRead).keySet()) {
			for (double[] s : M.get(c).values())
				s[to] = s[from];
		}
	}
	
	public void copySlotsReads(Codon fromCodon, int from, int to) {
		for (Read r : M.get(fromCodon).keySet()) {
			for (double[] s : I.get(r).values())
				s[to] = s[from];
		}
	}
	

	/**
	 * multiply the current codon activity with the probs in slot 0 and store in slot 1
	 */
	public void computeExpectedReadsPerCodon() {
		// for each codon: iterate over entries and multiply by codon activity 
		for (Codon c : codonProto.keySet()) {
			for (double[] s : M.get(c).values())
				s[1] = s[0]*c.totalActivity;
		}
	}
	
	public double computeLogLikelihood() {
		double re = 0;
		for (Read r : I.keySet()) {
			re+=computeReadLogLikelihood(r);
		}
		return re;
	}
	private double computeReadLogLikelihood(Read r) {
		double re = 0;
		HashMap<Codon, double[]> codons = I.get(r);
		for (Codon c : codons.keySet()) {
			re+=c.totalActivity*codons.get(c)[0];
		}
		return r.totalCount*Math.log(re);
	}
	
//	private HashMap<Codon,MutableDouble> codonLL = new HashMap<Codon, MutableDouble>();
//	/**
//	 * Recalculate only for given codons
//	 * @param recalc
//	 * @return
//	 */
//	public double computeLogLikelihood(HashSet<Codon> recalc) {
//		double s = 0;
//		for (Codon c : codonProto.keySet()) {
//			MutableDouble cll = codonLL.get(c);
//			if (recalc.contains(c))
//				cll.N = computeCodonLogLikelihood(c);
//			s+=cll.N;
////			System.out.println(c+" "+computeCodonLogLikelihood(c));
//		}
//		return s;
//	}
//	
//	public double computeLogLikelihood() {
//		double s = 0;
//		for (Codon c : codonProto.keySet()) {
//			MutableDouble cll = codonLL.get(c);
//			cll.N = computeCodonLogLikelihood(c);
//			s+=cll.N;
////			System.out.println(c+" "+computeCodonLogLikelihood(c));
//		}
//		return s;
//	}
//	
//	
//	public double computeCodonLogLikelihood(Codon codon) {
//		double s = 0;
//		double sa = 0;
//		double gs = 0;
//		for (double[] d : M.get(codon).values()) {
//			s+=(d[1])*Math.log(d[0]);
//			sa += d[1];
//			gs += Gamma.logGamma(d[1]+1);
//		}
//		return sa==0?0:(Gamma.logGamma(sa+1)-gs+s);
//	}
	
	
	public void removeZeroCodons() {
		Iterator<Codon> it = codonProto.keySet().iterator();
		while (it.hasNext()) {
			Codon c = it.next();
			if (c.totalActivity==0) {
				it.remove();
				for (Read r : M.remove(c).keySet()) 
					I.get(r).remove(c);
			}
		}
	}

	public void resetCodons() {
		Iterator<Codon> it = codonProto.keySet().iterator();
		while (it.hasNext()) {
			Codon c = it.next();
			c.totalActivity = 1;
		}
	}

	
	public HashSet<Codon> regularize(Codon codon) {
		HashSet<Codon> re = new HashSet<Codon>();
		re.add(codon);
		
		HashMap<Read, double[]> rs = M.get(codon);
		for (Read r : rs.keySet()) {
			double[] d = rs.get(r);
			// try to redistribute d[1] to other codons
			HashMap<Codon,double[]> cs = I.get(r);
			double s = 0;
			for (Codon c : cs.keySet()) {
				re.add(c);
				double[] d2 = cs.get(c);
				if (d2!=d) 
					s+=d2[1];
			}
			if (s==0) return null;
			for (Codon c : cs.keySet()) {
				double[] d2 = cs.get(c);
				if (d2!=d) 
					d2[1]+=d[1]*d2[1]/s;
			}
			d[1] = 0;
		}
		return re;
	}
	
	public double regularize2(Codon codon) {
		
		double deltaLL = 0;
		HashMap<Read, double[]> rs = M.get(codon);
		for (Read r : rs.keySet()) {
			double[] d = rs.get(r);
			if (d[1]==0) continue;
			
			// try to redistribute d[1] to other codons
			HashMap<Codon,double[]> cs = I.get(r);
			double s = 0;
			for (Codon c : cs.keySet()) {
				double[] d2 = cs.get(c);
				if (d2!=d) 
					s+=d2[1];
			}
			if (s==0) return Double.NEGATIVE_INFINITY;
			
			double beforesum = 0;
			double aftersum = 0;
			for (Codon c : cs.keySet()) {
				double[] d2 = cs.get(c);
				beforesum+=c.totalActivity*d2[0];
				if (d2!=d) {
					aftersum+=(c.totalActivity+d[1]*d2[1]/s)*d2[0];
					d2[1]+=d[1]*d2[1]/s;
				}
			}
			deltaLL+=Math.log(aftersum)-Math.log(beforesum);
			d[1] = 0;
		}
//		int deltaparam = -rs.keySet().size(); 
//		return 2*deltaparam-2*deltaLL; // == AIC_after - AIC_before, i.e. regularization is successful if this is negative
		return deltaLL;
	}

	public double regularize3(Codon codon) {
		
		
		double deltaLL = 0;
		HashMap<Read, double[]> rs = M.get(codon);
		HashMap<Codon,MutableDouble> ntotal = new HashMap<Codon, MutableDouble>();
		
		for (Read r : rs.keySet()) {
			double[] d = rs.get(r);
			if (d[1]==0) continue;
			
			// try to redistribute d[1] to other codons
			HashMap<Codon,double[]> cs = I.get(r);
			double s = 0;
			for (Codon c : cs.keySet()) {
				double[] d2 = cs.get(c);
				if (d2!=d) 
					s+=d2[1];
			}
			if (s==0) 
				return Double.NEGATIVE_INFINITY; // cannot distribute read to another codon!
			
			double beforesum = 0;
			for (Codon c : cs.keySet()) {
				double[] d2 = cs.get(c);
				beforesum+=c.totalActivity*d2[0];
				if (d2!=d) {
					ntotal.computeIfAbsent(c, x->new MutableDouble(c.totalActivity)).N+=d[1]*d2[1]/s;
					d2[1]+=d[1]*d2[1]/s;
				}
			}//JN555585:112387-112922
			deltaLL+=r.totalCount*(-Math.log(beforesum));
		}
		for (Read r : rs.keySet()) {
			double[] d = rs.get(r);
			if (d[1]==0) continue;
			
			HashMap<Codon,double[]> cs = I.get(r);
			double aftersum = 0;
			for (Codon c : cs.keySet()) {
				double[] d2 = cs.get(c);
				if (d2!=d) {
					aftersum+=ntotal.get(c).N*d2[0];
				}
			}
			deltaLL+=r.totalCount*(Math.log(aftersum));

			d[1] = 0;
		}
		
//		double deltaparam = -total; 
//		return 2*deltaparam-2*deltaLL; // == AIC_after - AIC_before, i.e. regularization is successful if this is negative
		return deltaLL;//codon.totalActivity;
	}
	
	public void prepareGoodnessOfFit() {
		computePriorReadProbabilities();
		computeExpectedCodonPerRead();
	}
	
	
	public double computeGoodnessOfFit(Collection<Codon> codons) {
		double[] obs = new double[maxHash()];
		
		for (Codon c : codons) {
			if (M.containsKey(c))
				for (Read r : M.get(c).keySet()) {
					int lm = r.leadingMismatch?1:0;
					int l = r.region.getTotalLength();
					int p = plusStrand?r.region.induce(c.getStart()):(r.region.getTotalLength()-1-r.region.induce(c.getStop()));
					int hash = computeHash(lm, l, p);
					obs[hash]+=M.get(c).get(r)[1];
				}
		}
		
		double corr = ArrayUtils.sum(obs)/ArrayUtils.sum(posteriorHashed);
		double ss = 0;
		for (int i=0; i<obs.length; i++) {
			double e = posteriorHashed[i]*corr;
			double oe = obs[i]-e;
			if (e>0) {
				ss+=oe*oe/e;
			}
		}
		
		return ss;
	}
	
	public void computeGoodnessOfFit() {
		computePriorReadProbabilities();
		computeExpectedCodonPerRead();
		for (Codon c : codonProto.keySet()) {
			double ps = 0; // sum of propensities
			double cs = 0; // sum of expected counts
			for (double[] r : M.get(c).values()) {
				ps+=r[0];
				cs+=r[1];
			}
			if (ps>1) {
				// can be, a codon may occur at two reads of same length at the same position due to alternative splicing
				// underestimate here!
				ps=1;
			}
			double s = (1-ps)*cs;
			for (double[] r : M.get(c).values()) {
				double e = r[0]*cs;
				double o = r[1];
				double oe = o-e;
				s+=oe*oe/e;
			}
			c.goodness = s;
		}
		
	}
	
//	private static double logddirich(double[] alpha1, double[] p) {
//		double re = 0;
//		double asum = 0;
//		double gsum = 0;
//		for (int i=0; i<p.length; i++) {
//			re+=Math.log(p[i])*(alpha1[i]);
//			asum+=alpha1[i]+1; 
//			gsum+=Gamma.logGamma(alpha1[i]+1);
//		}
//		return re+Gamma.logGamma(asum)-gsum;
//	}
	
	private double chisquarePvalue(double df, double x) {
        if (x <= 0)
            return 1;
        return 1-org.apache.commons.math3.special.Gamma.regularizedGammaP(df/2, x/2);
	}

	/**
	 * for each read: computes the sum of activities for each frame and multiplies slot 1 with the corresponding fraction
	 * this tends to put all weight onto a single frame...  
	 */
	public void computeFrameWeightProbabilities() {
		for (Read r : readProto.keySet()) {
			double[] sum = {0,0,0};
			for (Codon c : I.get(r).keySet()) {
				int f = r.region.induce(c.getStart())%3;
				sum[f]+=c.totalActivity;
			}
			double sumsum = ArrayUtils.sum(sum);
			for (Codon c : I.get(r).keySet()) {
				int f = r.region.induce(c.getStart())%3;
				I.get(r).get(c)[1]*=sum[f]/sumsum;
			}
		}
	}
	
	/**
	 * normalize slot 1 s.t. sums are 1 for each read
	 */
	public void computePriorReadProbabilities() {
		for (Read r : readProto.keySet()) {
			double sum = 0;
			for (double[] s : I.get(r).values())
				sum+=s[1];
			if (sum>0)
				for (double[] s : I.get(r).values())
					s[1]/=sum;
		}
	}


	/**
	 * multiply slot 1 by the corresponding read count
	 */
	public void computeExpectedCodonPerRead() {
		for (Read r : readProto.keySet()) {
			for (double[] s : I.get(r).values())
				s[1] *= r.totalCount;
		}
	}

	/**
	 * multiply slot 1 by the corresponding read count from condition index; overwrites slot 1
	 */
	public void computeExpectedCodonPerRead(int index) {
		for (Read r : readProto.keySet()) {
			for (double[] s : I.get(r).values())
				s[1] *= r.count[index];
		}
	}
	
	public double getTotal() {
		return total;
	}

	public void computeExpectedCodons(Codon codon) {
		HashSet<Codon> codons = new HashSet<Codon>();
		for (Read r : M.get(codon).keySet()) {
			codons.addAll(I.get(r).keySet());
		}
		for (Codon c : codons) {
			double prev = c.totalActivity;
			c.totalActivity=0;
			for (double[] s : M.get(c).values())
				c.totalActivity += s[1];
		}
	}
	
	/**
	 * sum slot 1 for each codon and store in the codon activity vector
	 * returns the sum of the absolute differences to the previous vector 
	 * @return
	 */
	public double computeExpectedCodons() {
		double re = 0;
		for (Codon c : codonProto.keySet()) {
			double prev = c.totalActivity;
			c.totalActivity=0;
			for (double[] s : M.get(c).values())
				c.totalActivity += s[1];
			re=Math.max(re,Math.abs(prev-c.totalActivity));
		}
		return re;
	}
	
//	public double applyPrior(double rho, double[] before) {
//		double total = 0;
//		for (Codon c : codonProto.keySet())
//			total+=c.totalActivity;
//		
//		double sum = 0;
//		int ind = 0;
//		for (Codon c : codonProto.keySet()) {
//			double lc = EI.wrap(endMap.get(c.getStart())).mapToDouble(cx->cx.totalActivity).sum();
//			double rc = EI.wrap(startMap.get(c.getEnd())).mapToDouble(cx->cx.totalActivity).sum();
//			double lf = EI.wrap(endMap.get(c.getStart())).mapToDouble(cx->first[c2i.get(cx)]).sum();
//			double rf = EI.wrap(startMap.get(c.getEnd())).mapToDouble(cx->first[c2i.get(cx)]).sum();
//			
//			//double a = c.totalActivity-rho*first[ind++];
//			
//			double a = (first[ind++]+rf+lf)/(c.totalActivity+rc+lc);
//			a = c.totalActivity*(1-rho*a);
//			if (c.totalActivity+rc+lc==0)
//				a = 0;
//			c.goodness = Math.max(0, a);
//			sum+=c.goodness;
//		}
//		
//		double re = 0;
//		ind = 0;
//		for (Codon c : codonProto.keySet()) {
//			c.totalActivity=c.goodness/sum*total;
//			re=Math.max(re,Math.abs(before[ind++]-c.totalActivity));
//		}
//		return re;
//	}
//	
//	public double[] getCurrentActivities(double[] act) {
//		if (act==null || act.length!=codonProto.size()) act = new double[codonProto.size()];
//		int ind = 0;
//		for (Codon c : codonProto.keySet()) 
//			act[ind++]=c.totalActivity;
//		return act;
//	}
	
	/**
	 * sum slot 1 for each codon and store in the codon activity vector at index index
	 * returns the sum of the absolute differences to the previous vector 
	 * @return
	 */
	public double computeExpectedCodons(int index) {
		double re = 0;
		for (Codon c : codonProto.keySet()) {
			double prev = c.activity[index];
			c.activity[index]=0;
			for (double[] s : M.get(c).values())
				c.activity[index] += s[1];
			re+=Math.abs(prev-c.activity[index]);
		}
		return re;
	}
	
	public Set<Codon> getCodons() {
		return codonProto.keySet();
	}

	
	private static class Read implements Comparable<Read> {
		private GenomicRegion region;
		private boolean leadingMismatch;
		private int hashcode;
		private double totalCount;
		private double[] count;
		
		public Read(ReferenceGenomicRegion<AlignedReadsData> rgr, int distinct) {
			this.leadingMismatch = RiboUtils.hasLeadingMismatch(rgr.getData(), distinct);
			if (leadingMismatch&&!RiboUtils.isLeadingMismatchInsideGenomicRegion(rgr.getData(), distinct))
				this.region = rgr.getReference().getStrand().equals(Strand.Plus)?rgr.getRegion().extendFront(1):rgr.getRegion().extendBack(1);
			else
				this.region = rgr.getRegion();
			
			final int prime = 31;
			hashcode = 1;
			hashcode = prime * hashcode + (leadingMismatch ? 1231 : 1237);
			hashcode = prime * hashcode
					+ ((region == null) ? 0 : region.hashCode());
		}
		
		
		public int hashCode() {
			return hashcode;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Read other = (Read) obj;
			if (leadingMismatch != other.leadingMismatch)
				return false;
			if (region == null) {
				if (other.region != null)
					return false;
			} else if (!region.equals(other.region))
				return false;
			return true;
		}




		@Override
		public String toString() {
			return (leadingMismatch?"L":" ")+region.toRegionString()+":"+count;
		}
		
		@Override
		public int compareTo(Read o) {
			int re = region.compareTo(o.region);
			if (re==0) re = Boolean.compare(leadingMismatch, o.leadingMismatch);
			return re;
		}
	}




	
	
}
