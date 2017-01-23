package gedi.gui.genovis.tracks.boxrenderer;

import gedi.core.data.annotation.Transcript;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.GenomicRegion;
import gedi.util.PaintUtils;
import gedi.util.gui.PixelBasepairMapper;

import java.awt.Color;
import java.awt.Graphics2D;

public class TranscriptRenderer extends BoxRenderer<Transcript> {

	
	private BoxRenderer<Transcript> coding = new BoxRenderer<Transcript>();
	
	public TranscriptRenderer() {
		coding.setBorder();
		coding.setBackground(t->PaintUtils.DARK_RED);
		coding.setForeground(t->Color.WHITE);
		coding.setHeight(20);
		coding.setFont("Arial", 14, true, false);
		coding.setStringer(t->t.getTranscriptId());
		setHeight(20);
		setFont("Arial", 14, true, false);
		setForeground(t->Color.WHITE);
		setBackground(t->PaintUtils.VERY_LIGHT_RED);
		setStringer();
	}
	

	@Override
	public GenomicRegion  renderBox(Graphics2D g2,
			PixelBasepairMapper locationMapper, ReferenceSequence reference, Strand strand, GenomicRegion region,
			Transcript data, double xOffset, double y, double h) {
		setStringer(data.isCoding()?null:coding.getStringer());
			
		GenomicRegion re = super.renderBox(g2, locationMapper, reference, strand, region, data, xOffset, y, h);
		if (data.isCoding()) {
			GenomicRegion codingRegion = data.getCds(reference, region);
			coding.renderBox(g2, locationMapper, reference, strand, codingRegion, data, xOffset, y, h);
		}
		
		return re;
	}


}
