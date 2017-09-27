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

import java.util.function.DoubleBinaryOperator;

import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.functions.IntDoubleConsumer;

public class DenseGenomicNumericProvider extends MutableReferenceGenomicRegion<NumericArray[]> implements GenomicNumericProvider {

	/**
	 * first dimension is row, second pos along region; i.e. the numericArrays represent rows
	 * @param reference
	 * @param region
	 * @param values
	 */
	public DenseGenomicNumericProvider(ReferenceSequence reference,
			GenomicRegion region, NumericArray[] values) {
		set(reference,region,values);
	}


	@Override
	public double getValue(ReferenceSequence reference, int pos, int row) {
		if (reference.equals(getReference()) && getRegion().contains(pos))
			return getData()[row].getDouble(getRegion().induce(pos));
		return 0;
	}
	
	private NumericArray getValues(ReferenceSequence reference, int pos) {
		if (reference.equals(getReference()) && getRegion().contains(pos))
			return getData()[getRegion().induce(pos)];
		return null;
	}
	
	@Override
	public int getLength(String name) {
		if (getReference().getName().equals(name))
			return -getData()[0].length();
		return -1;
	}

	@Override
	public int getNumDataRows() {
		return getData().length;
	}
	
	
//	@Override
//	public double aggregate(ReferenceSequence reference, GenomicRegion region, DoubleBinaryOperator fun) {
//		if (region.isEmpty()) return 0;
//		if (region.getTotalLength()==1) return getValue(reference, region.getStart());
//		double re = getValue(reference, region.map(0));
//		for (int i=1; i<region.getTotalLength(); i++)
//			re = fun.applyAsDouble(re, getValue(reference, region.map(i)));
//		return re;
//	}


	@Override
	public PositionNumericIterator iterateValues(ReferenceSequence reference,
			GenomicRegion region) {
		return new PositionNumericIterator() {
			
			private boolean isset = false;
			private int p = -1;
			
			private int val;
			
			private void tryNext() {
				if (isset) return;
				isset = true;
				while (!getRegion().contains(region.map(++p)) && p<region.getTotalLength());
			}
			
			@Override
			public boolean hasNext() {
				tryNext();
				return p+1<region.getTotalLength();
			}
			
			@Override
			public int nextInt() {
				tryNext();
				isset = false;
				if (p<region.getTotalLength()) {
					val = getRegion().induce(region.map(p));
					return region.map(p);
				}
				throw new IndexOutOfBoundsException();
			}
			
			@Override
			public double getValue(int row) {
				return getData()[row].getDouble(val);
			}
			
			@Override
			public double[] getValues(double[] re) {
				NumericArray[] d = getData();
				if (re==null || re.length!=d.length) 
					re = new double[d.length];
				for (int row=0; row<d.length; row++)
					re[row]=d[row].getDouble(val);
				return re;
			}
		};
	}


	
}
