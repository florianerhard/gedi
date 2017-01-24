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
import gedi.core.region.GenomicRegion;
import gedi.util.PaintUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.mutable.MutableDouble;

import java.awt.geom.CubicCurve2D;


@GenomicRegionDataMapping(fromType=IntervalTree.class)
public class RegionLinesTrack extends NumericRegionTrack {
	
	
	private double yFactor = .5;
	private double xFactor = .5;

	@Override
	protected int getPassesCount(
			TrackRenderContext<IntervalTree<GenomicRegion,NumericArray>> context) {
		if (context.data==null || context.data.size()==0) return 0;
		return context.data.values().iterator().next().length();
	}
	
	
	@Override
	protected void beginPass(TrackRenderContext<IntervalTree<GenomicRegion,NumericArray>> context, int row) {
		context.g2.setPaint(PaintUtils.parseColor(getStyles().get("["+row+"].color").asString("#000000")));
	}
	
	@Override
	protected int renderValue(
			TrackRenderContext<IntervalTree<GenomicRegion,NumericArray>> context,
			GenomicRegion region, NumericArray value, int pass) {
		
		
		
		double min = context.<MutableDouble>get(MIN_VALUE).N;
		double max = context.<MutableDouble>get(MAX_VALUE).N;
		
		double x1 = getBounds().getX()+viewer.getLocationMapper().bpToPixel(context.reference,region.getStart());
		double x2 = getBounds().getX()+viewer.getLocationMapper().bpToPixel(context.reference,region.getEnd());
		
		double bp1 = region.induceMaybeOutside(viewer.getLocationMapper().pixelToBp(x1-getBounds().getX()))/(double)(region.getTotalLength());
		double bp2 = region.induceMaybeOutside(viewer.getLocationMapper().pixelToBp(x2-getBounds().getX()))/(double)(region.getTotalLength());
		
		double y = getY(transform(value.getDouble(pass)),min,max);
		double y01 = getY(transform(0),min,max)*(1-bp1)+y*bp1;
		double y02 = getY(transform(0),min,max)*(bp2)+y*(1-bp2);
		
		double xc = (x1+x2)/2;
		
		context.g2.draw(new CubicCurve2D.Double(x1, y01, x1, y01+(y-y01)*yFactor, xc-(xc-x1)*xFactor, y, xc, y));
		context.g2.draw(new CubicCurve2D.Double(x2, y02, x2, y02+(y-y02)*yFactor, xc+(xc-x1)*xFactor, y, xc, y));
		
		
		return 1;
	}
	
	
}
