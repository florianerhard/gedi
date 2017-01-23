package gedi.gui.renderer;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;


public interface Renderer<T> {

	void render(Graphics2D g2, Rectangle2D world, Rectangle2D screen, AffineTransform worldToScreen, AffineTransform screenToWorld);
	T pick(Point2D world, AffineTransform worldToScreen,
			AffineTransform screenToWorld);
	
}
