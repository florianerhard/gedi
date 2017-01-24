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

package gedi.util.math.stat.binning;

import java.util.Arrays;

public class RealBinning extends AbstractBinning {

	
	private double[] boundaries;

	public RealBinning(double... boundaries) {
		super(false);
		this.boundaries = boundaries;
		Arrays.sort(this.boundaries);
	}
	
	@Override
	public int applyAsInt(double value) {
		int index = Arrays.binarySearch(boundaries, value);
		if (index<0) return -index-2;
		return index;
	}

	@Override
	public double getBinMin(int bin) {
		return boundaries[bin];
	}

	@Override
	public double getBinMax(int bin) {
		return boundaries[bin+1];
	}
	
	@Override
	public boolean isInBounds(double value) {
		return value>=boundaries[0] && value<boundaries[boundaries.length-1];
	}
	
	@Override
	public int getBins() {
		return boundaries.length-1;
	}
	
	
}
