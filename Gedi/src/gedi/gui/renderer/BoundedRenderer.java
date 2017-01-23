package gedi.gui.renderer;

import java.awt.geom.Dimension2D;


public interface BoundedRenderer<T> extends Renderer<T> {

	Dimension2D getSize();
	boolean isFixAspectRatio();
	
}
