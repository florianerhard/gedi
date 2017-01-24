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

package gedi.util.algorithm.mss;

import gedi.util.datastructure.array.NumericArray;


public class MaximalScoringSubsequence {

	public ScoreSubsequence getMss(NumericArray a) {
		int n = a.length();
		int max = 0;
		int l = 1;
		int r = 0;
		int rmax = 0;
		int rstart = 0;
		for (int i=0; i<n; i++) {
			double x = a.getDouble(i);
			if (rmax+x>0)
				rmax+=x;
			else {
				rmax = 0;
				rstart = i;
			}
			if (rmax>max) {
				max = rmax;
				l = rstart;
				r = i;
			}
		}
		return new ScoreSubsequence(a, l, r+1);
	}
	
}
