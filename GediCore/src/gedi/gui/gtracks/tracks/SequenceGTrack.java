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
package gedi.gui.gtracks.tracks;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;


import gedi.core.data.mapper.GenomicRegionDataMapping;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.gui.gtracks.GTrack;
import gedi.gui.gtracks.GTrack.HeightType;
import gedi.gui.gtracks.picking.CharSequencePickObject;
import gedi.gui.gtracks.rendering.GTracksRenderRequest;
import gedi.gui.gtracks.rendering.GTracksRenderer;
import gedi.gui.gtracks.style.GTrackStyleParser;
import gedi.gui.gtracks.style.GTrackStyles;
import gedi.util.PaintUtils;
import gedi.util.SequenceUtils;
import gedi.util.dynamic.DynamicObject;

@GenomicRegionDataMapping(fromType=CharSequence.class,toType=GTracksRenderer.class)
public class SequenceGTrack implements GTrack<CharSequence, CharSequencePickObject>{

	private boolean complement = false;
	
	private GTrackStyles style; // set by reflection in GTrack.setStyles
	
	private static final Color defaultColor = Color.black;
	private static final Color defaultBackground = Color.GRAY;
	private static final double defaultHeight = 15;
	private static final double defaultMargin = 2;
	
	
	
	@Override
	public HeightType geHeightType() {
		return HeightType.Fixed;
	}

	
	@Override
	public boolean isDisabled(ReferenceSequence ref, GenomicRegion reg, PixelLocationMapping pixelMapping) {
		return pixelMapping.getXmapper().getPixelsPerBasePair()<1;
	}

	@Override
	public GTracksRenderer map(ReferenceSequence reference, GenomicRegion region, PixelLocationMapping pixelMapping,
			CharSequence seq) {
		return (target,context)->{
			
			GenomicRegion rr = context.getRegionToRender(reference);
			if (rr==null) 
				return null;
			
			CharSequence ss = seq;
			
			if (!region.equals(rr)) {
				ArrayGenomicRegion inter = region.intersect(rr);
				rr = inter;
				inter = region.induce(inter);
				ss = SequenceUtils.extractSequence(inter, ss);
			}
			
			if (complement)
				ss = SequenceUtils.getDnaComplement(ss);

			
			double maxHeight = 0;
			int ind = 0;
			for (int p=0; p<rr.getNumParts(); p++) {

				for (int i=rr.getStart(p); i<rr.getEnd(p); i++, ind++) {
					
					double s = context.getLocationMapper().bpToPixel(reference,i);
					double e = context.getLocationMapper().bpToPixel(reference,i+1);
					String ch = ss.subSequence(ind,ind+1).toString();
					
					double height = style.getStyle(ch,"size", defaultHeight);
					double margin = style.getStyle(ch,"margin", defaultMargin);
					Color color = style.getStyle(ch,"color", defaultColor);
					Color bg = style.getStyle(ch,"fill", defaultBackground);
					
					maxHeight = Math.max(maxHeight, height+margin);
					target.rect(s,e,0,height,null,bg);
					
					if (context.getLocationMapper().getPixelsPerBasePair()>=5) {
						target.text(ch,s,e,0,height,color,bg);
					}
				}

			}
			
			
			return new GTracksRenderRequest(maxHeight-1);
			
		};
	}

	
}
