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
