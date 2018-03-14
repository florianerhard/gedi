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
package gedi.core.data.reads.functions;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.util.ParseUtils;
import gedi.util.datastructure.collections.intcollections.IntArrayList;

import java.util.function.ToDoubleFunction;

public class TotalCountFunction implements ToDoubleFunction<AlignedReadsData> {
	
	
	private int[] conditions;
	private String ranges;
	
	
	public void setConditions(int[] conditions) {
		this.conditions = conditions;
	}

	public void setRange(String ranges) {
		this.ranges = ranges;
	}

	@Override
	public double applyAsDouble(AlignedReadsData value) {
		if (ranges!=null && conditions==null) {
			conditions = ParseUtils.parseRangePositions(ranges, value.getNumConditions(), new IntArrayList()).toIntArray();
		}
		if (conditions==null) 
			return value.getTotalCountOverall(ReadCountMode.All);
		
		double re = 0;
		for (int i : conditions)
			re+=value.getTotalCountForCondition(i, ReadCountMode.All);
		return re;
	}

}
