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
import gedi.util.mutable.MutableDouble;

import java.awt.BasicStroke;
import java.awt.Stroke;


@GenomicRegionDataMapping(fromType=PixelBlockToValuesMap.class)
public class LinesTrack extends NumericValuesTrack {

	
	
	@Override
	protected int getPassesCount(
			TrackRenderContext<PixelBlockToValuesMap> context) {
		if (context.data.size()==0) return 0;
		return context.data.getValues(0).length();
	}
	
	
	@Override
	protected void beginPass(TrackRenderContext<PixelBlockToValuesMap> context, int row) {
		super.beginPass(context, row);
	}
	
	
	
	@Override
	protected int renderValue(
			TrackRenderContext<PixelBlockToValuesMap> context,
			PixelLocationMappingBlock prevBlock, NumericArray prev,
			PixelLocationMappingBlock block, NumericArray value, int index, int pass) {
		
		
		
		double min = context.<MutableDouble>get(MIN_VALUE).N;
		double max = context.<MutableDouble>get(MAX_VALUE).N;
		
		double x = getBounds().getX()+block.getCenterPixel(viewer.getLocationMapper());
		double y = getY(transform(value.getDouble(pass)),min,max);
		
		
		if (isSinglet(context.data, index, pass)) {
			point(context.g2,x-singleRad , y-singleRad, singleRad*2, singleRad*2);
			return 1;
		}
		
		if (pointRad>0 && points) {
			point(context.g2,x-pointRad , y-pointRad, pointRad*2, pointRad*2);
		}
		
		if (prevBlock==null || prev.isNA(pass) || value.isNA(pass))
			return 0;
		
		double lasty = getY(transform(prev.getDouble(pass)),min,max);
		double oldx = getBounds().getX()+prevBlock.getCenterPixel(viewer.getLocationMapper());
		
		Stroke ostroke = context.g2.getStroke();
		context.g2.setStroke(stroke);
		line(context.g2,oldx, lasty, x, y);
		context.g2.setStroke(ostroke);
		return 1;
	}


	
	
}
