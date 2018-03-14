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
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.gui.genovis.VisualizationTrackAdapter;
import gedi.gui.genovis.VisualizationTrackPickInfo;
import gedi.gui.genovis.style.StyleObject;
import gedi.util.ArrayUtils;
import gedi.util.PaintUtils;
import gedi.util.SequenceUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.EI;
import gedi.util.mutable.MutableDouble;
import gedi.util.mutable.MutableInteger;
import gedi.util.mutable.MutablePair;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.function.Function;


@GenomicRegionDataMapping(fromType={CharSequence.class,CharSequence[].class})
public class MsaTrack extends VisualizationTrackAdapter<MutablePair<CharSequence, CharSequence[]>,Character> {


	private Function<Character,Color> background = SequenceUtils.getNucleotideColorizer();
	private Function<Character,Color> foreground = c->Color.black;

	private boolean complement = false;
	
	public void setComplement(boolean complement) {
		this.complement = complement;
	}
	
	public MsaTrack() {
		super((Class)MutablePair.class);
		this.minHeight = this.prefHeight = this.maxHeight = 15;
		this.minPixelPerBasePair = 1;
		this.leftMarginWidth = 100;

	}
	
	public MsaTrack(boolean complement) {
		super((Class)MutablePair.class);
		this.complement=complement;
		this.minHeight = this.prefHeight = this.maxHeight = 15;
		this.minPixelPerBasePair = 1;
		this.leftMarginWidth = 100;

	}
	
	@Override
	protected gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<MutablePair<CharSequence, CharSequence[]>> renderBackground(
			gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<MutablePair<CharSequence, CharSequence[]>> context) {
		context.g2.setPaint(Color.white);
		context.g2.fill(bounds);

		return context;
	}
	
	@Override
	protected gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<MutablePair<CharSequence, CharSequence[]>> renderLabel(
			gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<MutablePair<CharSequence, CharSequence[]>> context) {
		return context;
	}
	

	@Override
	protected gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<MutablePair<CharSequence, CharSequence[]>> renderMargin(
			gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<MutablePair<CharSequence, CharSequence[]>> context) {
			
		// margin
		Rectangle2D left = getLeftMargin();
		context.g2.setPaint(getBackground());
		context.g2.fill(left);
		
		String[] species = EI.wrap("Consensus").chain(EI.wrap(meta.getEntry("species").asArray()).map(d->d.asString())).toArray(String.class);
		double[] widths = EI.wrap(species).mapToDouble(s->context.g2.getFont().getStringBounds(s, context.g2.getFontRenderContext()).getWidth()).toDoubleArray();
		context.putValue(LEGEND, ArrayUtils.max(widths)+5);

		context.g2.setPaint(Color.black);
		for (int i=0; i<species.length; i++) {
			paintLabel(context.g2, getBounds().getX(), leftMarginWidth, species[i], (i+0.5)*15);
		}
		

		return context;
	}

	@Override
	public TrackRenderContext<MutablePair<CharSequence, CharSequence[]>> renderTrack(TrackRenderContext<MutablePair<CharSequence, CharSequence[]>> context) {
		
		
		Rectangle2D tile = new Rectangle2D.Double(
				0,
				bounds.getY(),
				0,
				15
				);

		CharSequence cons = context.data.get(0);
		CharSequence[] seq = context.data.get(1);
		GenomicRegion rr = context.regionToRender;
		if (!context.isReady()) {
			ArrayGenomicRegion inter = context.regionOfData.intersect(context.regionToRender);
			rr = inter;
			inter = context.regionOfData.induce(inter);
			for (int i=0; i<seq.length; i++)
				seq[i] = SequenceUtils.extractSequenceSave(inter, seq[i].toString(),'-');
		}
		
		double h = 15;

		
		if (rr.getNumParts()>0)
		for (int f=-1; f<seq.length; f++) {
			int ind = 0;
			for (int p=0; p<rr.getNumParts(); p++) {

				for (int i=rr.getStart(p); i<rr.getEnd(p); i++, ind++) {
					
					char c = (f==-1?cons:seq[f]).charAt(ind);
					boolean iscons = c==cons.charAt(ind);
					if (iscons && f>=0) continue;
					
					if (complement)
						c = SequenceUtils.getDnaComplement(c);
					
					double s = viewer.getLocationMapper().bpToPixel(context.reference,i);
					double e = viewer.getLocationMapper().bpToPixel(context.reference,i+1);
					tile.setRect(bounds.getX()+s, bounds.getY()+h*(f+1), e-s, tile.getHeight());
					PaintUtils.normalize(tile);
					context.g2.setPaint(background.apply(c));
					context.g2.fill(tile);
					if (viewer.getLocationMapper().getPixelsPerBasePair()>=5) {
						context.g2.setPaint(foreground.apply(c));
						PaintUtils.paintString(String.valueOf(c), context.g2, tile, 0, 0);
					}
				}

			}

		}
		
		
		double height = Math.max(0, (seq.length+1)*15);
		context.putValue(MAX_HEIGHT, Math.max((Double) context.get(MAX_HEIGHT),height));

		
		return context;
	}
	
	
	protected static final int MAX_HEIGHT = 0;
	protected static final int LEGEND = 1;


	@Override
	protected TrackRenderContext<MutablePair<CharSequence, CharSequence[]>> renderBegin(
			HashMap<ReferenceSequence, MutableReferenceGenomicRegion<Void>> current,
			HashMap<ReferenceSequence, MutableReferenceGenomicRegion<MutablePair<CharSequence, CharSequence[]>>> data,
			TrackRenderContext<MutablePair<CharSequence, CharSequence[]>> context) {
		context.putValue(MAX_HEIGHT, 0.0);
		context.putValue(LEGEND, 0.0);
		return context;
	}
	
	@Override
	public TrackRenderContext<MutablePair<CharSequence, CharSequence[]>> renderEnd(
			TrackRenderContext<MutablePair<CharSequence, CharSequence[]>> context) {
		double height = context.get(MAX_HEIGHT);
		if (height!=bounds.getHeight()) {
			this.minHeight = this.prefHeight = this.maxHeight = height;
			this.viewer.relayout();
		}
		double leg = context.get(LEGEND);
		if (leg!=getLeftMarginWidth()) {
			leftMarginWidth = leg;
			this.viewer.relayout();
		}
		return context;
	}

	@Override
	public void pick(VisualizationTrackPickInfo<Character> info) {
	}

}
