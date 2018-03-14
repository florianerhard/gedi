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

import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.util.datastructure.array.NumericArray;

import java.util.function.DoubleBinaryOperator;

public class ComputedGenomicNumericProvider implements GenomicNumericProvider {

	private GenomicNumericProvider a;
	private GenomicNumericProvider b;
	private DoubleBinaryOperator fun;
	
	public ComputedGenomicNumericProvider(GenomicNumericProvider a,
			GenomicNumericProvider b, DoubleBinaryOperator fun) {
		if (a.getNumDataRows()!=b.getNumDataRows())
			throw new IllegalArgumentException("Incompatible inputs!");
		this.a = a;
		this.b = b;
		this.fun = fun;
	}
	
	@Override
	public int getNumDataRows() {
		return a.getNumDataRows();
	}
	
	
	@Override
	public int getLength(String name) {
		int a = this.a.getLength(name);
		int b = this.b.getLength(name);
		if (Math.max(a,b)>0) return Math.max(a,b);
		return Math.min(a,b);
		
	}
	
	@Override
	public double getValue(ReferenceSequence reference, int pos, int row) {
		return fun.applyAsDouble(a.getValue(reference, pos,row),b.getValue(reference, pos,row));
	}
	
	@Override
	public PositionNumericIterator iterateValues(ReferenceSequence reference,
			GenomicRegion region) {
		return new PositionNumericIterator() {
			
			PositionNumericIterator ait = a.iterateValues(reference,region);
			PositionNumericIterator bit = b.iterateValues(reference,region);
			int apos=-1, bpos=-1;
			boolean rea=false, reb=false;
			
			@Override
			public boolean hasNext() {
				return apos<0 || bpos<0 || ait.hasNext() || bit.hasNext();
			}
			
			@Override
			public int nextInt() {
				if (apos==-1 && ait.hasNext()) 
					apos = ait.nextInt();
				if (bpos==-1 && bit.hasNext()) 
					bpos = bit.nextInt();
				
				rea = reb = false;
				
				if (apos>=0 && bpos>=0) {
					int re = Math.min(apos,bpos);
					rea = re==apos;
					reb = re==bpos;
					if (rea) apos = -1;
					if (reb) bpos = -1;
					return re;
				}
				if (apos>=0) {
					int re = apos;
					rea = true;
					apos = -1;
					return re;
				}
				if (bpos>=0) {
					int re = bpos;
					reb = true;
					bpos = -1;
					return re;
				}
				throw new IndexOutOfBoundsException();
			}
			
			@Override
			public double getValue(int row) {
				if (rea && reb) return fun.applyAsDouble(ait.getValue(row), bit.getValue(row));
				if (rea) return ait.getValue(row);
				if (reb) return bit.getValue(row);
				throw new RuntimeException();
			}
			

			@Override
			public double[] getValues(double[] re) {
				if (re==null || re.length!=getNumDataRows()) 
					re = new double[getNumDataRows()];
				for (int row=0; row<re.length; row++)
					re[row]=getValue(row);
				return re;
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
