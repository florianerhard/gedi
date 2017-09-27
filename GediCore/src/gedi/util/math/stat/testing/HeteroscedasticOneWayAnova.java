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

package gedi.util.math.stat.testing;

import gedi.util.math.stat.RandomNumbers;

import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;



/**
 * PB method as described in K. Krishnamoorthy et al. /A Parametric Bootstrap Approach for ANOVA with Unequal Variances: Fixed and Random Models,  Computational Statistics & Data Analysis 51 (2007) 5731– 5742
 * @author erhard
 *
 */
public class HeteroscedasticOneWayAnova {

	private Mean mean = new Mean();
	private StandardDeviation stddev = new StandardDeviation();
	private RandomNumbers stoch = new RandomNumbers();
	
	
	private int N = 10000;
	
	
	private double pval = 1;
	private double TN0 = 0;
	
	public void clear() {
		pval = 1;
		TN0 = 0;
	}
	
	/**
	 * GroupStart contains the start indices of all groups 
	 * @param values
	 * @param groupStart
	 */
	public void compute(double[] values, int[] groupStart) {
		double[] mu = new double[groupStart.length];
		double[] sigma = new double[groupStart.length];
		int[] n = new int[groupStart.length];
		
		for (int i=0; i<groupStart.length; i++) {
			int s = groupStart[i];
			int e = i<groupStart.length-1?groupStart[i+1]:values.length;
			mu[i] = mean.evaluate(values, s, e-s);
			sigma[i] = stddev.evaluate(values, mu[i], s, e-s);
			n[i] = e-s;
		}
		
		compute(n,mu,sigma);
	}
	
	public void compute(double[]... values) {
		double[] mu = new double[values.length];
		double[] sigma = new double[values.length];
		int[] n = new int[values.length];
		
		for (int i=0; i<values.length; i++) {
			mu[i] = mean.evaluate(values[i]);
			sigma[i] = stddev.evaluate(values[i], mu[i]);
			n[i] = values[i].length;
		}
		
		compute(n,mu,sigma);
	}
	
	
	
	public void compute(int[] n, double[] mu, double[] sigma) {
		TN0 = computeTN(n, mu, sigma);
		int q = 0;
		
		double[] z = new double[n.length];
		double[] chisq = new double[n.length];
		for (int j=0; j<N; j++) {
			
			for (int i=0; i<n.length; i++) {
				z[i] = stoch.getNormal(0,1);
				chisq[i] = stoch.getChisq(n[i]-1);
			}
			double TNB = computeTNB(n, z, chisq, sigma);
			if (TNB>TN0) q++;
		}
		
		pval = q/(double)N;
	}
	
	public double getTN0() {
		return TN0;
	}
	
	public double getPvalue() {
		return pval;
	}
	
	
	private double computeTN(int[] n, double[] mu, double[] sigma) {
		double s1 = 0;
		double s2 = 0;
		double s3 = 0;
		
		for (int i=0; i<n.length; i++) {
			s1+=n[i]/(sigma[i]*sigma[i])*mu[i]*mu[i];
			s2+=n[i]*mu[i]/(sigma[i]*sigma[i]);
			s3+=n[i]/(sigma[i]*sigma[i]);
		}
		
		return s1-(s2*s2)/s3;
	}
	
	private double computeTNB(int[] n, double[] z, double[] chisq, double[] sigma) {
		double s1 = 0;
		double s2 = 0;
		double s3 = 0;
		
		for (int i=0; i<n.length; i++) {
			s1+=z[i]*z[i]*(n[i]-1)/chisq[i];
			s2+=Math.sqrt(n[i])*z[i]*(n[i]-1)/(sigma[i]*chisq[i]);
			s3+=(n[i]-1)*n[i]/(sigma[i]*sigma[i]*chisq[i]);
		}
		
		return s1-(s2*s2)/s3;
	}
}
