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
import gedi.util.mutable.MutableDouble;

import java.awt.geom.Rectangle2D;


@GenomicRegionDataMapping(fromType=PixelBlockToValuesMap.class)
public class StackedBoxesTrack extends NumericValuesTrack implements BarNumericValuesTrack {

	
	
	
	public StackedBoxesTrack() {
		this.rowOpMin = this.rowOpMax = NumericArrayFunction.SaveSum;
	}
	
	@Override
	protected int renderValue(
			TrackRenderContext<PixelBlockToValuesMap> context,
			PixelLocationMappingBlock prevBlock, NumericArray prev,
			PixelLocationMappingBlock block, NumericArray value, int index, int pass) {
	
		double min = context.<MutableDouble>get(MIN_VALUE).N;
		double max = context.<MutableDouble>get(MAX_VALUE).N;
		
		double cstack = 0;
		double lastY = getY(transform(0),min,max);
		
		double div = 1;
		double offs = 0;
		if (group!=null) {
			boolean found = false;
			for (NumericTrack t : group.getSubTracks()) {
				if (t instanceof BarNumericValuesTrack){
					if (t==this)
						found = true;
					else
						div++;
					if (!found) offs++;
				}
			}
		}
		for (int row=0; row<value.length(); row++) {
			if (value.getDouble(row)>0) {
				context.g2.setPaint(PaintUtils.parseColor(getStyles().get("["+row+"].fill").asString()));
				cstack+=Double.isNaN(value.getDouble(row))?0:value.getDouble(row);
				
				double y = getY(transform(cstack),min,max);
						
				double w = block.getWidth(viewer.getLocationMapper());
				Rectangle2D box = new Rectangle2D.Double(
						getBounds().getX()+block.getStartPixel(viewer.getLocationMapper())+w/div*offs, 
						Math.min(lastY, y), 
						w/div, 
						Math.abs(y-lastY));
				context.g2.fill(box);
				context.g2.draw(box);
				lastY = y;
			}
		}
		
		return value.length();
	}
	
	
	
}
