package gedi.gui.genovis.tracks;

import gedi.core.data.mapper.BinningProvider;
import gedi.core.data.mapper.GenomicRegionDataMapper;
import gedi.core.data.mapper.GenomicRegionDataMapping;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelBlockToValuesMap;
import gedi.gui.genovis.pixelMapping.PixelLocationMappingBlock;
import gedi.util.PaintUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.gui.ValueToColorMapper;
import gedi.util.math.stat.binning.Binning;
import gedi.util.mutable.MutableDouble;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.function.DoubleUnaryOperator;

@GenomicRegionDataMapping(fromType=PixelBlockToValuesMap.class)
public class HeatmapTrack extends NumericValuesTrack {

	protected static final int MAPPER = 3;
	protected static final int MAX_HEIGHT = 4;
	
	private DoubleUnaryOperator function = t->t;
	private Color[] spectrum = {Color.white,Color.blue}; 
	
	private double sizePerBin = 0;
	private boolean topDown = true;
	private Binning inputBinning;

	public HeatmapTrack() {
	}
	
	public void setAutoHeight(double sizePerBin) {
		this.sizePerBin = sizePerBin;
	}
	public void setBottomUp() {
		this.topDown = false;
	}
	public void setTopDown() {
		this.topDown = true;
	}
	
	public void setSpectrum(Color[] spectrum) {
		this.spectrum = spectrum;
	}
	
	
	public void setInput(int index, GenomicRegionDataMapper<?, PixelBlockToValuesMap> input){
		if (index==0 && input instanceof BinningProvider) {
			inputBinning = ((BinningProvider) input).getBinning(); 
		}
	}

	
	@Override
	protected TrackRenderContext<PixelBlockToValuesMap> renderMargin(
			TrackRenderContext<PixelBlockToValuesMap> context) {
		if (inputBinning==null) return context;
		
		// margin
		Rectangle2D left = getLeftMargin();
		context.g2.setPaint(getBackground());
		context.g2.fill(left);
		
		// scale bar and zero line
		context.g2.setPaint(axisColor );
	
		double min = inputBinning.getMin();
		double max = inputBinning.getMax();
		
		double minx = left.getMaxX();
		
		// tics
		double exp = PaintUtils.findNiceNumberGreater((int)Math.ceil((max-min)/(getBounds().getHeight()/minTicDistance))); //Math.pow(10, Math.ceil(Math.log10((max-min)/(height/minTicDistance/2))))/2;
		for (double m = exp*Math.ceil(min/exp); m<=max; m+=exp) {
			double y = getY(m, min,max);
			if (topDown)
				y = getBounds().getMaxY()-y+getBounds().getMinY();
			
			context.g2.draw(new Line2D.Double(getBounds().getX()-ticWidth,y,getBounds().getX(),y));
			double x = paintLabel(context.g2, getBounds().getX()-ticWidth, -ticWidth, m, y).getMinX();
			minx = Math.min(minx, x);
		}
		
		
		
		double lw = (minx-left.getMinX())/3;
		paintLegend(context, new Rectangle2D.Double(left.getMinX()+lw/2,left.getMinY()+lw,lw,left.getHeight()-2*lw));
		
		return context;
	}

	
	private void paintLegend(
			gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<PixelBlockToValuesMap> context,
			Rectangle2D rect) {
		double min = function.applyAsDouble(context.<MutableDouble>get(MIN_VALUE).N);
		double max = function.applyAsDouble(context.<MutableDouble>get(MAX_VALUE).N);

		ValueToColorMapper mapper = context.get(MAPPER);
		if (mapper==null) return;
		
		for (int i=(int) rect.getMinY(); i<rect.getMaxY(); i++) {
			double val = min+(1-(i-rect.getMinY())/rect.getHeight())*(max-min);
			context.g2.setPaint(mapper.apply(val));
			context.g2.draw(new Line2D.Double(rect.getMinX(),i,rect.getMaxX(),i));
		}
		
		context.g2.setPaint(Color.black);
		context.g2.draw(rect);
		
		// tics
		double exp = PaintUtils.findNiceNumberGreater((int)Math.ceil((max-min)/(getBounds().getHeight()/minTicDistance))); //Math.pow(10, Math.ceil(Math.log10((max-min)/(height/minTicDistance/2))))/2;
		for (double m = exp*Math.ceil(min/exp); m<=max; m+=exp) {
			double y = rect.getMaxY()-clamp((m-min)/(max-min))*rect.getHeight();
			
			context.g2.draw(new Line2D.Double(rect.getMaxX(),y,rect.getMaxX()+ticWidth,y));
			paintLabel(context.g2, rect.getMaxX()+2*ticWidth+rect.getWidth(), rect.getWidth(), m, y);
		}

	}

	@Override
	protected void beginPass(
			gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<PixelBlockToValuesMap> context,
			int pass) {
		double min = function.applyAsDouble(context.<MutableDouble>get(MIN_VALUE).N);
		double max = function.applyAsDouble(context.<MutableDouble>get(MAX_VALUE).N);
		
		ValueToColorMapper mapper = new ValueToColorMapper(function.andThen(t->(t-min)/(max-min)), spectrum);
		context.putValue(MAPPER, mapper);
	}

	@Override
	protected int renderValue(
			gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<PixelBlockToValuesMap> context,
			PixelLocationMappingBlock prevBlock, NumericArray prev,
			PixelLocationMappingBlock block, NumericArray value, int index, int pass) {
		
		ValueToColorMapper mapper = context.get(MAPPER);

		double x1 = getBounds().getX()+block.getStartPixel(viewer.getLocationMapper());
		double x2 = getBounds().getX()+block.getStopPixel(viewer.getLocationMapper());
		double w = Math.abs(x2-x1);
		double x = Math.min(x1,x2);
		
		double hh = getBounds().getHeight()/value.length();
		
		for (int i=0; i<value.length(); i++) {
			double y = getY(i,0,value.length()-1);
			if (topDown)
				y = getBounds().getMaxY()-y+getBounds().getMinY()-hh;
			context.g2.setPaint(mapper.apply(value.getDouble(i)));
			Rectangle2D rect = new Rectangle2D.Double(x, y, w, hh);
			context.g2.draw(rect);
			context.g2.fill(rect);
		}
		
		return value.length()*2;
		
	}
	
	@Override
	protected void endPass(
			gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<PixelBlockToValuesMap> context,
			int pass) {
		
		int height = context.data.getValues(0).length();
		context.putValue(MAX_HEIGHT, Math.max((Integer) context.get(MAX_HEIGHT),height));
		
	}
	
	@Override
	protected gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<PixelBlockToValuesMap> renderBegin(
			HashMap<ReferenceSequence, MutableReferenceGenomicRegion<Void>> current,
			HashMap<ReferenceSequence, MutableReferenceGenomicRegion<PixelBlockToValuesMap>> data,
			gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<PixelBlockToValuesMap> context) {
		context.putValue(MAX_HEIGHT, 0);
		
		return super.renderBegin(current, data, context);
	}

	@Override
	public TrackRenderContext<PixelBlockToValuesMap> renderEnd(
			TrackRenderContext<PixelBlockToValuesMap> context) {
		int height = context.get(MAX_HEIGHT);
		if (sizePerBin>0 && height*sizePerBin!=bounds.getHeight()) {
			this.minHeight = this.prefHeight = this.maxHeight = height*sizePerBin;
			this.viewer.relayout();
		}
		context.putValue(MIN_SCALE, 0.0);
		context.putValue(MAX_SCALE, height+0.0);
		
		
		return context;
	}
	
}
