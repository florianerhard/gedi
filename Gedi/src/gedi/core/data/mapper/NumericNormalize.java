package gedi.core.data.mapper;

import gedi.gui.genovis.pixelMapping.PixelBlockToValuesMap;
import gedi.util.datastructure.array.NumericArray;

import java.util.function.UnaryOperator;

@GenomicRegionDataMapping(fromType=PixelBlockToValuesMap.class,toType=PixelBlockToValuesMap.class)
public class NumericNormalize extends NumericCompute {

	

	public NumericNormalize(double[] totals) {
		super(new RpmNormalize(totals));
	}

	private static class RpmNormalize implements UnaryOperator<NumericArray> {

		private double[] totals;

		public RpmNormalize(double[] totals) {
			this.totals = totals;
		}

		@Override
		public NumericArray apply(NumericArray t) {
			if (totals.length!=1 && totals.length!=t.length())
				throw new RuntimeException("Given normalization constants do not match the input data!");
			NumericArray re = t.copy();
			for (int i=0; i<re.length(); i++)
				re.mult(i,1E6/totals[totals.length==1?0:i]);
			return re;
		}
		
	}


	
}
