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
package gedi.plot.scale;

import gedi.util.PaintUtils;
import gedi.util.datastructure.dataframe.DataColumn;
import gedi.util.mutable.MutableInteger;

public class DoubleScalingPreprocessed {

	private double min;
	private double max;
	
	public DoubleScalingPreprocessed(double min, double max) {
		this.min = min;
		this.max = max;
	}
	
	
	public double getMin() {
		return min;
	}
	
	public double getMax() {
		return max;
	}
	
	public void mix(DoubleScalingPreprocessed other) {
		other.min = this.min = Math.min(min, other.min);
		other.max = this.max = Math.max(max, other.max);
	}

	@Override
	public String toString() {
		return "DoubleScalingPreprocessed [min=" + min + ", max=" + max + "]";
	}
	
	
	
	public static DoubleScalingPreprocessed compute(DataColumn<?> c) {
		double min = Double.NaN;
		double max = Double.NaN;
		for (int r=0; r<c.size(); r++) {
			double v = c.getDoubleValue(r);
			if (!Double.isNaN(v) && !Double.isInfinite(v)) {
				if(Double.isNaN(min)) {
					min = max = v;
				} else {
					min = Math.min(min, v);
					max = Math.max(max, v);
				}
			}
		}
		if (Double.isNaN(min))
			return new DoubleScalingPreprocessed(0, 1);
		return new DoubleScalingPreprocessed(min, max);
	}

	
	
}
