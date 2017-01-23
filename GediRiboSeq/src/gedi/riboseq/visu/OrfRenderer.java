package gedi.riboseq.visu;

import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.GenomicRegion;
import gedi.gui.genovis.style.StyleObject;
import gedi.riboseq.inference.orf.Orf;
import gedi.util.ArrayUtils;
import gedi.util.dynamic.DynamicObject;
import gedi.util.gui.PixelBasepairMapper;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.HashMap;

public class OrfRenderer extends PeptideRenderer<Orf> {

	
	
	private Color[] brighterframeColors = {Color.WHITE,Color.WHITE,Color.WHITE};
	private Color[] save = {Color.WHITE,Color.WHITE,Color.WHITE};


	
	public void setStyles(DynamicObject styles) {
		if (styles.isArray()){
			HashMap<String, Color> colorMap = ArrayUtils.createMapping(styles.applyTo(new StyleObject[styles.length()]),s->s.getName(),s->s.getColor());
			for (int f=0; f<3;f++)
				brighterframeColors[f] = colorMap.getOrDefault("Frame"+f,Color.white).brighter();
		}
		super.setStyles(styles);
		save = frameColors;
	}
	
	
	@Override
	public GenomicRegion renderBox(Graphics2D g2,
			PixelBasepairMapper locationMapper, ReferenceSequence reference, Strand strand, GenomicRegion region,
			Orf data, double xOffset, double y, double h) {

		frameColors = brighterframeColors;
		if (data.hasStart() && data.hasStop())
			setStringer(o->"");
		else
			setStringer(o->o.getOrfType().toString());
		
		GenomicRegion re = super.renderBox(g2, locationMapper, reference, strand, region, data, xOffset, y, h);
		
		setStringer(o->o.getOrfType().toString());
		frameColors = save;
		if (data.hasStart() && data.hasStop()) {
			GenomicRegion codingRegion = data.getStartToStop(reference.toStrand(strand), region);
			super.renderBox(g2, locationMapper, reference, strand, codingRegion, data, xOffset, y, h);
		}
		
		return re;
	}

	
}
