package gedi.util.datastructure.tree.rtree;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

public class STRInnerNode implements SpatialObject {

	private ArrayList<SpatialObject> children = new ArrayList<>();
	private Rectangle2D bounds = null;

	
	public Rectangle2D getBounds() {
		if (bounds==null) 
			computeBounds();
		return bounds;
	}

	private void computeBounds() {
		if (children.size()==0) {
			bounds = new Rectangle2D.Double();
		}
			
		Rectangle2D ch = children.get(0).getBounds();
		double minx = ch.getMinX();
		double maxx = ch.getMaxX();
		double miny = ch.getMinY();
		double maxy = ch.getMaxY();
		for (int i=1; i<children.size(); i++) {
			ch = children.get(i).getBounds();
			minx = Math.min(minx,ch.getMinX());
			maxx = Math.min(maxx,ch.getMaxX());
			miny = Math.min(miny,ch.getMinY());
			maxy = Math.min(maxy,ch.getMaxY());
		}
		bounds = new Rectangle2D.Double(minx, miny, maxx-minx, maxy-miny);
	}


	public ArrayList<SpatialObject> getChildren() {
		return children;
	}


}
