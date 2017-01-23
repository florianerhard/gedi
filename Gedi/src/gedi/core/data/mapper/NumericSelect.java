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
