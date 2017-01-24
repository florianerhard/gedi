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

package gedi.gui.genovis.tracks;

import gedi.core.data.mapper.GenomicRegionDataMapping;
import gedi.gui.genovis.pixelMapping.PixelBlockToValuesMap;
import gedi.gui.genovis.pixelMapping.PixelLocationMappingBlock;
import gedi.util.PaintUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.dynamic.DynamicObject;
import gedi.util.mutable.MutableDouble;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;


@GenomicRegionDataMapping(fromType=PixelBlockToValuesMap.class)
public class StackedLinesTrack extends NumericValuesTrack {

	public final static double DEFAULT_SINGLE_RADIUS = 3;
	private double singleRad = DEFAULT_SINGLE_RADIUS;

	
	public final static double DEFAULT_POINT_RADIUS = 2;
	private double pointRad = DEFAULT_POINT_RADIUS;

	private boolean points = true;
	
	public void setPoints(boolean points) {
		this.points = points;
	}
	
	public StackedLinesTrack() {
		this.rowOpMin = this.rowOpMax = NumericArrayFunction.Sum;
	}
	
	@Override
	protected int renderValue(
			TrackRenderContext<PixelBlockToValuesMap> context,
			PixelLocationMappingBlock prevBlock, NumericArray prev,
			PixelLocationMappingBlock block, NumericArray value, int index, int pass) {
		
		
		if (isSinglet(context.data, index, pass)) {
			double x = getBounds().getX()+block.getCenterPixel(viewer.getLocationMapper());
			double min = context.<MutableDouble>get(MIN_VALUE).N;
			double max = context.<MutableDouble>get(MAX_VALUE).N;
			
			double cstack = 0;
			for (int row=0; row<value.length(); row++) {
				
				DynamicObject singleSize = getStyles().get("["+row+"].singleSize");
				singleRad = singleSize.isNull()?DEFAULT_SINGLE_RADIUS:singleSize.asDouble();
				context.g2.setPaint(PaintUtils.parseColor(getStyles().get("["+row+"].color").asString()));
				cstack+=value.getDouble(row);
				
				double y = getY(transform(cstack),min,max);
						
				Ellipse2D ell = new Ellipse2D.Double(x-singleRad , y-singleRad, singleRad*2, singleRad*2);
				context.g2.draw(ell);
				context.g2.fill(ell);
			}
			
			
			return 1;
		}
		
		if (prev==null) return 0;
		
		
		double oldx = getBounds().getX()+prevBlock.getCenterPixel(viewer.getLocationMapper());
		double x = getBounds().getX()+block.getCenterPixel(viewer.getLocationMapper());
		
		double min = context.<MutableDouble>get(MIN_VALUE).N;
		double max = context.<MutableDouble>get(MAX_VALUE).N;
		
		double lstack = 0;
		double cstack = 0;

		for (int row=0; row<value.length(); row++) {
			
			
			
			context.g2.setPaint(PaintUtils.parseColor(getStyles().get("["+row+"].color").asString()));
			lstack+=prev.getDouble(row);
			cstack+=value.getDouble(row);
			
			double lasty = getY(transform(lstack),min,max);
			double y = getY(transform(cstack),min,max);
				
			
			DynamicObject pointSize = getStyles().get("["+row+"].singleSize");
			pointRad = pointSize.isNull()?DEFAULT_SINGLE_RADIUS:pointSize.asDouble();if (points && pointRad>0) {
				context.g2.setPaint(PaintUtils.parseColor(getStyles().get("["+row+"].color").asString()));
				cstack+=value.getDouble(row);
				
						
				Ellipse2D ell = new Ellipse2D.Double(x-pointRad , y-pointRad, pointRad*2, pointRad*2);
				context.g2.draw(ell);
				context.g2.fill(ell);
			}
			
			context.g2.draw(new Line2D.Double(oldx, lasty, x, y));	
		}
		
		return value.length();
	}
	
	
}
