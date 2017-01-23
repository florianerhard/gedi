package gedi.core.data.numeric;

import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;

import java.util.Arrays;
import java.util.Collection;

import cern.colt.bitvector.BitVector;

public class MergedGenomicNumericProvider implements GenomicNumericProvider {

	private GenomicNumericProvider[] pp;
	private int[] cumSizes;
	
	public MergedGenomicNumericProvider(Collection<GenomicNumericProvider> c) {
		this.pp = c.toArray(new GenomicNumericProvider[0]);
		this.cumSizes = new int[pp.length];
		for (int i = 0; i < pp.length; i++) {
			cumSizes[i] = (i>0?cumSizes[i-1]:0)+pp[i].getNumDataRows();
		}
	}
	
	@Override
	public int getNumDataRows() {
		return cumSizes[cumSizes.length-1];
	}
	
	
	@Override
	public int getLength(String name) {
		int min = 0;
		int max = 0;
		for (int i = 0; i < pp.length; i++) {
			int l = pp[i].getLength(name);
			min = Math.min(min,l);
			max = Math.max(max,l);
		}
		if (max>0) return max;
		return min;
		
	}
	
	@Override
	public double getValue(ReferenceSequence reference, int pos, int row) {
		int ind = Arrays.binarySearch(cumSizes, row);
		if (ind<0) ind = -ind-1;
		int off = ind==0?row:row-cumSizes[ind-1];
		return pp[ind].getValue(reference, pos, off);
	}
	
	@Override
	public double getSum(ReferenceSequence reference, GenomicRegion region, int row) {
		int ind = Arrays.binarySearch(cumSizes, row);
		if (ind<0) ind = -ind-1;
		int off = ind==0?row:row-cumSizes[ind-1];
		return pp[ind].getSum(reference, region, off);
	}
	
	@Override
	public double getMin(ReferenceSequence reference, GenomicRegion region, int row) {
		int ind = Arrays.binarySearch(cumSizes, row);
		if (ind<0) ind = -ind-1;
		int off = ind==0?row:row-cumSizes[ind-1];
		return pp[ind].getMin(reference, region, off);
	}
	
	@Override
	public double getMax(ReferenceSequence reference, GenomicRegion region, int row) {
		int ind = Arrays.binarySearch(cumSizes, row);
		if (ind<0) ind = -ind-1;
		int off = ind==0?row:row-cumSizes[ind-1];
		return pp[ind].getMax(reference, region, off);
	}
	
	@Override
	public PositionNumericIterator iterateValues(ReferenceSequence reference,
			GenomicRegion region) {
		
		PositionNumericIterator[] its = new PositionNumericIterator[pp.length];
		for (int i=0; i<its.length; i++)
			its[i] = pp[i].iterateValues(reference, region);
		
		
		return new PositionNumericIterator() {
			private boolean ensured = false;
			private BitVector hasnonext;
			private int smallestPos;
			private int[] next;
			private int N;

			{
				this.N = its.length;
				next = new int[N];
				hasnonext = new BitVector(N);
			}
			
			@Override
			public boolean hasNext() {
				lookAhead();
				return next!=null;
			}

			@Override
			public int nextInt() {
				lookAhead();
				ensured = false;
				return smallestPos;
			}

			private void lookAhead() {
				if (!ensured) {
					for (int i = 0; i < next.length; i++) {
						if (next[i]==smallestPos) {
							if (its[i].hasNext())
								next[i] = its[i].nextInt();
							else 
								hasnonext.putQuick(i, true);
						}
					}
					
					
					if (hasnonext.cardinality()==hasnonext.size())
						next = null;
					else {
						smallestPos = Integer.MAX_VALUE;
						
						hasnonext.forEachIndexFromToInState(0, N-1, false, p->{
							smallestPos = Math.min(smallestPos,next[p]);
							return false;
						});
					}
					ensured = true;
				}
			}


			@Override
			public double[] getValues(double[] re) {
				if (re==null || re.length!=getNumDataRows())
					re = new double[getNumDataRows()];
				Arrays.fill(re, Double.NaN);
				int pos = 0;
				for (int ind=0; ind<its.length; ind++) {
					if (next[ind]==smallestPos)
						for (int off = 0; off<pp[ind].getNumDataRows(); off++)
							re[pos++] = its[ind].getValue(off);
					else
						pos+=pp[ind].getNumDataRows();
				}
				return re;
			}

			@Override
			public double getValue(int row) {
				int ind = Arrays.binarySearch(cumSizes, row);
				if (ind<0) ind = -ind-1;
				int off = ind==0?row:row-cumSizes[ind-1];
				if (next[ind]==smallestPos) return its[ind].getValue(off);
				return Double.NaN;
			}

		};
	}
	
	

//
//	@Override
//	public double aggregate(ReferenceSequence reference, GenomicRegion region,
//			DoubleBinaryOperator fun) {
//		if (region.isEmpty()) return 0;
//		if (region.getTotalLength()==1) return getValue(reference, region.getStart());
//		PositionNumericIterator pit = iterateValues(reference, region);
//		double re = Double.NaN;
//		int lp = region.getStart()-1;
//		boolean gap = false;
//		while (pit.hasNext()) {
//			int p = pit.nextInt();
//			double value = pit.getValue();
//			re = Double.isNaN(re)?value:fun.applyAsDouble(re, value);
//			gap|=p-lp!=1;
//			lp = p;
//		}
//		if (gap)
//			re = Double.isNaN(re)?0:fun.applyAsDouble(re, 0);
//		return re;
//	}
	
	
}
