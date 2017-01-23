package gedi.plot;

import java.awt.geom.Rectangle2D;
import java.util.function.Function;

import gedi.plot.scale.transformer.DoubleInvertibleTransformer;

public class GPlotSubPlot {

	private Rectangle2D area;
	
	private Function<GPlotContext,DoubleInvertibleTransformer> xScaling;
	private Function<GPlotContext,DoubleInvertibleTransformer> yScaling;
	
	
	public GPlotSubPlot(Function<GPlotContext,DoubleInvertibleTransformer> xScaling,
			Function<GPlotContext,DoubleInvertibleTransformer> yScaling) {
		this.xScaling = xScaling;
		this.yScaling = yScaling;
	}
	public DoubleInvertibleTransformer getXScaling(GPlotContext ctx) {
		return xScaling.apply(ctx);
	}
	public DoubleInvertibleTransformer getYScaling(GPlotContext ctx) {
		return yScaling.apply(ctx);
	}
	public Rectangle2D getArea() {
		return area;
	}
	public void setArea(Rectangle2D area) {
		this.area = area;
	}
	
	
}
