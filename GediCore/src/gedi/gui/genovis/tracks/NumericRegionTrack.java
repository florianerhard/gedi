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

import gedi.core.region.GenomicRegion;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;

public abstract class NumericRegionTrack extends NumericTrack<IntervalTree<GenomicRegion,NumericArray>> {

	
		
	public NumericRegionTrack() {
		super((Class)IntervalTree.class);
	}
	
	
	@Override
	public boolean isEmptyData(IntervalTree<GenomicRegion, NumericArray> data) {
		return data.isEmpty();
	}
	
	@Override
	protected  double computeCurrentMin(IntervalTree<GenomicRegion,NumericArray> values) {
		double min = this.fixedMin;
		if (Double.isNaN(min) && values.size()>0) {
			for (NumericArray a : values.values()) {
				double c = rowOpMax.applyAsDouble(a);
				if (Double.isNaN(min) || c<min)
					min = c;
			}
		}
		if (isLogScale()) 
			min = Math.log(min+1)/Math.log(getLogbase());
		return min;
	}
	@Override
	protected double computeCurrentMax(IntervalTree<GenomicRegion,NumericArray> values) {
		double max = this.fixedMax;
		if (Double.isNaN(max) && values.size()>0) {
			for (NumericArray a : values.values()) {
				double c = rowOpMax.applyAsDouble(a);
				if (Double.isNaN(max) || c>max)
					max = c;
			}
		}
		if (isLogScale()) 
			max = Math.log(max+1)/Math.log(getLogbase());
		return max;
	}

	@Override
	protected void renderPass(
			TrackRenderContext<IntervalTree<GenomicRegion, NumericArray>> context,
			int pass) {
		context.data.entrySet().iterator().forEachRemaining(e->renderValue(context,e.getKey(),e.getValue(),pass));
	}

	

	protected abstract int renderValue(
			TrackRenderContext<IntervalTree<GenomicRegion,NumericArray>> context,
			GenomicRegion region, NumericArray value,
			int pass);


	
	
}
