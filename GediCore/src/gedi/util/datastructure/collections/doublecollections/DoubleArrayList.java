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
package gedi.util.datastructure.collections.doublecollections;

import gedi.util.ArrayUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.collections.CapacityMode;
import gedi.util.math.stat.RandomNumbers;

import java.util.Arrays;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.stat.descriptive.UnivariateStatistic;

public class DoubleArrayList extends AbstractDoubleCollection {


	public final static int INITIAL_SIZE = 100;

	protected double[] doubleArray;
	protected int count;

	protected CapacityMode mode = CapacityMode.NextMultiplicativeOf2;

	public DoubleArrayList() {
		this(INITIAL_SIZE);
	}

	public DoubleArrayList(int initialSize) {
		this.doubleArray = new double[initialSize];
		this.count = 0;
	}

	public DoubleArrayList(double[] a) {
		this.doubleArray = a.clone();
		this.count = a.length;
	}

	public void setMode(CapacityMode mode) {
		this.mode = mode;
	}

	public CapacityMode getMode() {
		return mode;
	}

	@Override
	public DoubleIterator iterator() {
		return new DoubleIterator.ArrayIterator(doubleArray,0,count);
	}

	/**
	 * Clears the array in constant time.
	 */
	public void clear() {
		count = 0;
	}


	public double getDouble(int index) {
		return index<count?doubleArray[index]:0;
	}

	public boolean add(double i) {
		ensureCapacity(count+1);
		doubleArray[count++]=i;
		return true;
	}

	public boolean add(Double i) {
		ensureCapacity(count+1);
		doubleArray[count++]=i;
		return true;
	}

	public boolean addIfNotNaN(double i) {
		if (Double.isNaN(i)) return false;
		return add(i);
	}


	public void set(int index, double val) {
		for (int i=count; i<Math.min(index,doubleArray.length); i++)
			doubleArray[i] = 0;
		ensureCapacity(index+1);
		count = Math.max(count,index+1);
		doubleArray[index]=val;
	}

	public void increment(int index) {
		for (int i=count; i<Math.min(index+1,doubleArray.length); i++)
			doubleArray[i] = 0;
		ensureCapacity(index+1);
		count = Math.max(count,index+1);
		doubleArray[index]++;
	}


	public void decrement(int index) {
		for (int i=count; i<Math.min(index+1,doubleArray.length); i++)
			doubleArray[i] = 0;
		ensureCapacity(index+1);
		count = Math.max(count,index+1);
		doubleArray[index]--;
	}

	public void increment(int index, double n) {
		for (int i=count; i<Math.min(index+1,doubleArray.length); i++)
			doubleArray[i] = 0;
		ensureCapacity(index+1);
		count = Math.max(count,index+1);
		doubleArray[index]+=n;
	}


	public void decrement(int index, double n) {
		for (int i=count; i<Math.min(index+1,doubleArray.length); i++)
			doubleArray[i] = 0;
		ensureCapacity(index+1);
		count = Math.max(count,index+1);
		doubleArray[index]-=n;
	}

	/**
	 * If this and other are sorted, this will contain all elements from this and other and will also be sorted.
	 * @param dynamicIntArray
	 */
	public void addSorted(DoubleArrayList other) {
		ensureCapacity(count+other.count);

		int i = count-1;
		int j = other.count-1;
		int index = count+other.count-1;

		while (i>=0 && j>=0) {
			if (doubleArray[i]>other.doubleArray[j])
				doubleArray[index--] = doubleArray[i--];
			else
				doubleArray[index--] = other.doubleArray[j--];
		}
		if (j>=0)
			System.arraycopy(other.doubleArray, 0, doubleArray, 0, j+1);

		count+=other.count;
	}

	public void addAll(DoubleArrayList a) {
		ensureCapacity(count+a.count);
		System.arraycopy(a.doubleArray, 0, doubleArray, count, a.count);
		count+=a.count;
	}

	public void addAll(double[] a) {
		ensureCapacity(count+a.length);
		System.arraycopy(a, 0, doubleArray, count, a.length);
		count+=a.length;
	}

	/**
	 * Adds all elements from a, starting from s (inclusive) to end (exclusive)
	 * @param a
	 * @param s
	 * @param e
	 */
	public void addAll(double[] a, int s, int e) {
		ensureCapacity(count+e-s);
		System.arraycopy(a, s, doubleArray, count, e-s);
		count+=e-s;
	}

	public double getLastDouble() {
		return getDouble(size()-1);
	}

	/**
	 * i==0 is equivalent to {@link #getLastInt()}
	 * @param i
	 * @return
	 */
	public double getLastDouble(int i) {
		return getDouble(size()-i-1);
	}

