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

package gedi.plot.renderables.legend;

import gedi.plot.GPlotContext;
import gedi.plot.GPlotRenderable;
import gedi.plot.primitives.Glyph;
import gedi.plot.renderables.GlyphRenderable;
import gedi.util.datastructure.dataframe.DoubleDataColumn;


public class SizeLegend extends AbstractDoubleLegend {

	@Override
	protected int getDiscreteCount(GPlotContext context) {
		return aes.getTicks().length;
	}
	

	@Override
	protected GPlotRenderable getDiscreteRenderable(GPlotContext context, int index) {
		double size = aes.transform(new DoubleDataColumn(null, new double[] {aes.getTicks()[index]}), 0);
		return new GlyphRenderable(Glyph.DOT).factor(size).absoluteSize();
	}
	
	@Override
	protected String getDiscreteLabel(GPlotContext context, int index) {
		return String.format("%."+aes.getTickDigits()+"f", aes.getTicks()[index]);
	}
	

}
