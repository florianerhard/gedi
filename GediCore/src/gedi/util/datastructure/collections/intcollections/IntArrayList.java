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
package gedi.util.datastructure.collections.intcollections;

import gedi.util.ArrayUtils;
import gedi.util.GeneralUtils;
import gedi.util.datastructure.collections.CapacityMode;
import gedi.util.math.stat.RandomNumbers;

import java.util.Arrays;

public class IntArrayList extends AbstractIntCollection {

	
	public final static int INITIAL_SIZE = 100;
	
	private int[] intArray;
	private int count;
	
	private CapacityMode mode = CapacityMode.NextMultiplicativeOf2;
	
	public IntArrayList() {
		this(INITIAL_SIZE);
	}
	
	public IntArrayList(int initialSize) {
		this.intArray = new int[initialSize];
		this.count = 0;
	}
	
	public IntArrayList(int[] a) {
		this.intArray = a.clone();
		this.count = a.length;
	}
	
	public void setMode(CapacityMode mode) {
		this.mode = mode;
	}
	
	public CapacityMode getMode() {
		return mode;
	}
	
	@Override
	public IntIterator iterator() {
		return new IntIterator.ArrayIterator(intArray,0,count);
	}
	
	/**
	 * Clears the array in constant time.
	 */
	public void clear() {
		count = 0;
	}
	
	
	public int getInt(int index) {
		return index<count?intArray[index]:0;
	}

	public boolean add(int i) {
		ensureCapacity(count+1);
		intArray[count++]=i;
		return true;
	}
	
	public void set(int[] val) {
		clear();
		for (int i=0; i<val.length; i++)
			set(i,val[i]);
	}
	
	public void set(int index, int val) {
		for (int i=count; i<Math.min(index,intArray.length); i++)
			intArray[i] = 0;
		ensureCapacity(index+1);
		count = Math.max(count,index+1);
		intArray[index]=val;
	}
	
	public void increment(int index) {
		for (int i=count; i<Math.min(index+1,intArray.length); i++)
			intArray[i] = 0;
		ensureCapacity(index+1);
		count = Math.max(count,index+1);
		intArray[index]++;
	}
	
	
	public void decrement(int index) {
		for (int i=count; i<Math.min(index+1,intArray.length); i++)
			intArray[i] = 0;
		ensureCapacity(index+1);
		count = Math.max(count,index+1);
		intArray[index]--;
	}
	
	public void increment(int index, int n) {
		for (int i=count; i<Math.min(index+1,intArray.length); i++)
			intArray[i] = 0;
		ensureCapacity(index+1);
		count = Math.max(count,index+1);
		intArray[index]+=n;
	}
	
	
	public void decrement(int index, int n) {
		for (int i=count; i<Math.min(index+1,intArray.length); i++)
			intArray[i] = 0;
		ensureCapacity(index+1);
		count = Math.max(count,index+1);
		intArray[index]-=n;
	}
	
	/**
	 * If this and other are sorted, this will contain all elements from this and other and will also be sorted.
	 * @param dynamicIntArray
	 */
	public void addSorted(IntArrayList other) {
		ensureCapacity(count+other.count);
		
		int i = count-1;
		int j = other.count-1;
		int index = count+other.count-1;
		
		while (i>=0 && j>=0) {
			if (intArray[i]>other.intArray[j])
				intArray[index--] = intArray[i--];
			else
				intArray[index--] = other.intArray[j--];
		}
		if (j>=0)
			System.arraycopy(other.intArray, 0, intArray, 0, j+1);
			
		count+=other.count;
	}
	
	public void addAll(IntArrayList a) {
		ensureCapacity(count+a.count);
		System.arraycopy(a.intArray, 0, intArray, count, a.count);
		count+=a.count;
	}
	
	public void addAll(int[] a) {
		ensureCapacity(count+a.length);
		System.arraycopy(a, 0, intArray, count, a.length);
		count+=a.length;
	}
	
	/**
	 * Adds all elements from a, starting from s (inclusive) to end (exclusive)
	 * @param a
	 * @param s
	 * @param e
	 */
	public void addAll(int[] a, int s, int e) {
		ensureCapacity(count+e-s);
		System.arraycopy(a, s, intArray, count, e-s);
		count+=e-s;
	}
		
	public int getLastInt() {
		return getInt(size()-1);
	}
	
