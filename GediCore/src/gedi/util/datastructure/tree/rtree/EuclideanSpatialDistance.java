/**
 * 
 *    Copyright 2017 Florian Erhard
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */
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
