package gedi.plot;

import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;

public interface GPlotRenderable {

	Dimension2D measureMinSize(GPlotContext context);
	void render(GPlotContext context, Rectangle2D area);
	
	default boolean keepAspectRatio() {
		return false;
	}
	
	
	
	default void addMargin(Rectangle2D sub, double[] mar) {
		sub.setRect(sub.getX()+mar[1], sub.getY()+mar[2], sub.getWidth()-mar[1]-mar[3], sub.getHeight()-mar[0]-mar[2]);		
	}

	default void correctAspect(Rectangle2D sub, Dimension2D min) {
		double asp = min.getWidth()/min.getHeight();
		double obs = sub.getWidth()/sub.getHeight();
		if (obs>asp) { // too broad
			double w = sub.getHeight()*asp;
			double hoff = (sub.getWidth()-w)/2;
			sub.setRect(sub.getX()+hoff, sub.getY(), w, sub.getHeight());
		} else if (obs<asp) { // too high
			double h = sub.getWidth()/asp;
			double woff = (sub.getHeight()-h)/2;
			sub.setRect(sub.getX(), sub.getY()+woff, sub.getWidth(), h);
		}
	}

	/**
	 * Sizes are generally calculated as percentages (imagine a plot that is 10cm x 10cm; then the size corresponds to mm!)
	 * @param s
	 * @param context
	 * @return
	 */
	default double sizeToPixel(double s, GPlotContext context) {
		Rectangle2D full = context.fullArea;
		return Math.min(full.getHeight()*s/100, full.getWidth()*s/100);
	}

	default double sizeToPixelHorizontal(double s, GPlotContext context) {
		Rectangle2D full = context.fullArea;
		return full.getWidth()*s/100;
	}
	
	default double sizeToPixelVertical(double s, GPlotContext context) {
		Rectangle2D full = context.fullArea;
		return full.getHeight()*s/100;
	}

	
	default double scaleX(double x, Rectangle2D area) {
		return x*area.getWidth()+area.getX();
	}
	
	default double scaleY(double y, Rectangle2D area) {
		return area.getMaxY()-y*area.getHeight();
	}
	
	
}