	/**
	 * i==0 is equivalent to {@link #getLastInt()}
	 * @param i
	 * @return
	 */
	public int getLastInt(int i) {
		return getInt(size()-i-1);
	}
	
	public int removeLast() {
		count--;
		return intArray[count];
	}
	
	/**
	 * Does not maintain ordering!
	 * @param index
	 * @return
	 */
	public int removeEntry(int index) {
		int re = intArray[index];
		intArray[index] = intArray[--count];
		return re;
	}
	
	public int size() {
		return count;
	}
	
	public boolean isEmpty() {
		return size()==0;
	}
	
	public void reverse() {
		ArrayUtils.reverse(intArray, 0, size());
	}
	
	public void shuffle(RandomNumbers stochastics) {
		int intTemp;
		for (int i=0; i<size(); i++) {
			int j = stochastics.getUnif(i, size());
			if (j!=i) {
				intTemp = intArray[i];
				intArray[i] = intArray[j];
				intArray[j] = intTemp;
			}
		}
	}
	
	public void compact() {
		if (intArray.length>count) 
			intArray = ArrayUtils.redimPreserve(intArray, count);
	}
	
	private void ensureCapacity(int cap) {
		if (intArray==null || intArray.length<=cap) {
			int[] newInt;
			switch (mode) {
			case Exact:
				newInt = new int[cap];
				break;
			case NextMultiplicativeOf2:
				int nm = 1;
				for (;nm<cap; nm<<=1);
				newInt = new int[nm];
				break;
			default:
				throw new RuntimeException("Mode "+mode.toString()+" not supported!");
			}
			System.arraycopy(intArray, 0, newInt, 0, intArray.length);
			intArray = newInt;
		}
	}
	
	@Override
	public String toString() {
		return toString("[",",","]");
	}
	
	public String toString(String pref, String sep, String suff) {
		StringBuilder sb = new StringBuilder();
		sb.append(pref);
		for (int i=0; i<size(); i++) {
			if (i>0) sb.append(sep);
			sb.append(intArray[i]);
		}
		sb.append(suff);
		return sb.toString();
	}
	
	public String toHistogramString(boolean printZero) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<size(); i++)
			if (printZero||intArray[i]>0) {
				sb.append(i);
				sb.append("\t");
				sb.append(intArray[i]);
				sb.append("\n");
			}
		return sb.toString();
	}

	public int[] toIntArray() {
		int[] re = new int[count];
		System.arraycopy(intArray, 0, re, 0, count);
		return re;
	}
	
	public short[] toShortArray() {
		short[] re = new short[count];
		for (int i=0; i<re.length; i++)
			re[i] = GeneralUtils.checkedIntToShort(intArray[i]);
		return re;
	}
	

	public double[] toDoubleArray() {
		double[] re = new double[count];
		for (int i=0; i<re.length; i++)
			re[i] = intArray[i];
		return re;
	}
	
	public double[] toDoubleArray(int size) {
		double[] re = new double[size];
		for (int i=0; i<Math.min(size,count); i++)
			re[i] = intArray[i];
		return re;
	}

	public IntArrayList clone() {
		IntArrayList re = new IntArrayList(intArray);
		re.count = count;
		return re;
	}

	public void getInts(int start, int end, int[] destination, int dstOffset) {
		System.arraycopy(intArray, start, destination, dstOffset, end-start);
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
		return Arrays.binarySearch(intArray, 0, count, index);
	}

	public void sort() {
		Arrays.sort(intArray,0,count);
	}
	
	public void unique() {
		count = ArrayUtils.unique(intArray,0,count);
	}

	public void cumSum(int dir) {
		ArrayUtils.cumSumInPlace(intArray, dir);
	}

	public int multByIndex() {
		int re = 0;
		for (int i=1; i<size(); i++)
			re+=i*getInt(i);
		return re;
	}

	public int totalCount() {
		int re = 0;
		for (int i : intArray)
			re+=i;
		return re;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof IntArrayList)) return false;
		IntArrayList o = (IntArrayList) obj;
		return count==o.count && ArrayUtils.equals(intArray, 0, count, o.intArray, 0, count);
	}

	@Override
	public int hashCode() {
        int result = 1;
        for (int i=0; i<count; i++)
            result = 31 * result + intArray[i];

        return result;
	}

	public boolean isSorted() {
		for (int i=1; i<count; i++)
			if (intArray[i-1]>intArray[i])
				return false;
		return true;
	}

	public int[] getRaw() {
		return intArray;
	}
	
}
