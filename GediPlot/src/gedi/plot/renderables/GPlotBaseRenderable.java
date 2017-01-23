package gedi.plot.renderables;

import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;

import gedi.plot.GPlotContext;
import gedi.plot.GPlotRenderable;
import gedi.util.gui.Dimension2DDouble;

public class GPlotBaseRenderable implements GPlotRenderable {

	
	private double w,h;
	private boolean keepAspect;
	
	public GPlotBaseRenderable() {
	}

	public GPlotBaseRenderable(double w, double h, boolean keepAspect) {
		this.w = w;
		this.h = h;
		this.keepAspect = keepAspect;
	}
	
	@Override
	public boolean keepAspectRatio() {
		return keepAspect;
	}

	@Override
	public Dimension2D measureMinSize(GPlotContext context) {
		return new Dimension2DDouble(sizeToPixelHorizontal(w, context),sizeToPixelVertical(h, context));
	}

	@Override
	public void render(GPlotContext context, Rectangle2D area) {
	}
	
}
