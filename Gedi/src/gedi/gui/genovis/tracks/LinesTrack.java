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

	public final static double DEFAULT_SINGLE_RADIUS = 2;
	private double singleRad = DEFAULT_SINGLE_RADIUS;

	public final static double DEFAULT_POINT_RADIUS = 2;
	public final static double DEFAULT_LINE_WIDTH = 1;
	private double pointRad = DEFAULT_POINT_RADIUS;

	private boolean points = true;
	private Stroke stroke;
	
	
	public void setPoints(boolean points) {
		this.points = points;
	}
	
	
	@Override
	protected int getPassesCount(
			TrackRenderContext<PixelBlockToValuesMap> context) {
		if (context.data.size()==0) return 0;
		return context.data.getValues(0).length();
	}
	
	
	@Override
	protected void beginPass(TrackRenderContext<PixelBlockToValuesMap> context, int row) {
		singleRad = getStyles().get("["+row+"].singleSize").asDouble(DEFAULT_SINGLE_RADIUS);
		
		pointRad = getStyles().get("["+row+"].pointSize").asDouble(DEFAULT_POINT_RADIUS);
		stroke = new BasicStroke((float)getStyles().get("["+row+"].lineWidth").asDouble(DEFAULT_LINE_WIDTH));
		context.g2.setPaint(PaintUtils.parseColor(getStyles().get("["+row+"].color").asString("#000000")));
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
