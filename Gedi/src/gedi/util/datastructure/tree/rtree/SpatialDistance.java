package gedi.util.datastructure.tree.rtree;

import java.awt.geom.Rectangle2D;

public interface SpatialDistance {
	double distance(Rectangle2D item1, Rectangle2D item2);
}
