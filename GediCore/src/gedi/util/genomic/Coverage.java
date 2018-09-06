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
package gedi.util.genomic;


import gedi.core.region.GenomicRegion;
import gedi.util.ArrayUtils;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.datastructure.collections.longcollections.LongArrayList;


/**
 * Use {@link CoverageAlgorithm} instead!
 * @author erhard
 *
 */
@Deprecated
public class Coverage {

	
	private long sum = 0;
	private LongArrayList re;
	
	public Coverage() {
		 re = new LongArrayList();
	}
	public Coverage(int lengthEstimate) {
		re = new LongArrayList(lengthEstimate);
	}
	
	
	
	public void add(GenomicRegion reg, int count) {
		for (int p=0; p<reg.getNumParts(); p++) {
			re.increment(reg.getStart(p), count);
			re.decrement(reg.getEnd(p), count);
		}
		sum+=count;
	}
	
	public void clear() {
		re = new LongArrayList();
	}
	
	public void add(int pos, int count) {
		re.increment(pos, count);
		re.decrement(pos+1, count);
		sum+=count;
	}
	
	public long[] getCoverage() {
		long[] re = this.re.toLongArray();
		ArrayUtils.cumSumInPlace(re, 1);
		return re;
	}
	
	public double[] getCoverageAsDouble() {
		double[] re = this.re.toDoubleArray();
		ArrayUtils.cumSumInPlace(re, 1);
		return re;
	}
	
	
	public long getSum() {
		return sum;
	}
	
}
