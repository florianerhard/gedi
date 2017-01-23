package gedi.riboseq.visu;

import gedi.core.region.GenomicRegion;
import gedi.gui.genovis.style.StyleObject;
import gedi.gui.genovis.tracks.boxrenderer.BoxRenderer;
import gedi.util.ArrayUtils;
import gedi.util.StringUtils;
import gedi.util.dynamic.DynamicObject;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;

public class PeptideRenderer<T> extends BoxRenderer<T> {

	
	protected Color[] frameColors = {Color.WHITE,Color.WHITE,Color.WHITE};
	
	
	public PeptideRenderer() {
		setStringer(f->StringUtils.toString(f));
	}
	
	public void setStyles(DynamicObject styles) {
		if (styles.isArray()){
			HashMap<String, Color> colorMap = ArrayUtils.createMapping(styles.applyTo(new StyleObject[styles.length()]),s->s.getName(),s->s.getColor());
			for (int f=0; f<3;f++)
				frameColors[f] = colorMap.getOrDefault("Frame"+f,Color.white);
		}
		
	}

	
	
//	@Override
//	public double renderBox(Graphics2D g2, PixelBasepairMapper locationMapper,
//			ReferenceSequence reference, Strand strand, GenomicRegion region,
//			T d, double xOffset, double y, double h) {
//		
//		background = x->frameColors[region.getStart()%3];
//		
//		return super.renderBox(g2, locationMapper, reference, strand, region, d, xOffset, y, h);
//	}
	
	@Override
	protected boolean renderTile(Graphics2D g2, Rectangle2D tile, Paint border,
			Paint bg, Paint fg, Font font, String label, GenomicRegion region, int part, T d) {
		bg = frameColors[((region.getStart(part)%3)+3-(region.induce(region.getStart(part))%3))%3];
		return super.renderTile(g2, tile, border, bg, fg, font, label, region, part,d);
	}

}
