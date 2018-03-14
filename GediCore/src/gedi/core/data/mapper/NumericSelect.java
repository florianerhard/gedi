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
package gedi.core.data.mapper;

import gedi.gui.genovis.pixelMapping.PixelBlockToValuesMap;
import gedi.util.ParseUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.collections.intcollections.IntArrayList;

import java.util.function.UnaryOperator;

@GenomicRegionDataMapping(fromType=PixelBlockToValuesMap.class,toType=PixelBlockToValuesMap.class)
public class NumericSelect extends NumericCompute {

	

	public NumericSelect(String range) {
		super(new SelectOp(range));
	}

	private static class SelectOp implements UnaryOperator<NumericArray> {

		private String range;
		private int[] pos;

		public SelectOp(String range) {
			this.range = range;
		}

		@Override
		public NumericArray apply(NumericArray t) {
			if (pos==null) 
				pos = ParseUtils.parseRangePositions(range, t.length(), new IntArrayList()).toIntArray();
			
			NumericArray re = NumericArray.createMemory(pos.length, t.getType());
			for (int i=0; i<pos.length; i++)
				re.copy(t, pos[i], i);
			
			return re;
		}
		
	}


	
}
