package gedi.core.data.numeric;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.function.DoubleBinaryOperator;
import java.util.TreeMap;

import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.functions.IntDoubleConsumer;

public class SparseGenomicNumericProvider extends MutableReferenceGenomicRegion<TreeMap<Integer,NumericArray>> implements GenomicNumericProvider {

	/**
	 * Data contains positions according to genomic location
	 * @param reference
	 * @param region
	 * @param data
	 */
	public SparseGenomicNumericProvider(ReferenceSequence reference,
			GenomicRegion region, TreeMap<Integer,NumericArray> data) {
		set(reference,region,data);
	}

	@Override
	public int getNumDataRows() {
		return getData().firstEntry().getValue().length();
	}

	@Override
	public int getLength(String name) {
		if (getReference().getName().equals(name))
			return -getData().lastKey();
		return -1;
	}
	
	@Override
	public double getValue(ReferenceSequence reference, int pos, int row) {
		if (reference.equals(getReference()) && getRegion().contains(pos)) {
			NumericArray re = getData().get(pos);
			return re==null?0:re.getDouble(row);
		}
		return 0;
	}

//	@Override
//	public double aggregate(ReferenceSequence reference, GenomicRegion region, DoubleBinaryOperator fun) {
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
	
	@Override
	public PositionNumericIterator iterateValues(ReferenceSequence reference,
			GenomicRegion region) {
		return new PositionNumericIterator() {
			private double[] val;
			int p = 0;
			Iterator<?> eit = getData().subMap(region.getStart(p), region.getEnd(p)).entrySet().iterator();
			
			private void checkNextPart() {
				while (!eit.hasNext() && ++p<region.getNumParts()) {
					eit = getData().subMap(region.getStart(p), region.getEnd(p)).entrySet().iterator();
				}
			}
			
			@Override
			public boolean hasNext() {
				checkNextPart();
				return eit.hasNext();
			}
			
			@Override
			public int nextInt() {
				checkNextPart();
				Entry<Integer,  double[]> e = (Entry<Integer, double[]>) eit.next();
				val = e.getValue();
				return e.getKey();
			}
			
			@Override
			public double getValue(int row) {
				return val[row];
			}
			
			@Override
			public double[] getValues(double[] re) {
				if (re==null || re.length!=val.length) 
					re = val.clone();
				else 
					System.arraycopy(val, 0, re, 0, re.length);
				return re;
			}
		};
	}
	
	
}
