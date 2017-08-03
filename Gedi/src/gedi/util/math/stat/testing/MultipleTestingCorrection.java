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

import gedi.util.math.stat.DoubleRanking;

public class MultipleTestingCorrection {

	
	/**
	 * Inplace!!
	 * @param p
	 * @return
	 */
	public static double[] benjaminiHochberg(double[] p) {
		double n = p.length;
		DoubleRanking r = new DoubleRanking(p);
		r.sort(false);
		double min = 1;
		for (int i=0; i<p.length; i++) {
			min = p[i] = Math.min(min, n/(n-i) * p[i]);
		}
		r.restore();
		return p;
	}
	
}
