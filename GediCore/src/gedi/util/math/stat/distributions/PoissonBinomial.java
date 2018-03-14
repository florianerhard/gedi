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
package gedi.util.math.stat.distributions;

import java.util.Arrays;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import gedi.util.ArrayUtils;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import jdistlib.Normal;
import jdistlib.Uniform;
import jdistlib.generic.GenericDistribution;
import jdistlib.math.MathFunctions;

/**
 * According to Hong,  Y. (2012).   On computing the distribution function for the Poisson binomial distribution.
Computational Statistics & Data Analysis.
 *
 **/
public class PoissonBinomial extends GenericDistribution {

	private double[] pp;
	private Complex[] z;
	private int m;
	
	private Normal normalApprox;
	
	
	/**
	 * for n>2000
	 * @param useApprox
	 */
	public PoissonBinomial(double[] pp) {
		this.pp = pp.clone();
		Arrays.sort(this.pp);
		double mu = ArrayUtils.sum(pp);
		double sigma = 0;
		for (double p : pp)
			sigma+=p*(1-p);
		sigma = Math.sqrt(sigma);
		normalApprox = new Normal(mu,sigma);
		if (pp.length<=2000)
			preprocess();
	}
	
	public int getProbabilityCount() {
		return pp.length;
	}
	
	public double getExpected() {
		return normalApprox.mu;
	}
	
	public double getProbability(int index) {
		return pp[index];
	}

	private void preprocess() {
		int n = pp.length;
		m=n+1;
		int nextPowerOf2 = Integer.highestOneBit(m);
		if (nextPowerOf2!=m) nextPowerOf2<<=1;
		m = nextPowerOf2;
		n = m-1;
		
		int ins = 0;
		int start = 0;
		for (int i=1; i<pp.length; i++) {
			if (Math.abs(pp[i]-pp[start])>1E-10) {
				if (i-start>1) {
					double p = pp[start];
					pp[ins++] = -(i-start);
					pp[ins++] = p;
				}
				else {
					pp[ins++] = pp[i-1];
				}
				start = i;
			}
		}
		
		if (pp.length-start>1) {
			double p = pp[start];
			pp[ins++] = -(pp.length-start);
			pp[ins++] = p;
		}
		else {
			pp[ins++] = pp[pp.length-1];
		}
		
		
		double delta=2*Math.PI/m;
		z = new Complex[m];
		z[0] = new Complex(1,0);

		for(int i=1;i<=Math.ceil(n/2.0);i++)
		{
			double tt=i*delta;
			
//			for(int j=0;j<pp.length;j++)
//			{
//				double pj=j<opp.length?opp[j]:0;
//				double ax=1-pj+pj*Math.cos(tt);
//				double bx=pj*Math.sin(tt);
//				double tmp1=Math.sqrt(ax*ax+bx*bx);
//				double tmp2=Math.atan2(bx,ax); //atan2(x,y)
//				c1o+=Math.log(tmp1);
//				c2o+=tmp2;
//			}
			
			double c1=0.00;
			double c2=0.00;
			for(int j=0;j<ins;j++)
			{
				double pj=pp[j];
				double f = 1;
				if (pj<0) {
					f=-pj;
					pj = pp[++j];
				}
				
				double ax=1-pj+pj*Math.cos(tt);
				double bx=pj*Math.sin(tt);
				double tmp1=Math.sqrt(ax*ax+bx*bx);
				double tmp2=Math.atan2(bx,ax); //atan2(x,y)
				c1+=Math.log(tmp1)*f;
				c2+=tmp2*f;
			}
			z[i] = new Complex(
					Math.exp(c1)*Math.cos(c2),
					Math.exp(c1)*Math.sin(c2)
					);
			z[z.length-i] = z[i].conjugate();
		}
		FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
		z = fft.transform(z,TransformType.FORWARD);
	}


	@Override
	public double density(double x, boolean log) {
		if (pp.length>2000) return normalApprox.density(x+0.5,log);
		if (MathFunctions.isNonInt(x)) return Double.NaN;
		if (x<0 || x>=z.length) return 0;
		double re = Math.max(0, z[(int) x].getReal()/m);
		if (log) re = Math.log(re);
		return re;
	}

	@Override
	public double cumulative(double p, boolean lower_tail, boolean log_p) {
		if (pp.length>2000) return normalApprox.cumulative(p+0.5,lower_tail,log_p);
		
		if (lower_tail) {
			double re = 0;
			int t = (int) Math.min(p, z.length-1);
			for (int i=0; i<=t; i++)
				re+=z[i].getReal()/m;
			if (log_p) re = Math.log(re);
			return re;
		}
		
		double re = 0;
		int t = (int) Math.max(p, -1);
		for (int i=z.length-1; i>t; i--)
			re+=z[i].getReal()/m;
		if (log_p) re = Math.log(re);
		return re;
	}

	@Override
	public double quantile(double q, boolean lower_tail, boolean log_p) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double random() {
		double u = Uniform.random(0, 1, getRandomEngine());
		return quantile(u);
	}



}