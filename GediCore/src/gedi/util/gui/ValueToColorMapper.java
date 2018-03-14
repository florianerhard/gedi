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
package gedi.util.gui;

import gedi.util.math.stat.factor.Factor;

import java.awt.Color;
import java.util.function.DoubleFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;

/**
 * Always maps numbers in the range 0-1 to a linear interpolation of the specified colors;
 * @author erhard
 *
 */
public class ValueToColorMapper implements DoubleFunction<Color> {

	private DoubleUnaryOperator range;
	private Color[] colors;
	
	public ValueToColorMapper(Color... colors) {
		this(d->d,colors);
	}

	public ValueToColorMapper(DoubleUnaryOperator range, Color... colors) {
		this.range = range;
		this.colors = colors;
	}
	
	public Color[] getColors() {
		return colors;
	}
	
	public DoubleUnaryOperator getRange() {
		return range;
	}
	
	public IntFunction<Color> toDiscreteMapper(int count) {
		Color[] re = new Color[count];
		for (int i=0; i<re.length; i++) 
			re[i] = apply((double)i/(re.length-1));
		return f->re[f];
	}
	
	public Function<Factor, Color> toFactorMapper(Factor factor) {
		Color[] re = new Color[factor.getLevels().length];
		for (int i=0; i<re.length; i++) 
			re[i] = apply((double)i/(re.length-1));
		return f->re[f.getIndex()];
	}

	@Override
	public Color apply(double t) {
		float val = (float) range.applyAsDouble(t);
		if (val>=1) return colors[colors.length-1];
		if (val<=0) return colors[0];

		int leftIndex = (int) Math.floor((colors.length-1)*val);
		int rightIndex = (int) Math.ceil((colors.length-1)*val);

		if (leftIndex==rightIndex)
			return colors[leftIndex];

		val = (val-(float)leftIndex/(float)(colors.length-1))*(colors.length-1);

		return new Color(
				colors[leftIndex].getRed()/255f*(1-val)+colors[rightIndex].getRed()/255f*val,
				colors[leftIndex].getGreen()/255f*(1-val)+colors[rightIndex].getGreen()/255f*val,
				colors[leftIndex].getBlue()/255f*(1-val)+colors[rightIndex].getBlue()/255f*val
				);
	}

}
