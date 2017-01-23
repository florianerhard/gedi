package gedi.util.parsing;

import gedi.util.ParseUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.collections.intcollections.IntArrayList;

public class IntArrayParser implements Parser<int[]> {
	@Override
	public int[] apply(String s) {
		return ParseUtils.parseRangePositions(s, -1, new IntArrayList()).toIntArray();
//		return StringUtils.parseInt(StringUtils.split(s, ','));
	}

	@Override
	public Class<int[]> getParsedType() {
		return int[].class;
	}
}