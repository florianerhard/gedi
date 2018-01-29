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

package gedi.util.math.stat;

import java.util.Arrays;

import gedi.util.ArrayUtils;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.math.function.StepFunction;
import gedi.util.math.stat.classification.CompleteRocAnalysis;

public class NumericSample extends DoubleArrayList {

	protected boolean sorted = false;
	
	public NumericSample() {
		super();
	}
	
	public NumericSample(int initialSize) {
		super(initialSize);
	}
	
	public NumericSample(double[] a) {
		super(a);
	}
	
	
	
	 /**
     * Searches for the specified value using the
     * binary search algorithm.
     * The array must be sorted (as
     * by the {@link #sort(int[], int, int)} method)
     * prior to making this call.  If it
     * is not sorted, the results are undefined.  If the array contains
     * multiple elements with the specified value, there is no guarantee which
     * one will be found.
     *
     * @param index the value to be searched for
     * @return index of the search key, if it is contained in the array
     *	       within the specified range;
     *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
     *	       <i>insertion point</i> is defined as the point at which the
     *	       key would be inserted into the array: the index of the first
     *	       element in the range greater than the key,
     *	       or <tt>toIndex</tt> if all
     *	       elements in the range are less than the specified key.  Note
     *	       that this guarantees that the return value will be &gt;= 0 if
     *	       and only if the key is found.
     * @throws IllegalArgumentException
     *	       if {@code fromIndex > toIndex}
     * @throws ArrayIndexOutOfBoundsException
     *	       if {@code fromIndex < 0 or toIndex > a.length}
     * @since 1.6
     */
	public int binarySearch(double value) {
		sort();
		return Arrays.binarySearch(doubleArray, 0, count, value);
	}

	/**
	 * re[0] becomes min, re[1] max
	 * @param re
	 */
	public double[] minmax(double[] re) {
		re[0]=re[1]=doubleArray[0];
		for (int i=1; i<count; i++) {
			if (doubleArray[i]>re[1])
				re[1] = doubleArray[i];
			else if (doubleArray[i]<re[0])
				re[0] = doubleArray[i];
		}
		return re;
	}
	
	public double min() {
		double re = doubleArray[0];
		for (int i=1; i<count; i++) 
			re = Math.min(doubleArray[i],re);
		return re;
	}
	public double max() {
		double re = doubleArray[0];
		for (int i=1; i<count; i++) 
			re = Math.min(doubleArray[i],re);
		return re;
	}
	public int argmax() {
		int re = 0;
		for (int i=1; i<count; i++)
			if (doubleArray[i]>doubleArray[re])
				re = i;
		return re;
	}
	public int argmin() {
		int re = 0;
		for (int i=1; i<count; i++)
			if (doubleArray[i]<doubleArray[re])
				re = i;
		return re;
	}
	

	public void unique() {
		count = ArrayUtils.unique(doubleArray,0,count);
	}

	/**
	 * L1
	 */
	public void normalize() {
		double sum = 0;
		for (int i=0; i<count; i++)
			sum+=doubleArray[i];
		for (int i=0; i<count; i++)
			doubleArray[i]/=sum;
	}


	
	
	public int rightTailN(double value) {
		sort();
		int index = binarySearch(value);
		if (index>=0) {
			for (; index>=0 && doubleArray[index]==value; index--);
			return count-index-1;
		}
		else return count+index+1;
	}
	
	public int leftTailN(double value) {
		sort();
		int index = binarySearch(value);
		if (index>=0) {
			for (; index<size() && doubleArray[index]==value; index++);
			return index;
		}
		else return -index-1;
	}
	
	public double rightTailPValue(double value) {
		return rightTailN(value)/(double)count;
	}
	
	public double leftTailPValue(double value) {
		return leftTailN(value)/(double)count;
	}
	
	public double twoTailPValue(double value) {
		sort();
		int index = binarySearch(value);
		if (index>=0) return 2*Math.min((index+1)/(double)count,(count-index)/(double)count);
		else return 2*Math.min((-index-1)/(double)count,(count+index+1)/(double)count);
	}

	public CompleteRocAnalysis roc(NumericSample negative) {
		CompleteRocAnalysis roc = new CompleteRocAnalysis();
		for (int i=0; i<size(); i++)
			roc.addScore(getDouble(i), true);
		for (int i=0; i<negative.size(); i++)
			roc.addScore(negative.getDouble(i), false);
		return roc;
	}
	
	public double getMedian() {
		sort();
		if (size()==0) return Double.NaN;
		return size()%2==1?getDouble(size()/2):(getDouble(size()/2-1)+getDouble(size()/2))*0.5;
	}
	
	public double getQuantile(double quantile) {
		sort();
		if (size()==0) return Double.NaN;
		return getDouble((int) ((size()-1)*quantile));
	}
	

	public double getMad() {
		sort();
		double med = getMedian();
		NumericSample b = new NumericSample(size());
		for (int i=0; i<size(); i++)
			b.add(Math.abs(med-getDouble(i)));
		return b.getMedian();
	}

	public double getSpan() {
		sort();
		return getDouble(size()-1)-getDouble(0);
	}


	@Override
	public void clear() {
		sorted = true;
		super.clear();
	}
	
	@Override
	public boolean add(double e) {
		sorted &= size()==0||e>=getLastDouble();
		return super.add(e);
	}
	
	@Override
	public void set(int index, double val) {
		super.set(index, val);
		sorted &= (index==0 || getDouble(index-1)<=val) && (index==count || getDouble(index+1)>=val);
	}
	
	@Override
	public void increment(int index) {
		sorted = false;
		super.increment(index);
	}
	
	@Override
	public void increment(int index, double n) {
		sorted = false;
		super.increment(index, n);
	}
	
	@Override
	public void decrement(int index) {
		sorted = false;
		super.decrement(index);
	}
	
	@Override
	public void decrement(int index, double n) {
		sorted = false;
		super.decrement(index, n);
	}
	
	@Override
	public void addAll(DoubleArrayList a) {
		sorted = false;
		super.addAll(a);
	}
	
	@Override
	public void addAll(double[] a) {
		sorted = false;
		super.addAll(a);
	}
	
	@Override
	public void addAll(double[] a, int s, int e) {
		sorted = false;
		super.addAll(a,s,e);
	}
	
	@Override
	public void reverse() {
		sorted = false;
		super.reverse();
	}
	
	@Override
	public void shuffle(RandomNumbers stochastics) {
		sorted = false;
		super.shuffle(stochastics);
	}
	

	public void sort() {
		if (!sorted)
			synchronized (doubleArray) {
				super.sort();	
			}
		sorted = true;
	}

	public StepFunction ecdf() {
		if (size()==0) return new StepFunction(new double[0], new double[0]);
		
		sort();
		DoubleArrayList x = new DoubleArrayList(size());
		DoubleArrayList y = new DoubleArrayList(size());
		x.add(doubleArray[0]-1);
		y.add(0);
		
		x.add(doubleArray[0]);
		y.add(1);
		for (int i=1; i<count; i++) {
			if (doubleArray[i]==doubleArray[i-1]) 
				y.increment(y.size()-1);
			else {
				x.add(doubleArray[i]);
				y.add(y.getLastDouble()+1);
			}
		}
		ArrayUtils.mult(y.getRaw(), 0, y.size(), 1.0/size());
		return new StepFunction(x.toDoubleArray(), y.toDoubleArray());
	}
}
