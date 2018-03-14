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
package gedi.gui.genovis.tracks.boxrenderer;

import gedi.core.data.annotation.NarrowPeakAnnotation;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.GenomicRegion;
import gedi.util.MathUtils;
import gedi.util.PaintUtils;
import gedi.util.gui.PixelBasepairMapper;
import gedi.util.gui.ValueToColorMapper;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

public class NarrowPeakRenderer extends BoxRenderer<NarrowPeakAnnotation> {

	private ValueToColorMapper mapper = new ValueToColorMapper(Color.WHITE,Color.BLACK);
	
	private Color summitColor=Color.RED;
	
	public NarrowPeakRenderer() {
		setBackground(t->mapper.apply(t.getScore()));
		setForeground(t->PaintUtils.isDarkColor(mapper.apply(t.getScore()))?Color.WHITE:Color.BLACK);
		stringer = s->s.getData().toString();
	}
	
	public void linear(double min, double max) {
		mapper = new ValueToColorMapper(MathUtils.linearRange(min,max), mapper.getColors());
	}
	
	
	public void colors(Color... colors) {
		mapper = new ValueToColorMapper(mapper.getRange(), colors);
	}
	

	@Override
	public GenomicRegion renderBox(Graphics2D g2, PixelBasepairMapper locationMapper,
			ReferenceSequence reference, Strand strand, GenomicRegion region,
			NarrowPeakAnnotation d, double xOffset, double y, double h) {
		GenomicRegion re = super.renderBox(g2, locationMapper, reference, strand, region, d, xOffset, y, h);
		
		if (d.getSummit()>=0 && d.getSummit()<region.getTotalLength()) {
			Rectangle2D tile = getTile(reference, region.map(d.getSummit()), region.map(d.getSummit())+1, locationMapper, xOffset, y, h);
			g2.setPaint(summitColor);
			g2.fill(tile);
			g2.draw(tile);
		}
		
		return re;
	}

}