	public double removeLast() {
		count--;
		return doubleArray[count];
	}

	public double removeEntry(int index) {
		double re = doubleArray[index];
		doubleArray[index] = doubleArray[--count];
		return re;
	}

	public int size() {
		return count;
	}

	public boolean isEmpty() {
		return size()==0;
	}

	public void reverse() {
		ArrayUtils.reverse(doubleArray, 0, size());
	}

	public void shuffle(RandomNumbers stochastics) {
		double intTemp;
		for (int i=0; i<size(); i++) {
			int j = stochastics.getUnif(i, size());
			if (j!=i) {
				intTemp = doubleArray[i];
				doubleArray[i] = doubleArray[j];
				doubleArray[j] = intTemp;
			}
		}
	}

	private void ensureCapacity(int cap) {
		if (doubleArray==null || doubleArray.length<=cap) {
			double[] newDouble;
			switch (mode) {
			case Exact:
				newDouble = new double[cap];
				break;
			case NextMultiplicativeOf2:
				int nm = 1;
				for (;nm<cap; nm<<=1);
				newDouble = new double[nm];
				break;
			default:
				throw new RuntimeException("Mode "+mode.toString()+" not supported!");
			}
			System.arraycopy(doubleArray, 0, newDouble, 0, doubleArray.length);
			doubleArray = newDouble;
		}
	}

	@Override
	public String toString() {
		return toString("[",",","]");
	}

	public String toString(String prefix, String infix, String suffix) {
		StringBuilder sb = new StringBuilder();
		sb.append(prefix);
		if (size()>0)
			sb.append(doubleArray[0]);
		for (int i=1; i<size(); i++) {
			sb.append(infix);
			sb.append(doubleArray[i]);
		}
		sb.append(suffix);
		return sb.toString();
	}

	public String toHistogramString(boolean printZero) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<size(); i++)
			if (printZero||doubleArray[i]>0) {
				sb.append(i);
				sb.append("\t");
				sb.append(doubleArray[i]);
				sb.append("\n");
			}
		return sb.toString();
	}

	public NumericArray toNumericArray() {
		return NumericArray.wrap(doubleArray,0,size());
	}
	
	public float[] toFloatArray() {
		float[] re = new float[count];
		for (int i=0; i<count; i++)
			re[i] = (float)doubleArray[i];
		return re;
	}

	public double[] toDoubleArray() {
		double[] re = new double[count];
		for (int i=0; i<re.length; i++)
			re[i] = doubleArray[i];
		return re;
	}

	public double[] toDoubleArray(int size) {
		double[] re = new double[size];
		for (int i=0; i<Math.min(size,count); i++)
			re[i] = doubleArray[i];
		return re;
	}

	public DoubleArrayList clone() {
		DoubleArrayList re = new DoubleArrayList(doubleArray);
		re.count = count;
		return re;
	}

	public void getDoubles(int start, int end, double[] destination, int dstOffset) {
		System.arraycopy(doubleArray, start, destination, dstOffset, end-start);
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
	public int binarySearch(int index) {
		return Arrays.binarySearch(doubleArray, 0, count, index);
	}


	public void parallelSort(DoubleArrayList other) {
		if (size() != other.size()) 
			throw new DimensionMismatchException(size(), other.size());
		
		ArrayUtils.parallelSort(this.doubleArray, other.doubleArray,0,size());
	}


	public void sort() {
		Arrays.sort(doubleArray,0,count);
	}

	public void unique() {
		count = ArrayUtils.unique(doubleArray,0,count);
	}

	public void cumSum(int dir) {
		ArrayUtils.cumSumInPlace(doubleArray, dir);
	}

	public double multByIndex() {
		double re = 0;
		for (int i=1; i<size(); i++)
			re+=i*getDouble(i);
		return re;
	}
	
	public double[] getRaw() {
		return doubleArray;
	}

	public double totalCount() {
		double re = 0;
		for (double i : doubleArray)
			re+=i;
		return re;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof DoubleArrayList)) return false;
		DoubleArrayList o = (DoubleArrayList) obj;
		return count==o.count && ArrayUtils.equals(doubleArray, 0, count, o.doubleArray, 0, count);
	}

	@Override
	public int hashCode() {
		int result = 1;
		for (int i=0; i<count; i++) {
			long bits = Double.doubleToLongBits(doubleArray[i]);
			result = 31 * result + (int)(bits ^ (bits >>> 32));;
		}

		return result;
	}

	public double evaluate(UnivariateStatistic stat) {
		return stat.evaluate(doubleArray, 0, count);
	}




}
