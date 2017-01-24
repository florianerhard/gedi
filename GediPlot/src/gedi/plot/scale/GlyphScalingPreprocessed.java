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

import java.awt.Color;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;

import gedi.plot.GPlotContext;
import gedi.plot.GPlotRenderable;
import gedi.plot.primitives.Glyph;
import gedi.plot.primitives.LineType;
import gedi.plot.renderables.GPlotEmptyRenderable;
import gedi.util.ArrayUtils;
import gedi.util.datastructure.dataframe.DataColumn;
import gedi.util.math.stat.factor.Factor;

public class GlyphScalingPreprocessed {

	private Glyph[] glyphs;
	private Factor[] levels;
	
	public GlyphScalingPreprocessed(DataColumn<Object> col, Glyph[] allGlyphs) {
		if (col.size()==0) this.glyphs = new Glyph[0];
		else {
			levels = col.getFactorValue(0).getLevels();
			glyphs = allGlyphs;
		}
	}

	public Factor[] getLevels(){
		return levels;
	}
	
	public int count() {
		return levels.length;
	}
	
	
	public Glyph get(int index) {
		return glyphs[index%glyphs.length];
	}


}
