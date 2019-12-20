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
		coding.setBackground(t->PaintUtils.parseColor("#5b95ad"));
		coding.setForeground(t->Color.WHITE);
		coding.setHeight(20);
		coding.setFont("Arial", 14, true, false);
		coding.stringer = t->t.getData().getTranscriptId();
		setHeight(20);
		setFont("Arial", 14, true, false);
		setForeground(t->Color.WHITE);
		setBackground(t->PaintUtils.parseColor("#98c1d2"));
		stringer = d->"";
	}
	

	@Override
	public GenomicRegion  renderBox(Graphics2D g2,
			PixelBasepairMapper locationMapper, ReferenceSequence reference, Strand strand, GenomicRegion region,
			Transcript data, double xOffset, double y, double h) {
		stringer = data.isCoding()?null:coding.stringer;
			
		GenomicRegion re = super.renderBox(g2, locationMapper, reference, strand, region, data, xOffset, y, h);
		if (data.isCoding()) {
			GenomicRegion codingRegion = data.getCds(reference, region);
			coding.renderBox(g2, locationMapper, reference, strand, codingRegion, data, xOffset, y, h);
		}
		
		return re;
	}


}
