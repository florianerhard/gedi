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

import jdistlib.ChiSquare;


/**
 * Not threadsafe!
 * @author erhard
 *
 */
public class PValueCombiner {

	private double accum = 0;
	private int count;
	
	
	public void add(double pval) {
		accum +=Math.log(pval);
		count++;
	}
	
	public void clear() {
		accum = 0;
		count = 0;
	}
	public double combineFisher() {
		if (Double.isInfinite(accum) && accum<0) return 0;
		return ChiSquare.cumulative(-2*accum, 2*count, false, false);
	}
			
	
	public static double combineFisher(double[] pvals) {
		return combineFisher(pvals,0,pvals.length);
	}
	public static double combineFisher(double[] pvals, int start, int length) {
		double x = 0;
		int nonnan = 0;
		for (int i=start+length-1; i>=start; i--) {
			if (!Double.isNaN(pvals[i])) {
				x+=Math.log(pvals[i]);
				nonnan++;
			}
		}
		if (Double.isInfinite(x) && x<0) return 0;
		return ChiSquare.cumulative(-2*x, 2*nonnan, false, false);
	}
	
}
