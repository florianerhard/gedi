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
package gedi.core.data.numeric;


import gedi.core.data.annotation.ReferenceSequenceLengthProvider;
import gedi.core.data.numeric.GenomicNumericProvider.PositionNumericIterator;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.datastructure.collections.intcollections.IntIterator;

import java.util.function.DoubleBinaryOperator;

public interface GenomicNumericProvider extends ReferenceSequenceLengthProvider {

	
	public enum SpecialAggregators{
		Min {
			@Override
			public double getAggregatedValue(GenomicNumericProvider provider,
					ReferenceSequence reference, GenomicRegion region, int row) {
				return provider.getMin(reference, region, row);
			}
			@Override
			public double increment(double a, double b) {
				return Math.min(a,b);
			}
			@Override
			public double getIncrementalResult(double total, int count) {
				return total;
			}
		},Max {
			@Override
			public double getAggregatedValue(GenomicNumericProvider provider,
					ReferenceSequence reference, GenomicRegion region, int row) {
				return provider.getMax(reference, region, row);
			}
			@Override
			public double increment(double a, double b) {
				return Math.max(a,b);
			}
			@Override
			public double getIncrementalResult(double total, int count) {
				return total;
			}
		},Mean {
			@Override
			public double getAggregatedValue(GenomicNumericProvider provider,
					ReferenceSequence reference, GenomicRegion region, int row) {
				return provider.getMean(reference, region, row);
			}
			@Override
			public double increment(double a, double b) {
				return a+b;
			}
			@Override
			public double getIncrementalResult(double total, int count) {
				return total/count;
			}
		},AvailableMean {
			@Override
			public double getAggregatedValue(GenomicNumericProvider provider,
					ReferenceSequence reference, GenomicRegion region, int row) {
				return provider.getAvailableMean(reference, region, row);
			}
			
			@Override
			public double increment(double a, double b) {
				return a+b;
			}
			@Override
			public double getIncrementalResult(double total, int count) {
				return total/count;
			}
		},Sum {
			@Override
			public double getAggregatedValue(GenomicNumericProvider provider,
					ReferenceSequence reference, GenomicRegion region, int row) {
				return provider.getSum(reference, region, row);
			}
			@Override
			public double increment(double a, double b) {
				return a+b;
			}
			@Override
			public double getIncrementalResult(double total, int count) {
				return total;
			}
		};
		
		
		public abstract double getAggregatedValue(GenomicNumericProvider provider, ReferenceSequence reference, GenomicRegion region, int row);
		public abstract double increment(double a, double b);
		public abstract double getIncrementalResult(double total, int count);
		
		
	}
	
	
	int getNumDataRows();
	
	

	double getValue(ReferenceSequence reference, int pos, int row);
	
	default double[] getValues(ReferenceSequence reference, int pos, double[] re) {
		for (int i=0; i<re.length; i++)
			re[i] = getValue(reference, pos, i);
		return re;
	}
	
	
	PositionNumericIterator iterateValues(ReferenceSequence reference, GenomicRegion region);
	
	default PositionNumericIterator iterateValues(ReferenceGenomicRegion<?> rgr) {
		return iterateValues(rgr.getReference(),rgr.getRegion());
	}
	
	public static interface PositionNumericIterator extends IntIterator {
		double[] getValues(double[] re);
		double getValue(int row);
	}
	
	public static PositionNumericIterator empty() {
		return new PositionNumericIterator() {
			@Override
			public int nextInt() {
				return 0;
			}

			@Override
			public boolean hasNext() {
				return false;
			}

			@Override
			public double[] getValues(double[] re) {
				return null;
			}

			@Override
			public double getValue(int row) {
				return 0;
			}
			
		};
	}
	
	public default double aggregatedValue(ReferenceSequence reference, GenomicRegion region, int row, DoubleBinaryOperator aggregator) {
		PositionNumericIterator it = iterateValues(reference, region);
		if (!it.hasNext()) return Double.NaN;
		it.next();
		double re = it.getValue(row);
		while (it.hasNext()) {
			it.next();
			re = aggregator.applyAsDouble(re, it.getValue(row));
		}
		return re;
	}
	
	
	public default double getSum(ReferenceSequence reference, GenomicRegion region, int row) {
		return aggregatedValue(reference, region, row, (a,b)->a+b);
	}
	
	public default double getMean(ReferenceSequence reference, GenomicRegion region, int row) {
		return getSum(reference, region, row)/region.getTotalLength();
	}
	
	/**
	 * Sum divided by the number of available positions
	 * @param reference
	 * @param region
	 * @param row
	 * @return
	 */
	public default double getAvailableMean(ReferenceSequence reference, GenomicRegion region,
			int row) {
		PositionNumericIterator it = iterateValues(reference, region);
		if (!it.hasNext()) return Double.NaN;
		it.next();
		double re = it.getValue(row);
		int n = 0;
		while (it.hasNext()) {
			it.next();
			n++;
			re = re+it.getValue(row);
		}
		return re/n;
	}
	
	public default double getMin(ReferenceSequence reference, GenomicRegion region, int row) {
		return aggregatedValue(reference, region, row, Math::min);
	}
	
	public default double getMax(ReferenceSequence reference, GenomicRegion region, int row) {
		return aggregatedValue(reference, region, row, Math::max);
	}
	
}
