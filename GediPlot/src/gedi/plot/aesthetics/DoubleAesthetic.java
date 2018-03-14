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
package gedi.plot.aesthetics;

import gedi.plot.renderables.legend.DoubleLegend;
import gedi.plot.scale.DoubleAestheticScale;
import gedi.plot.scale.DoubleScalingPreprocessed;
import gedi.util.PaintUtils;
import gedi.util.datastructure.dataframe.DataColumn;
import gedi.util.datastructure.dataframe.DoubleDataColumn;
import gedi.util.mutable.MutableInteger;

public class DoubleAesthetic extends Aesthetic<Double, DoubleScalingPreprocessed> {

	/** 
	 * for x and y, both scale and preprocess must be the same for all layers!
	 */
	private DoubleAestheticScale scale;
	private DoubleLegend legend;
	private int tickCount = 5;
	private double[] ticks = null;
	private int tickDigits;
	
	public static final double margin = 0.05;
	
	
	public DoubleAesthetic(DataColumn<?> col, DoubleScalingPreprocessed scalePreprocessed, DoubleAestheticScale scale, DoubleLegend legend) {
		super(col,scalePreprocessed,scale,legend);
		this.scale = scale;
		this.legend = legend;
	}
	
	
	public DoubleAesthetic tickCount(int tickCount) {
		this.tickCount = tickCount;
		this.ticks = null;
		return this;
	}
	
	public double[] getTicks() {
		if (ticks==null) {
			MutableInteger dig = new MutableInteger();
			ticks = PaintUtils.findNiceTicks(scale.pretransform(getPreprocess().getMin()),scale.pretransform(getPreprocess().getMax()), tickCount, dig);
			this.tickDigits = dig.N;
		}
		return ticks;
	}
	
	public int getTickDigits() {
		getTicks();
		return tickDigits;
	}


	public double transformAsDouble(int row) {
		return margin(scale.scaleAsDouble(preprocess, col, row));
	}
	
	public double transform(DataColumn<?> col, int row) {
		return margin(scale.scaleAsDouble(preprocess, col, row));
	}
	public double transform(double v) {
		return margin(scale.scaleAsDouble(preprocess, v));
	}
	public double transform(DoubleScalingPreprocessed preprocess, double v) {
		return margin(scale.scaleAsDouble(preprocess, v));
	}
	
	private double margin(double unit) {
		return margin+unit*(1-2*margin);
	}

	@Override
	public DoubleLegend getLegend() {
		return legend;
	}
	
	
	public void mix(DoubleAesthetic other) {
		this.ticks = null;
		preprocess.mix(other.preprocess);
		scale = other.scale;
	}

	public DoubleAestheticScale getScale() {
		return scale;
	}
	
}
