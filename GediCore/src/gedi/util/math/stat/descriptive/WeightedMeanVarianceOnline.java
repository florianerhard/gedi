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
package gedi.util.math.stat.descriptive;

/**
 * Single-pass constant space computation of mean and variance according to [Knuth.The Art of Computer Programming, volume 2 (1998)]
 * @author erhard
 *
 */
public class WeightedMeanVarianceOnline {

	private double n=0;
	private double mean = 0;
	private double M2 = 0;

	@Override
	public String toString() {
		return String.format("Mean=%.4g, Sd=%.4g, n=%.4g, M2=%.4g", mean,getStandardDeviation(),n,M2);
	}
	
	public boolean add(double x) {
		if (Double.isNaN(x) || Double.isInfinite(x)) 
			return false;
		n++;
		double d = x-mean;
		mean+=d/n;
		M2+=d*(x-mean);
		return true;
	}

	public boolean add(double x, double w) {
		if (Double.isNaN(x) || Double.isInfinite(x)) 
			return false;
		n+=w;
		double d = x-mean;
		mean+=w*d/n;
		M2+=w*d*(x-mean);
		return true;
	}

	public double getMean() {
		return mean;
	}

	public double getVariance() {
		return M2/(n-1);
	}

	public double getStandardDeviation() {
		return Math.sqrt(getVariance());
	}

	public double getCount() {
		return n;
	}
	
	public WeightedMeanVarianceOnline add(WeightedMeanVarianceOnline other) {
		double msum = this.mean*this.n;
		double osum = other.mean*other.n;
		this.n+=other.n;
		this.mean=this.n==0?0:((msum+osum)/this.n);
		this.M2+=other.M2;
		return this;
		
	}


}
