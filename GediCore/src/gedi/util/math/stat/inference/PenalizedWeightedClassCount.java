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

package gedi.util.math.stat.inference;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.ToDoubleFunction;

import gedi.core.region.ArrayGenomicRegion;
import gedi.util.ArrayUtils;
import gedi.util.StringUtils;
import gedi.util.functions.EI;
import gedi.util.math.stat.RandomNumbers;
import gedi.util.mutable.MutableDouble;

/**
 * Implements the penalized likelihood approach by Jiang & Salzman, Stat Interface 2015
** Class objects are e.g. isoforms (set I in the paper)
 * Count objects are e.g. read classes (set J in the paper)
 * 	 
 * @author erhard
 *
 * @param <CLS> class of the class objects
 * @param <CNT> class of the count objects
 */
public class PenalizedWeightedClassCount<CNT,CLS> {

	
	private SparseWeightMatrix A;
	private ToDoubleFunction<CNT> n;
	
	private HashMap<CLS,MutableDouble> theta;
	private HashMap<CNT,MutableDouble> b;
	private double lambda;
	private double threshold = 0.01;
	
	public PenalizedWeightedClassCount(Collection<CNT> countObjects, Collection<CLS> classObjects, ToDoubleFunction<CNT> n) {
		this(countObjects, classObjects, n, computeLambda(countObjects,n));
	}
	private static <CNT> double computeLambda(Collection<CNT> countObjects, ToDoubleFunction<CNT> n) {
		double m = 0;
		for (CNT cnt : countObjects)
			m = Math.max(m, n.applyAsDouble(cnt));
		return Math.sqrt(m);
	}
	public PenalizedWeightedClassCount(Collection<CNT> countObjects, Collection<CLS> classObjects, ToDoubleFunction<CNT> n, double lambda) {
		A = new SparseWeightMatrix();
		this.n = n;
		this.theta = new HashMap<>();
		this.b = new HashMap<>();
		this.lambda = lambda;
		
		double uni = 1.0/classObjects.size();
		for (CLS cls : classObjects)
			theta.put(cls, new MutableDouble(uni));
		
		for (CNT cnt : countObjects)
			b.put(cnt, new MutableDouble(0));
	}
	
	public void setRate(CNT countObject, CLS classObject, double a) {
		A.setWeight(classObject, countObject, a);
	}
	
	
	public double compute(int miniter, int maxiter, BiConsumer<CLS,Double> thetaSetter) {
		
		HashMap<CLS,MutableDouble> oldtheta = new HashMap<>();
		for (CLS cls : theta.keySet()) 
			oldtheta.put(cls, new MutableDouble());
		
		double N = 0;
		for (CNT countObject : A.getCountObjects())
			N+=n.applyAsDouble(countObject);
		
		for (int it=0; it<maxiter; it++) {
			
			emStep();
			if (lambda>0) {
				penalizeStep();
				centerB();
			}
			
			if (it>miniter) {
				
				// check for convergence
				boolean finished = true;
				for (CLS classObject : theta.keySet()) {
					if (theta.get(classObject).N*N>threshold) {
						if (Math.abs(theta.get(classObject).N*N-oldtheta.get(classObject).N*N)>threshold) {
							finished = false;
							break;
						}
					}
				}
				if (finished) break;
			}
			
			for (CLS cls : theta.keySet()) 
				oldtheta.get(cls).N = theta.get(cls).N;
		}
		emStep();
		for (CLS cls : theta.keySet()) 
			thetaSetter.accept(cls, theta.get(cls).N);
		
		
		return computeLogLikelihood();
		
	}
	
	public double computeLogLikelihood() {
		double ll = 0;
		double penll = 0;
		for (CNT countObject : A.getCountObjects()) {
			double bb = b.get(countObject).N;
			double eb = Math.exp(bb);
			double sum = 0;
			for (Entry<CLS, double[]> e : A.getClassObjects(countObject))
				sum+=theta.get(e.getKey()).N*e.getValue()[0]*eb;
			ll+=n.applyAsDouble(countObject)*Math.log(sum)-sum;
			penll+=Math.abs(bb);
		}
		return ll-lambda*penll;
	}

	private void emStep() {
		// E
		for (CNT countObject : A.getCountObjects()) {
			double sum = 0;
			double nj = n.applyAsDouble(countObject);
			
			for (Entry<CLS,double[]> e : A.getClassObjects(countObject)) 
				sum += e.getValue()[0]*theta.get(e.getKey()).N;
			for (Entry<CLS,double[]> e : A.getClassObjects(countObject))
				e.getValue()[1] = nj*theta.get(e.getKey()).N*e.getValue()[0]/sum; 
		}
		
		// M
		for (CLS classObject : A.getClassObjects()) {
			double num = 0;
			double den = 0;
			for (Entry<CNT,double[]> e : A.getCountObjects(classObject)) {
				num += e.getValue()[1];
				den += e.getValue()[0]+Math.exp(b.get(e.getKey()).N);
			}
			theta.get(classObject).N = num/den;
		}
	}


