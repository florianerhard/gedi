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

import gedi.gui.genovis.pixelMapping.PixelBlockToValuesMap;
import gedi.gui.genovis.pixelMapping.PixelLocationMappingBlock;
import gedi.util.datastructure.array.NumericArray;

public abstract class NumericValuesTrack extends NumericTrack<PixelBlockToValuesMap> {
	
	public NumericValuesTrack() {
		super(PixelBlockToValuesMap.class);
	}
	
	@Override
	public boolean isEmptyData(PixelBlockToValuesMap data) {
		return data.size()==0;
	}
	
	
	@Override
	protected  double computeCurrentMin(PixelBlockToValuesMap values) {
		double min = this.fixedMin;
		if (Double.isNaN(min) && values.size()>0) {
			min = rowOpMin.applyAsDouble(values.getValues(0));
			for (int i=1; i<values.size(); i++) {
				double c = rowOpMin.applyAsDouble(values.getValues(i));
				if (Double.isNaN(min) || c<min)
					min = c;
			}
		}
		if (isLogScale()) 
			min = Math.log(min+1)/Math.log(getLogbase());
		return min;
	}
	
	@Override
	protected  double computeCurrentMax(PixelBlockToValuesMap values) {
		double max = this.fixedMax;
		if (Double.isNaN(max) && values!=null && values.size()>0) {
			max = rowOpMin.applyAsDouble(values.getValues(0));
			for (int i=1; i<values.size(); i++) {
				double c = rowOpMax.applyAsDouble(values.getValues(i));
				if (Double.isNaN(max) || c>max)
					max = c;
			}
		}
		if (isLogScale()) 
			max = Math.log(max+1)/Math.log(getLogbase());
		
		if (Double.isNaN(max)) {
			max = this.fixedMax;
			if (Double.isNaN(max) && values!=null && values.size()>0) {
				max = rowOpMin.applyAsDouble(values.getValues(0));
				for (int i=1; i<values.size(); i++) {
					double c = rowOpMax.applyAsDouble(values.getValues(i));
					if (Double.isNaN(max) || c>max)
						max = c;
				}
			}
			if (isLogScale()) 
				max = Math.log(max+1)/Math.log(getLogbase());
		}
		
		return max;
	}

	
	protected boolean isSinglet(PixelBlockToValuesMap data, int i, int p) {
		boolean leftNa = i==0 || data.getValues(i-1).isNA(p);
		boolean rightNa = i==data.size()-1 || data.getValues(i+1).isNA(p);
		boolean singlet = leftNa && rightNa;
		return singlet;
	}
	

	@Override
	protected void renderPass(
			TrackRenderContext<PixelBlockToValuesMap> context,
			int pass) {
		
		for (int i=0; i<context.data.size(); i++) {
			
			
			renderValue(context, 
					i>0?context.data.getBlock(i-1):null,
					i>0?context.data.getValues(i-1):null,
					context.data.getBlock(i),
					context.data.getValues(i),
					i, pass);
		}
		
	}

	protected abstract int renderValue(
			TrackRenderContext<PixelBlockToValuesMap> context,
			PixelLocationMappingBlock prevBlock, NumericArray prev,
			PixelLocationMappingBlock block, NumericArray value,
			int index, int pass);


	
	
}
