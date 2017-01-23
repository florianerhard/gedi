package gedi.gui.genovis.tracks;


import gedi.core.data.mapper.GenomicRegionDataMapping;
import gedi.gui.genovis.VisualizationTrackAdapter;
import gedi.gui.genovis.VisualizationTrackPickInfo;

import java.awt.Paint;


@GenomicRegionDataMapping(fromType=Void.class)
public class SpacerTrack extends VisualizationTrackAdapter<Void,Void> {


	public SpacerTrack() {
		this(null,5);
	}
	
	public SpacerTrack(Paint background, double height) {
		super(Void.class);
		this.minHeight = this.prefHeight = this.maxHeight = height;
		this.minPixelPerBasePair=0;
		this.maxPixelPerBasePair=Double.POSITIVE_INFINITY;
		
		setBackground(background);
	}
	
	@Override
	public void pick(VisualizationTrackPickInfo<Void> info) {
	}

	public void setHeight(double height) {
		this.minHeight = this.prefHeight = this.maxHeight = height;
		if (viewer!=null)
			this.viewer.relayout();
	}
	
	
	@Override
	protected gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<Void> renderLabel(
			gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<Void> context) {
		return context;
	}
	@Override
	protected TrackRenderContext<Void> renderTrack(TrackRenderContext<Void> context) {
		context.g2.setPaint(getBackground());
		context.g2.fill(getBoundsWithMargin());
		return context;
	}





	
}
