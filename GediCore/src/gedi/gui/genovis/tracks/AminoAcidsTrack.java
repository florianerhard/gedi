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
package gedi.gui.genovis.tracks;

import gedi.core.data.mapper.GenomicRegionDataMapping;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.gui.genovis.VisualizationTrackAdapter;
import gedi.gui.genovis.VisualizationTrackPickInfo;
import gedi.gui.genovis.style.StyleObject;
import gedi.util.ArrayUtils;
import gedi.util.PaintUtils;
import gedi.util.SequenceUtils;
import gedi.util.StringUtils;
import gedi.util.dynamic.DynamicObject;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.function.Function;


@GenomicRegionDataMapping(fromType=CharSequence.class)
public class AminoAcidsTrack extends VisualizationTrackAdapter<CharSequence,Character> {


	private Function<String,Color> background = c->Color.gray;
	private Color[] frameColors = {Color.WHITE,Color.WHITE,Color.WHITE};
	private Function<String,Color> foreground = c->Color.black;

	private boolean complement = false;
	
	public void setComplement(boolean complement) {
		this.complement = complement;
	}
	
	public void setHeight(double height) {
		this.minHeight = this.prefHeight = this.maxHeight = height;
		this.viewer.relayout();
	}


	public AminoAcidsTrack() {
		super(CharSequence.class);
		this.minHeight = this.prefHeight = this.maxHeight = 15*3;
		this.minPixelPerBasePair = 1;
	}
	
	public AminoAcidsTrack(boolean complement) {
		super(CharSequence.class);
		this.complement=complement;
		this.minHeight = this.prefHeight = this.maxHeight = 15*3;
		this.minPixelPerBasePair = 1;
	}
	
	@Override
	public void setStyles(DynamicObject styles) {
		super.setStyles(styles);
		
		if (getStyles().isArray()){
			HashMap<String, Color> map = ArrayUtils.createMapping(getStyles().applyTo(new StyleObject[styles.length()]),s->s.getName(),s->s.getColor());
			background = l->map.getOrDefault(l,null);
			for (int f=0; f<3;f++)
				frameColors[f] = map.getOrDefault("Frame"+f,Color.white);
		}
		
	}
	@Override
	protected gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<CharSequence> renderBackground(
			gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<CharSequence> context) {
		context.g2.setPaint(Color.white);
		context.g2.fill(bounds);

		return context;
	}
	
	@Override
	public void pick(VisualizationTrackPickInfo<Character> info) {
		ImmutableReferenceGenomicRegion<CharSequence> data = getData(info.getReference()).toImmutable();
		
		int pos = info.getBp();
		int f = (int) ((info.getPixelY()-getBounds().getY())/getBounds().getHeight()*3);
		pos=((pos-f)/3)*3+f;
				
		pos = data.induceMaybeOutside(pos);
		if (pos>=0 && pos<data.getData().length()) {
			
			
			String dna = data.getData().subSequence(pos, pos+3).toString();
			if (complement) dna = SequenceUtils.getDnaReverseComplement(dna);
			String aa = SequenceUtils.translate(dna);
			
			info.setData(aa.charAt(0));
		}
	}

	@Override
	protected gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<CharSequence> renderLabel(
			gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<CharSequence> context) {
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
		
		double h = tile.getHeight()/3;

		
		if (rr.getNumParts()>0)
		for (int f=0; f<3; f++) {
			
			int ind = 0;
			for (int p=0; p<rr.getNumParts(); p++) {
				int offset = (3-rr.getStart(p)%3+f)%3;
				ind+=offset;
				int i;
				for (i=offset+rr.getStart(p); i<rr.getEnd(p); i+=3, ind+=3) {
					if (ind+3<=seq.length()) {
						String dna = seq.subSequence(ind, ind+3).toString();
						if (complement) dna = StringUtils.reverse(dna).toString();
						String aa = SequenceUtils.translate(dna);
	
						double s = viewer.getLocationMapper().bpToPixel(context.reference,i);
						double e = viewer.getLocationMapper().bpToPixel(context.reference,i+3);
						tile.setRect(bounds.getX()+s, bounds.getY()+h*f, e-s, h);
						PaintUtils.normalize(tile);
						
						Color b = background.apply(aa);
						if (b!=null) 
							context.g2.setPaint(b);
						else
							context.g2.setPaint(frameColors[f]);

						context.g2.fill(tile);
						
						if (viewer.getLocationMapper().getPixelsPerBasePair()>=2) {
							context.g2.setPaint(foreground.apply(aa));
							PaintUtils.paintString(aa, context.g2, tile, 0, 0);
						}
						context.g2.setPaint(Color.black);
						context.g2.draw(tile);
						
					}
				}
				ind-=i-rr.getEnd(p);
			
			}

		}
		return context;
	}



}
