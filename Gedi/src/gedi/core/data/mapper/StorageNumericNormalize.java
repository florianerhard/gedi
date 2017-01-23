package gedi.core.data.mapper;

import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;

import java.util.function.UnaryOperator;

@GenomicRegionDataMapping(fromType=IntervalTree.class,toType=IntervalTree.class)
public class StorageNumericNormalize extends StorageNumericCompute {

	

	public StorageNumericNormalize(double[] totals) {
		super(new RpmNormalize(totals));
	}

	private static class RpmNormalize implements UnaryOperator<NumericArray> {

		private double[] totals;

		public RpmNormalize(double[] totals) {
			this.totals = totals;
		}

		@Override
		public NumericArray apply(NumericArray t) {
			NumericArray re = t.copy();
			for (int i=0; i<re.length(); i++)
				re.mult(i,1E6/totals[i]);
			return re;
		}
		
	}


	
}
