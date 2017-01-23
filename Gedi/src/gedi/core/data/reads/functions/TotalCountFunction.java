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
