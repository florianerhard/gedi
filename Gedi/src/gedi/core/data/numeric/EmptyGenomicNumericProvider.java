package gedi.core.data.numeric;

import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;

public class EmptyGenomicNumericProvider implements GenomicNumericProvider {

	@Override
	public int getNumDataRows() {
		return 0;
	}

	@Override
	public double getValue(ReferenceSequence reference, int pos, int row) {
		return 0;
	}
	
	@Override
	public int getLength(String name) {
		return -1;
	}

	@Override
	public PositionNumericIterator iterateValues(ReferenceSequence reference,
			GenomicRegion region) {
		return new PositionNumericIterator() {
			
			@Override
			public boolean hasNext() {
				return false;
			}
			
			@Override
			public int nextInt() {
				throw new RuntimeException();
			}
			
			@Override
			public double getValue(int row) {
				return 0;
			}
			
			@Override
			public double[] getValues(double[] re) {
				return re;
			}
		};
	}

}
