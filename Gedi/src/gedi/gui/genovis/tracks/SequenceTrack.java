package gedi.gui.genovis.tracks;

import gedi.core.data.mapper.GenomicRegionDataMapping;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.gui.genovis.VisualizationTrackAdapter;
import gedi.gui.genovis.VisualizationTrackPickInfo;
import gedi.util.PaintUtils;
import gedi.util.SequenceUtils;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.function.Function;


@GenomicRegionDataMapping(fromType=CharSequence.class)
public class SequenceTrack extends VisualizationTrackAdapter<CharSequence,Character> {


	private Function<Character,Color> background = SequenceUtils.getNucleotideColorizer();
	private Function<Character,Color> foreground = c->Color.black;

	private boolean complement = false;
	
	public void setComplement(boolean complement) {
		this.complement = complement;
	}
	
	public void setHeight(double height) {
		this.minHeight = this.prefHeight = this.maxHeight = height;
		this.viewer.relayout();
	}

	public void setNucleotideColors() {
		background = SequenceUtils.getNucleotideColorizer();
		foreground = c->Color.black;
		this.viewer.repaint();
	}

	@Override
	protected gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<CharSequence> renderLabel(
			gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<CharSequence> context) {
		return context;
	}
	

	public void pick(VisualizationTrackPickInfo<Character> info) {
		MutableReferenceGenomicRegion<CharSequence> d = getData(info.getReference());
		if (d==null) return;
		
		ImmutableReferenceGenomicRegion<CharSequence> data = d.toImmutable();
		if (data.getData()==null) return;
		
		int pos = data.induceMaybeOutside(info.getBp());
		if (pos>=0 && pos<data.getData().length()) {
			char c = data.getData().charAt(pos);
			if (complement)
				c = SequenceUtils.getDnaComplement(c);
			info.setData(c);
		}
	}
	
	public SequenceTrack() {
		super(CharSequence.class);
		this.minHeight = this.prefHeight = this.maxHeight = 15;
		this.minPixelPerBasePair = 1;
	}
	
	public SequenceTrack(boolean complement) {
		super(CharSequence.class);
		this.complement=complement;
		this.minHeight = this.prefHeight = this.maxHeight = 15;
		this.minPixelPerBasePair = 1;
	}
	
	@Override
	protected gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<CharSequence> renderBackground(
			gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<CharSequence> context) {
		context.g2.setPaint(Color.white);
		context.g2.fill(bounds);

		return context;
	}

	@Override
	public TrackRenderContext<CharSequence> renderTrack(TrackRenderContext<CharSequence> context) {
		
		
		Rectangle2D tile = new Rectangle2D.Double(
				0,
				bounds.getY(),
				0,
				bounds.getHeight()
				);

		CharSequence seq = context.data;
		GenomicRegion rr = context.regionToRender;
		if (!context.isReady()) {
			ArrayGenomicRegion inter = context.regionOfData.intersect(context.regionToRender);
			rr = inter;
			inter = context.regionOfData.induce(inter);
			seq = SequenceUtils.extractSequence(inter, seq);
		}
		
		if (complement)
			seq = SequenceUtils.getDnaComplement(seq);

		int ind = 0;
		for (int p=0; p<rr.getNumParts(); p++) {

			for (int i=rr.getStart(p); i<rr.getEnd(p); i++, ind++) {
				double s = viewer.getLocationMapper().bpToPixel(context.reference,i);
				double e = viewer.getLocationMapper().bpToPixel(context.reference,i+1);
				tile.setRect(bounds.getX()+s, tile.getY(), e-s, tile.getHeight());
				PaintUtils.normalize(tile);
				context.g2.setPaint(background.apply(seq.charAt(ind)));
				context.g2.fill(tile);
				if (viewer.getLocationMapper().getPixelsPerBasePair()>=5) {
					context.g2.setPaint(foreground.apply(seq.charAt(ind)));
					PaintUtils.paintString(seq.subSequence(ind,ind+1).toString(), context.g2, tile, 0, 0);
				}
			}

		}
		return context;
	}



}
