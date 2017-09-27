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
public class MeanVarianceOnline {

	private int n=0;
	private double mean = 0;
	private double M2 = 0;

	public boolean add(double x) {
		if (Double.isNaN(x) || Double.isInfinite(x)) 
			return false;
		n++;
		double d = x-mean;
		mean+=d/n;
		M2+=d*(x-mean);
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

	public int getCount() {
		return n;
	}


}
