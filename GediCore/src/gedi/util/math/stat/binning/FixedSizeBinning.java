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


public class FixedSizeBinning extends AbstractBinning {

	private final double minValue;
	private final double maxValue;
	private final int bins;
	
	private final double binSize;
	

	public FixedSizeBinning(double minValue, double maxValue, int bins) {
		super(false);
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.bins = bins;
		binSize = (maxValue-minValue)/bins;
	}
	
	public double getBinSize() {
		return binSize;
	}

	@Override
	public int applyAsInt(double value) {
		int re = (int) ((value-minValue)/(binSize));
		re = Math.min(Math.max(re,-1),bins);
		return re;
	}

	@Override
	public double getBinMin(int bin) {
		return minValue+bin*binSize;
	}

	@Override
	public double getBinMax(int bin) {
		return minValue+(bin+1)*binSize;
	}
	
	@Override
	public boolean isInBounds(double value) {
		return value>=minValue && value<maxValue;
	}
	
	@Override
	public int getBins() {
		return bins;
	}
	
	
}