	private void penalizeStep() {
		
		for (CNT countObject : A.getCountObjects()) {
			double sum = 0;
			for (Entry<CLS,double[]> e : A.getClassObjects(countObject)) 
				sum+=e.getValue()[0]*theta.get(e.getKey()).N;
			double x = n.applyAsDouble(countObject)-sum;
			double soft = Math.signum(x)*Math.max(Math.abs(x)-lambda,0);
			b.get(countObject).N = Math.log(1+soft/sum);
		}
	}

	private void centerB() {
		double[] bs = new double[b.size()];
		int index = 0;
		for (MutableDouble n : b.values())
			bs[index++] = n.N;
		Arrays.sort(bs);
		double med = bs[bs.length/2];
		for (MutableDouble n : b.values())
			n.N-=med;
	}


	/**
	 * 
	 * @author erhard
	 *
	 * @param <CLS> class of the class objects
	 * @param <CNT> class of the count objects
	 */
	private class SparseWeightMatrix {
		
		private HashMap<CLS,HashMap<CNT,double[]>> clsToCnt = new HashMap<>();
		private HashMap<CNT,HashMap<CLS,double[]>> cntToCls = new HashMap<>();
		private double[] empty = new double[2];
		

		public void setWeight(CLS classObject, CNT countObject, double value) {
			HashMap<CNT, double[]> map = clsToCnt.computeIfAbsent(classObject, x->new HashMap<>());
			double[] val = map.get(countObject);
			if (val==null) {
				map.put(countObject, val = new double[2]);
				cntToCls.computeIfAbsent(countObject, x->new HashMap<>()).put(classObject, val);
			}
			val[0] = value;
		}
		
		public double[] get(CLS classObject, CNT countObject) {
			HashMap<CNT, double[]> map = clsToCnt.get(classObject);
			if (map==null) return empty ;
			double[] val = map.get(countObject);
			return val==null?empty:val;
		}
		
		public double getEstimatedCount(CLS classObject, CNT countObject) {
			HashMap<CNT, double[]> map = clsToCnt.get(classObject);
			if (map==null) return 0;
			double[] val = map.get(countObject);
			return val==null?0:val[1];
		}
		
		public Set<CLS> getClassObjects() {
			return clsToCnt.keySet();
		}
		
		public Set<CNT> getCountObjects() {
			return cntToCls.keySet();
		}
		
		public Set<Entry<CLS, double[]>> getClassObjects(CNT countObject) {
			HashMap<CLS, double[]> map = cntToCls.get(countObject);
			if (map==null) return Collections.emptySet();
			return map.entrySet();
		}

		public Set<Entry<CNT, double[]>> getCountObjects(CLS classObject) {
			HashMap<CNT, double[]> map = clsToCnt.get(classObject);
			if (map==null) return Collections.emptySet();
			return map.entrySet();
		}
	}
	
	
	public static void main(String[] args) {
		
		RandomNumbers rnd = new RandomNumbers();
		System.out.println("Seed: "+rnd.getSeed());
		
		
		// these are possible ORFS in both examples:
		//         ##########################################################################################      
		// #####################
		//                                                                                                        ############
		
		ArrayGenomicRegion[] orfs = new ArrayGenomicRegion[] {
			new ArrayGenomicRegion(47,137),
			new ArrayGenomicRegion(0,90),
//			new ArrayGenomicRegion(17,77),
//			new ArrayGenomicRegion(101,113)
		};

		double[] model = {1,1,1};
		int[] truth = {10000,10000};
		double[] a = create(orfs,truth,ArrayUtils.cumSum(model, 1),rnd);
		System.out.println(StringUtils.toString(a));
		
		PenalizedWeightedClassCount<Integer,ArrayGenomicRegion> algo = new PenalizedWeightedClassCount<>(
				EI.seq(0, a.length).list(), 
				Arrays.asList(orfs), 
				p->a[p],0);
		for (ArrayGenomicRegion r : orfs) {
			for (int p=0; p<r.getTotalLength(); p++)
				algo.setRate(r.map(p), r, model[p%3]*ArrayUtils.sum(a));
		}
		
		algo.compute(1000, 10000, (r,t)->System.out.println(r+" "+t));
		
	}
	private static double[] create(ArrayGenomicRegion[] orfs, int[] truth, double[] model, RandomNumbers rnd) {
		double[] a = new double[EI.wrap(orfs).mapToInt(r->r.getEnd()).max()];
		for (int i=0; i<truth.length; i++) {
			for (int l=0; l<truth[i]; l++) {
				int frame = rnd.getCategorial(model);
				a[orfs[i].map(rnd.getUnif(0, orfs[i].getTotalLength()/3)*3+frame)]++;
			}
		}
		return a;
	}
	
	
}
