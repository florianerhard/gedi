package gedi.util.datastructure.tree.rtree;

import java.awt.geom.Rectangle2D;

public class EuclideanSpatialDistance implements SpatialDistance {

	@Override
	public double distance(Rectangle2D item1, Rectangle2D item2) {
		double xdev = Math.max(0, Math.max(item1.getMinX(), item2.getMinX())-Math.min(item1.getMaxX(), item2.getMaxX()));
		double ydev = Math.max(0, Math.max(item1.getMinY(), item2.getMinY())-Math.min(item1.getMaxY(), item2.getMaxY()));
		return Math.sqrt(xdev*xdev+ydev*ydev);
	}

	private static EuclideanSpatialDistance instance;
	public static EuclideanSpatialDistance instance() {
		if (instance==null) instance = new EuclideanSpatialDistance();
		return instance;
	}
	
}
