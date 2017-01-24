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

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.UnivariateSolver;
import org.apache.commons.math3.analysis.solvers.UnivariateSolverUtils;
import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.fitting.MultivariateNormalMixtureExpectationMaximization;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;
import org.apache.commons.math3.special.Gamma;
import org.apache.commons.math3.util.MathUtils;

import static java.lang.Math.*;

public class GeneralizedExtremeValueDistribution extends AbstractRealDistribution {

	private static final double EULER = 0.5772156649015328606065120900824024310421;
	private double location;
	private double scale;
	private double shape;
	
	public GeneralizedExtremeValueDistribution(RandomGenerator rnd, double location, double scale,
			double shape) {
		super(rnd);
		this.location = location;
		this.scale = scale;
		this.shape = shape;
	}
	
	public GeneralizedExtremeValueDistribution(double location, double scale,
			double shape) {
		super(new Well19937c());
		this.location = location;
		this.scale = scale;
		this.shape = shape;
	}
	
	public double getLocation() {
		return location;
	}

	public void setLocation(double location) {
		this.location = location;
	}

	public double getScale() {
		return scale;
	}

	public void setScale(double scale) {
		this.scale = scale;
	}

	public double getShape() {
		return shape;
	}

	public void setShape(double shape) {
		this.shape = shape;
	}

	public boolean isSupported(double x) {
		if (shape==0)
			return true;
		if (shape>0)
			return x>location-scale/shape;
		return x<location-scale/shape;
	}

	
	@Override
	public double inverseCumulativeProbability(double p) {
		if (shape!=0) 
			return location-scale*(1-pow(-log(p),-shape))/shape;
		else
			return location-scale*log(-log(p));
	}

	@Override
	public double cumulativeProbability(double x) {
		return exp(-t(x));
	}
	
	@Override
	public double density(double x) {
		double t = t(x);
		return 1/scale*pow(t,shape+1)*exp(-t);
	}
	
	private double t(double x) {
		if (shape!=0)
			return pow(1+shape*(x-location)/scale,-1/shape);
		else
			return exp(-(x-location)/shape);
	}

	@Override
	public String toString() {
		return "GEV("+location+","+scale+","+shape+")";
	}
	
	/**
	 * Fits this distribution to the given sample data using the method described in 
	 * 
	 *     
     * Estimation of the Generalized Extreme-Value Distribution by the Method of Probability-Weighted Momen... more
     * J. R. M. Hosking, J. R. Wallis and E. F. Wood
     * Technometrics, Vol. 27, No. 3 (Aug., 1985), pp. 251-261
     * Published by: American Statistical Association and American Society for Quality
	 *
	 * @param data will be sorted afterwards
	 * @throws FunctionEvaluationException 
	 * @throws ConvergenceException 
	 */
	public static void fitDataByMoments(double[] data) {
		fitDataByMoments(data,0,data.length);
	}
	
	public static GeneralizedExtremeValueDistribution fitDataByMoments(double[] data, int start, int end) {
		int n = end-start;
		if (n<2)
			throw new RuntimeException("Too few data!");
		Arrays.sort(data,start,end);
		
		// compute moments
		final double[] b = new double[3];
		for (int j=1; j<=n; j++) {
			int index = j-1-start;
			b[0]+=data[index];
			b[1]+=data[index]*((j-1.0)/(n-1.0));
			b[2]+=data[index]*((j-1.0)/(n-1.0))*((j-2.0)/(n-2.0));
		}
		b[0]/=n;b[1]/=n;b[2]/=n;
		
		
		

		// solve parameters
		UnivariateFunction f = new UnivariateFunction() {
			public double value(double k)  {
				return (3*b[2]-b[0])/(2*b[1]-b[0])-(1-Math.pow(3, -k))/(1-Math.pow(2, -k));
			}
		};
		double k;
		
		if (Math.signum(f.value(-0.5))!=Math.signum(f.value(0.5)))
			k = UnivariateSolverUtils.solve(f, -0.5, 0.5);
		else {
			double c = (2*b[1]-b[0])/(3*b[2]-b[0])-Math.log(2)/Math.log(3);
			k = 7.859*c+2.9554*c*c;
		}
		
		double g = Gamma.gamma(1+k);
		double alpha = ((2*b[1]-b[0])*k)/(g*(1-Math.pow(2, -k)));
		double xi = b[0]+alpha*(g-1)/k;
		
		double location = xi;
		double scale = alpha;
		double shape = -k;
		
		return new GeneralizedExtremeValueDistribution(location, scale, shape);
	}

	@Override
	public double getNumericalMean() {
		if (shape==0) return location+scale*EULER;
		if (shape>=1) return Double.POSITIVE_INFINITY;
		return location+scale*(Gamma.gamma(1-shape)-1)/shape;
	}

	@Override
	public double getNumericalVariance() {
		if (shape==0) return scale*scale*Math.PI*Math.PI/6;
		if (shape>=0.5) return Double.POSITIVE_INFINITY;
		double g1 = Gamma.gamma(1-shape);
		double g2 = Gamma.gamma(1-2*shape);
		return scale*scale*(g2-g1*g1)/shape/shape;
	}

	@Override
	public double getSupportLowerBound() {
		if (shape>0) return location-scale/shape;
		return Double.NEGATIVE_INFINITY;
	}

	@Override
	public double getSupportUpperBound() {
		if (shape<0) return location-scale/shape;
		return Double.POSITIVE_INFINITY;
	}

	@Override
	public boolean isSupportLowerBoundInclusive() {
		return shape>0;
	}

	@Override
	public boolean isSupportUpperBoundInclusive() {
		return shape<0;
	}

	@Override
	public boolean isSupportConnected() {
		return true;
	}

	

}
