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

package gedi.util.math.stat.testing.multipleTesting;


public class BenjaminiHochbergCorrection extends AbstractMultipleTestingCorrection {

	@Override
	protected void correct(double[] pvals, int length, int additionalInsignificant) {
		double oldPv = Double.NaN;
		int oldRank = -1;
		int total = length+additionalInsignificant;
		for (int i=length-1; i>=0; i--) {
			double p = pvals[i];
			if (p==oldPv)
				pvals[i]=pvals[oldRank];
			else {
				oldRank = i;
				oldPv = p;
				pvals[i] = Math.min(1, p*total/(i+1));
			}
		}
	}
	
}
