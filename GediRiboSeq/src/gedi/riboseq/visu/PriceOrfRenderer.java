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

package gedi.riboseq.visu;

import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.gui.genovis.style.StyleObject;
import gedi.riboseq.inference.orf.Orf;
import gedi.riboseq.inference.orf.PriceOrf;
import gedi.util.ArrayUtils;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.TriFunction;
import gedi.util.gui.PixelBasepairMapper;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.util.HashMap;

public class PriceOrfRenderer extends PeptideRenderer<PriceOrf> {

	
	
	private Color[] white = {Color.WHITE,Color.WHITE,Color.WHITE};
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
			PriceOrf data, double xOffset, double y, double h) {

		setStringer(o->"");
		
		frameColors = white;
		super.renderBox(g2, locationMapper, reference, strand, region, data, xOffset, y, h);
		
		TriFunction<ReferenceSequence, GenomicRegion, PriceOrf, Paint> oldBorder = border;
		
		frameColors = brighterframeColors;
		for (int a=0; a<data.getNumAlternativeStartCodons(); a++) {
			if (!data.isPredictedStartIndex(a)) {
				GenomicRegion codingRegion = data.getStartStop(new ImmutableReferenceGenomicRegion<>(reference.toStrand(strand),region),a,true).getRegion();
				super.renderBox(g2, locationMapper, reference, strand, codingRegion, data, xOffset, y, h);
			}
		}
		
		frameColors = save;
		
		setStringer(o->o.getTranscript());
		GenomicRegion codingRegion = data.getStartStop(new ImmutableReferenceGenomicRegion<>(reference.toStrand(strand),region),true).getRegion();
		super.renderBox(g2, locationMapper, reference, strand, codingRegion, data, xOffset, y, h);
		
		border = oldBorder;
		
		return region;
	}

	
}
