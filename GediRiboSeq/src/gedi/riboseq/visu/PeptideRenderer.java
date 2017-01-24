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
