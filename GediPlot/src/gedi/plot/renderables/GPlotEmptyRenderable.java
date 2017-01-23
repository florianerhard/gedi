package gedi.plot.renderables;

import java.awt.Paint;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;

import gedi.plot.GPlotContext;
import gedi.plot.GPlotRenderable;
import gedi.util.gui.Dimension2DDouble;

public class GPlotEmptyRenderable extends GPlotBaseRenderable {

	
	private Paint color;

	public GPlotEmptyRenderable() {
	}
	
	public GPlotEmptyRenderable(double w, double h, Paint color, boolean keepAspect) {
		super(w,h,keepAspect);
		this.color = color;
	}
	
	@Override
	public void render(GPlotContext context, Rectangle2D area) {
		if (color==null) return;
		context.g2.setPaint(color);
		context.g2.fill(area);
	}
	
}
