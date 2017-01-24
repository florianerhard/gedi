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

import java.awt.Paint;

import gedi.plot.GPlotContext;
import gedi.plot.GPlotRenderable;
import gedi.plot.primitives.Glyph;
import gedi.plot.renderables.GlyphRenderable;
import gedi.plot.scale.GlyphScalingPreprocessed;
import gedi.plot.scale.PaintScalingPreprocessed;

public class GlyphLegend extends AbstractLegend<Glyph,GlyphScalingPreprocessed> {


	@Override
	protected int getDiscreteCount(GPlotContext context) {
		return aes.getPreprocess().count();
	}
	

	@Override
	protected GPlotRenderable getDiscreteRenderable(GPlotContext context, int index) {
		return new GlyphRenderable(aes.getPreprocess().get(index));
	}
	
	@Override
	protected String getDiscreteLabel(GPlotContext context, int index) {
		return aes.getPreprocess().getLevels()[index].name();
	}
	
}
