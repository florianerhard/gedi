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

import gedi.util.ArrayUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.ToDoubleFunction;


/**
 * Implements the EM algorithm for expression data equivalence classes as described in Bray et al.,Nature Biotechnology 34,525–527 (2016)
 * 
 * The likelihood function is 
 * \prod_{e \in E} ( \sum_{i \in e} \frac{\pi_i}{l_i} ^ {\alpha_e}
 * 
 * where 
 * each e \in E is an equivalence class (e.g. a set of isoforms consistent with a specific set of RNA-seq reads)
 * \alpha_e is the total expression value for the equivalence class (i.e. the sufficient statistics for this model)
 * l_i is the effective length of the object i \in e
 * 
 * The parameters \pi_i are inferred (the probability of getting a read from object i),i.e. to obtain relative expression values, divide by effective lengths and renormalize! 
 * 
 * @author erhard
 *
 */
public class EquivalenceClassCountEM<O> {

	private O[][] E;
	
	private HashMap<O,Integer> o2Index;
	private int[] o2Order;
	private double N;
	private double[] alpha;
	private double[] l;
	
	public EquivalenceClassCountEM(O[][] E,
			double[] alpha, ToDoubleFunction<O> l) {
		this.E = E;
		
		int ll = 0;
		o2Index = new HashMap<O, Integer>();
		for (O[] e : E) 
			for (O o : e) {
				o2Index.computeIfAbsent(o, x->o2Index.size());
				ll++;
			}
		this.alpha = alpha;
		this.l = new double[o2Index.size()];
		
		this.o2Order = new int[ll];
		int ind = 0;
		for (int ei=0; ei<E.length; ei++) 
			for (O o : E[ei]) 
				o2Order[ind++] = o2Index.get(o);
		
		for (O o : o2Index.keySet()) {
			ind = o2Index.get(o);
			this.l[ind] = l.applyAsDouble(o);
		}
		N = ArrayUtils.sum(alpha);
	}

	
	public double compute(int miniter, int maxiter, BiConsumer<O,Double> proportionSetter) {
		
		double[] w = new double[l.length];
		double[] pi = new double[l.length];
		double[] oldPi = new double[l.length];
		double oldll  = Double.NEGATIVE_INFINITY;
		
		Arrays.fill(pi,1.0/pi.length);
//		System.out.println(o2Index);
		for (int it=0; it<maxiter; it++) {
			
			// e step
			Arrays.fill(w, 0);
			int o2ind = 0;
			for (int ei=0; ei<E.length; ei++) {
				double tot = 0;
				int start = o2ind;
				for (O o : E[ei]) {
					int ind = o2Order[o2ind++];
					tot+=pi[ind]/l[ind];
				}
				o2ind = start;
				for (O o : E[ei]) {
					int ind = o2Order[o2ind++];
					w[ind]+=tot==0?0:pi[ind]/l[ind]/tot*alpha[ei];
				}
			}
//			System.out.println("Iteration "+it);//+" w="+StringUtils.toString(w));
				
			// m step
			double[] tmp = oldPi;
			oldPi = pi;
			pi = tmp;
			
			for (int i=0; i<pi.length; i++)
				pi[i] = w[i]/N;
			ArrayUtils.normalize(pi);
			
			
//			System.out.println("Iteration "+it+" pi="+StringUtils.toString(pi));
//			
//			double ll = 0;
//			for (int ei=0; ei<E.length; ei++) {
//				double sum = 0;
//				for (O o : E[ei]) {
//					int ind = o2Index.get(o);
//					sum+=pi[ind]/l[ind];
//				}
//				ll+=alpha[ei]*Math.log(sum);
//			}
//			System.out.println(it+": "+ll);
			
			if (it>miniter || true) {
				
				
//				double ll = 0;
//				for (int ei=0; ei<w.length; ei++) {
//					double sum = 0;
//					for (O o : E[ei]) {
//						int ind = o2Index.get(o);
//						sum+=pi[ind]/l[ind];
//					}
//					ll+=alpha[ei]*Math.log(sum);
//				}
//				System.out.println(it+": "+ll);
//				if (ll-oldll<Math.log(1.001)) 
//					break;
//				oldll = ll;
				
				// check for convergence
				boolean finished = true;
				for(int i=0; i<pi.length; i++) {
					if (pi[i]*N>0.01) {
						if (Math.abs(pi[i]*N-oldPi[i]*N)>0.01) {
//							System.out.println(i+"/"+pi.length+" "+Math.abs(pi[i]*N-oldPi[i]*N));
							finished = false;
							break;
						}
					}
				}
				if (finished) break;
			}
			
			
		}
		
		for (O o : o2Index.keySet())
			proportionSetter.accept(o,pi[o2Index.get(o)]);
		
		double ll = 0;
		for (int ei=0; ei<E.length; ei++) {
			double sum = 0;
			for (O o : E[ei]) {
				int ind = o2Index.get(o);
				sum+=pi[ind]/l[ind];
			}
			ll+=alpha[ei]*Math.log(sum);
		}
		
		return ll;
	}
	
	
	
}
