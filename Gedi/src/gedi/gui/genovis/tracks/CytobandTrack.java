package gedi.gui.genovis.tracks;

import gedi.core.data.annotation.ScoreNameAnnotation;
import gedi.core.data.mapper.GenomicRegionDataMapping;
import gedi.core.region.GenomicRegion;
import gedi.gui.genovis.tracks.boxrenderer.BoxRenderer;
import gedi.util.PaintUtils;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;

import java.awt.Color;


@GenomicRegionDataMapping(fromType=IntervalTree.class)
public class CytobandTrack extends PackRegionTrack<ScoreNameAnnotation> {


	public CytobandTrack() {
		this.minHeight = this.prefHeight = this.maxHeight = 15;
		this.hspace = 0;
		this.minPixelPerBasePair=0;
		this.maxPixelPerBasePair=1.0/5_000;
		simpleBasePairsPerPixel = 0;
		BoxRenderer<ScoreNameAnnotation> ren = (BoxRenderer<ScoreNameAnnotation>) this.boxRenderer;
		
		ren.stringer = (ref,reg,s)->s.getName();
		ren.background = (ref,reg,s)->s.getScore()>0.5?PaintUtils.parseColor("#CC6600"):PaintUtils.parseColor("#FFFFCC");
		ren.foreground = (ref,reg,s)->s.getScore()>0.5?Color.WHITE:Color.BLACK;
		ren.border = ren.background;
	}
	
	
	@Override
	protected gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<IntervalTree<GenomicRegion, ScoreNameAnnotation>> renderLabel(
			gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<IntervalTree<GenomicRegion, ScoreNameAnnotation>> context) {
		return context;
	}
	
}
