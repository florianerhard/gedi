package gedi.core.data.numeric;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.TreeMap;

import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.functions.IntDoubleConsumer;

public class SparseRangeGenomicNumericProvider extends SparseGenomicNumericProvider {

	/**
	 * Data contains positions according to genomic location
	 * @param reference
	 * @param region
	 * @param data must contain the leftmost positions of the ranges with associated values
	 */
	public SparseRangeGenomicNumericProvider(ReferenceSequence reference,
			GenomicRegion region, TreeMap<Integer,NumericArray> data) {
		super(reference,region,data);
	}


	@Override
	public double getValue(ReferenceSequence reference, int pos, int row) {
		if (reference.equals(getReference()) && getRegion().contains(pos)) {
			Entry<Integer, NumericArray> re = getData().floorEntry(pos);
			return re==null?0:re.getValue().getDouble(row);
		}
		return 0;
	}
//
//
//
//	@Override
//	public PositionNumericIterator iterateValues(ReferenceSequence reference,
//			GenomicRegion region) {
//		return new PositionNumericIterator() {
//			private double val;
//			int p = 0;
//			Iterator<Entry<Integer, Double>> eit = getData().subMap(region.getStart(p), region.getEnd(p)).entrySet().iterator();
//			
//			private void checkNextPart() {
//				while (!eit.hasNext() && ++p<region.getNumParts()) {
//					eit = getData().subMap(region.getStart(p), region.getEnd(p)).entrySet().iterator();
//				}
//			}
//			
//			@Override
//			public boolean hasNext() {
//				checkNextPart();
//				return eit.hasNext();
//			}
//			
//			@Override
//			public int nextInt() {
//				checkNextPart();
//				Entry<Integer, Double> e = eit.next();
//				val = e.getValue();
//				return e.getKey();
//			}
//			
//			@Override
//			public double getValue() {
//				return val;
//			}
//		};
//	}
//	
	
}
