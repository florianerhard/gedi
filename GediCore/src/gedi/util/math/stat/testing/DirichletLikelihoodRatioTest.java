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

import gedi.util.ArrayUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.mutable.MutableDouble;

import java.io.IOException;
import java.util.function.ToDoubleFunction;
import java.util.function.UnaryOperator;

import jdistlib.ChiSquare;

import org.apache.commons.math3.special.Gamma;

import cern.colt.bitvector.BitVector;

public class DirichletLikelihoodRatioTest implements ToDoubleFunction<double[]>, UnaryOperator<NumericArray> {

	private double[] n;
	
	
	// one number, line by line!
	public void load(String path) throws IOException {
		n = StringUtils.parseDouble(new LineOrientedFile(path).readAllLines("#"));
		ArrayUtils.normalize(n);
	}

	
	public void setReference(double[] n) {
		this.n = n.clone();
		ArrayUtils.normalize(this.n);
	}

	public double applyAsDouble(double[] counts) {
		double[] a = n.clone();
		ArrayUtils.mult(a, ArrayUtils.sum(counts));
		
		
		double lh1 = logProbability(a, n);
		double lh0 = logProbability(counts, n);
		
		double lr = 2*lh1-2*lh0;
		
		return lr;
	}
	
	@Override
	public NumericArray apply(NumericArray t) {
		double[] a = n.clone();
		ArrayUtils.mult(a, NumericArrayFunction.Sum.applyAsDouble(t));
		
		
		double lh1 = logProbability(a, n);
		double lh0 = logProbability(t, n);
		
		double lr = 2*lh1-2*lh0;
		
		if (lr>100)
			System.out.println("m<-rbind(m,c("+ArrayUtils.concat(",", t.toNumberArray())+"))");

		
		return NumericArray.wrap(new double[] {lr});
	}

	
	
	public static double logProbability(double[] alpha1, double[] p) {
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
	public static double logProbability(NumericArray alpha1, double[] p) {
		double re = 0;
		double asum = 0;
		double gsum = 0;
		for (int i=0; i<p.length; i++) {
			re+=Math.log(p[i])*(alpha1.getDouble(i));
			asum+=alpha1.getDouble(i)+1;
			gsum+=Gamma.logGamma(alpha1.getDouble(i)+1);
		}
		return re+Gamma.logGamma(asum)-gsum;
	}

	public static int getNumAffected(double[]... samples) {
		assert samples.length>1;
		BitVector nans = new BitVector(samples[0].length);
		for (int i=0; i<samples.length; i++) {
			assert samples[i].length == samples[0].length;
			for (int j=0; j<samples[i].length; j++)
				if (Double.isNaN(samples[i][j]))
					nans.putQuick(j, true);
		}
		nans.not();
		return nans.cardinality();
	}
	
	public static double effectSizeMultinomials(double pseudo, double[]... samples) {
		
		double[][] psamples = new double[samples.length][];
		assert samples.length>1;
		BitVector nans = new BitVector(samples[0].length);
		for (int i=0; i<samples.length; i++) {
			assert samples[i].length == samples[0].length;
			for (int j=0; j<samples[i].length; j++)
				if (Double.isNaN(samples[i][j]))
					nans.putQuick(j, true);
		}
		nans.not();
		
		double sum = 0;
		for (int i=0; i<samples.length; i++) {
			psamples[i] = ArrayUtils.restrict(samples[i],nans);
			
			assert ArrayUtils.min(psamples[i])>=0;
			sum+=ArrayUtils.sum(psamples[i]);
		}
		
		for (int i=0; i<samples.length; i++) {
			ArrayUtils.add(psamples[i], pseudo*ArrayUtils.sum(psamples[i])/sum);
		}
		
		int dim = psamples[0].length;
		int obj = psamples.length;
		if (dim<2) return Double.NaN;
		if (obj!=2) throw new RuntimeException("Can only compute the effect size for a pair of samples!");
		
		double exp = (Math.log(ArrayUtils.sum(psamples[0]))-Math.log(ArrayUtils.sum(psamples[1])))/Math.log(2);
		
		double re = 0;
		for (int i=0; i<dim; i++) {
			re+=Math.abs(exp-(Math.log(psamples[0][i])-Math.log(psamples[1][i]))/Math.log(2));
		}
		
		return re;
		
	}
	public static double effectSizeMultinomials(double[]... samples) {
		return effectSizeMultinomials(1, samples);
	}

	
	/**
	 * Tests the 0 Null hypothesis that all sample vectors come from a multinomial with equal probability vector
	 * 
	 * Removes all components where one of the vectors has NaN!
	 * 
	 * TODO: Test with more than two samples (are the df right?)
	 * 
	 * @param pseudo
	 * @param samples is changed!
	 * 
	 * @return
	 */
	public static double testMultinomials(MutableDouble statistic, double pseudo, double[]... samples) {
		
		double[][] psamples = new double[samples.length][];
		assert samples.length>1;
		
		
		// remove NaNs
		BitVector nans = new BitVector(samples[0].length);
		for (int i=0; i<samples.length; i++) {
			assert samples[i].length == samples[0].length;
			for (int j=0; j<samples[i].length; j++)
				if (Double.isNaN(samples[i][j]))
					nans.putQuick(j, true);
		}
		nans.not();
		for (int i=0; i<samples.length; i++) {
			psamples[i] = ArrayUtils.restrict(samples[i],nans);
			assert ArrayUtils.min(psamples[i])>=0;
		}
		
		// remove all zero components
		nans.clear();
		for (int i=0; i<samples.length; i++) 
			for (int j=0; j<samples[i].length; j++) 
				if (samples[i][j]>0) 
					nans.putQuick(j, true);
		for (int i=0; i<samples.length; i++) {
			psamples[i] = ArrayUtils.restrict(psamples[i],nans);
		}
		
		int dim = psamples[0].length;
		int obj = psamples.length;
		if (dim<2) return Double.NaN;
		
		
		// add pseudocounts proportional to total sum of counts
		double[] n1 = new double[dim];
		for (int i=0; i<obj; i++) 
			ArrayUtils.add(n1, psamples[i]);
		ArrayUtils.normalize(n1);
		
		for (int i=0; i<obj; i++)
			for (int j=0; j<dim; j++)
				psamples[i][j]+=pseudo*n1[j];
		
		
		double[] concat = ArrayUtils.concat(psamples);
		double[] norm = concat.clone();
		ArrayUtils.normalize(norm);
		
		// normalize for L1
		double[] c1 = new double[dim];
		for (int i=0; i<norm.length; i+=dim) {
			for (int j=0; j<dim; j++)
				c1[j]+=norm[i+j];
		}
		ArrayUtils.normalize(c1);
		
		// normalize for L2
		double[] cnorm = new double[concat.length];
		for (int i=0; i<norm.length; i+=dim) {
			double s = ArrayUtils.sum(norm, i, i+dim);
			for (int j=0; j<dim; j++)
				cnorm[i+j]=c1[j]*s;
		}
		
		
		// do the test
		double L1 = logProbability(concat,norm);
		double L2 = logProbability(concat,cnorm);
		
		return ChiSquare.cumulative(2*(L1-L2), (dim-1)*(obj-1), false, false);
	}




	public static double testMultinomials(MutableDouble statistic, double[]... samples) {
		return testMultinomials(statistic,1, samples);
	}

	
	public static double testMultinomials(double pseudo, double[]... samples) {
		return testMultinomials(null, pseudo, samples);
	}
	
	public static double testMultinomials(double[]... samples) {
		return testMultinomials(null, samples);
	}
	
}
