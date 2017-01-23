package gedi.util.datastructure.tree.rtree;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class DefaultSpatialObject implements SpatialObject {

	private Rectangle2D bounds;
	
	
	public DefaultSpatialObject(Point2D point) {
		this.bounds = new Rectangle2D.Double(point.getX(), point.getY(),0,0);
	}
	
	public DefaultSpatialObject(Rectangle2D bounds) {
		this.bounds = bounds;
	}


	@Override
	public Rectangle2D getBounds() {
		return bounds;
	}

}
