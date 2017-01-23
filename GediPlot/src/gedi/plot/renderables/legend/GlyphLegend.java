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
