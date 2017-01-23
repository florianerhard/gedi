package gedi.gui.genovis.tracks;

import gedi.core.data.mapper.GenomicRegionDataMapping;
import gedi.gui.genovis.pixelMapping.PixelBlockToValuesMap;
import gedi.gui.genovis.pixelMapping.PixelLocationMappingBlock;
import gedi.util.PaintUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.functions.NumericArrayFunction;

import java.awt.geom.Rectangle2D;


@GenomicRegionDataMapping(fromType=PixelBlockToValuesMap.class)
public class PercentageStackedBoxesTrack extends NumericValuesTrack implements BarNumericValuesTrack {

	
	
	
	public PercentageStackedBoxesTrack() {
		this.rowOpMin = this.rowOpMax = NumericArrayFunction.Sum;
		fixedMax = 1;
		fixedMin = 0;
	}
	
	@Override
	protected int renderValue(
			TrackRenderContext<PixelBlockToValuesMap> context,
			PixelLocationMappingBlock prevBlock, NumericArray prev,
			PixelLocationMappingBlock block, NumericArray value, int index, int pass) {
	
		double min = 0;
		double max = 1;
		
		double cstack = 0;
		double lastY = getY(transform(min),min,max);
		
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
		
		double sum = 0;
		for (int row=0; row<value.length(); row++) 
			sum+=Double.isNaN(value.getDouble(row))?0:value.getDouble(row);

		for (int row=0; row<value.length(); row++) {
			
			context.g2.setPaint(PaintUtils.parseColor(getStyles().get("["+row+"].color").asString()));
			cstack+=Double.isNaN(value.getDouble(row))?0:value.getDouble(row);
			
			double y = getY(cstack/sum,min,max);
					
			double w = block.getWidth(viewer.getLocationMapper());
			Rectangle2D box = new Rectangle2D.Double(
					getBounds().getX()+block.getStartPixel(viewer.getLocationMapper())+w/div*offs, 
					y, 
					w/div, 
					Math.abs(y-lastY));
			
			context.g2.fill(box);
			context.g2.draw(box);
			lastY = y;
		}
		
		return value.length();
	}
	
	
	
}
