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
