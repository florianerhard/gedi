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
package gedi.util.datastructure.collections.longcollections;

import java.io.IOException;
import java.util.Arrays;

import gedi.util.ArrayUtils;
import gedi.util.GeneralUtils;
import gedi.util.datastructure.collections.CapacityMode;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryReaderWriter;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;
import gedi.util.math.stat.RandomNumbers;

public class LongArrayList extends AbstractLongCollection implements BinarySerializable {

	
	public final static int INITIAL_SIZE = 100;
	
	private long[] longArray;
	private int count;
	
	private CapacityMode mode = CapacityMode.NextMultiplicativeOf2;
	
	public LongArrayList() {
		this(INITIAL_SIZE);
	}
	
	public LongArrayList(int initialSize) {
		this.longArray = new long[initialSize];
		this.count = 0;
	}
	
	public LongArrayList(long[] a) {
		this.longArray = a.clone();
		this.count = a.length;
	}
	
	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putCInt(count);
		for (int i=0; i<count; i++)
			out.putCLong(longArray[i]);
	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
		this.count = in.getCInt();
		ensureCapacity(this.count);
		for (int i=0; i<this.count; i++)
			this.longArray[i] = in.getCLong();
	}
	
	public void setMode(CapacityMode mode) {
		this.mode = mode;
	}
	
	public CapacityMode getMode() {
		return mode;
	}
	
	@Override
	public LongIterator iterator() {
		return new LongIterator.ArrayIterator(longArray,0,count);
	}
	
	/**
	 * Clears the array in constant time.
	 */
	public void clear() {
		count = 0;
	}
	
	
	public long getLong(int index) {
		return index<count?longArray[index]:0;
	}

	public boolean add(long i) {
		ensureCapacity(count+1);
		longArray[count++]=i;
		return true;
	}
	
	public void set(long[] val) {
		clear();
		for (int i=0; i<val.length; i++)
			set(i,val[i]);
	}
	
	public void set(int index, long val) {
		for (int i=count; i<Math.min(index,longArray.length); i++)
			longArray[i] = 0;
		ensureCapacity(index+1);
		count = Math.max(count,index+1);
		longArray[index]=val;
	}
	
	public void increment(int index) {
		for (int i=count; i<Math.min(index+1,longArray.length); i++)
			longArray[i] = 0;
		ensureCapacity(index+1);
		count = Math.max(count,index+1);
		longArray[index]++;
	}
	
	
	public void decrement(int index) {
		for (int i=count; i<Math.min(index+1,longArray.length); i++)
			longArray[i] = 0;
		ensureCapacity(index+1);
		count = Math.max(count,index+1);
		longArray[index]--;
	}
	
	public void increment(int index, long n) {
		for (int i=count; i<Math.min(index+1,longArray.length); i++)
			longArray[i] = 0;
		ensureCapacity(index+1);
		count = Math.max(count,index+1);
		longArray[index]+=n;
	}
	
	
	public void decrement(int index, long n) {
		for (int i=count; i<Math.min(index+1,longArray.length); i++)
			longArray[i] = 0;
		ensureCapacity(index+1);
		count = Math.max(count,index+1);
		longArray[index]-=n;
	}
	
	/**
	 * If this and other are sorted, this will contain all elements from this and other and will also be sorted.
	 * @param dynamiclongArray
	 */
	public void addSorted(LongArrayList other) {
		ensureCapacity(count+other.count);
		
		int i = count-1;
		int j = other.count-1;
		int index = count+other.count-1;
		
		while (i>=0 && j>=0) {
			if (longArray[i]>other.longArray[j])
				longArray[index--] = longArray[i--];
			else
				longArray[index--] = other.longArray[j--];
		}
		if (j>=0)
			System.arraycopy(other.longArray, 0, longArray, 0, j+1);
			
		count+=other.count;
	}
	
	public void addAll(LongArrayList a) {
		ensureCapacity(count+a.count);
		System.arraycopy(a.longArray, 0, longArray, count, a.count);
		count+=a.count;
	}
	
	public void addAll(long[] a) {
		ensureCapacity(count+a.length);
		System.arraycopy(a, 0, longArray, count, a.length);
		count+=a.length;
	}
	
	/**
	 * Adds all elements from a, starting from s (inclusive) to end (exclusive)
	 * @param a
	 * @param s
	 * @param e
	 */
	public void addAll(long[] a, int s, int e) {
		ensureCapacity(count+e-s);
		System.arraycopy(a, s, longArray, count, e-s);
		count+=e-s;
	}
		
	public long getLastLong() {
		return getLong(size()-1);
	}
	
	/**
	 * i==0 is equivalent to {@link #getLastInt()}
	 * @param i
	 * @return
	 */
	public long getLastLong(int i) {
		return getLong(size()-i-1);
	}
	
	public long removeLast() {
		count--;
		return longArray[count];
	}
	
	/**
	 * Does not maintain ordering!
	 * @param index
	 * @return
	 */
	public long removeEntry(int index) {
		long re = longArray[index];
		longArray[index] = longArray[--count];
		return re;
	}
	
	public int size() {
		return count;
	}
	
	public boolean isEmpty() {
		return size()==0;
	}
	
	public void reverse() {
		ArrayUtils.reverse(longArray, 0, size());
	}
	
	public void shuffle(RandomNumbers stochastics) {
		long longTemp;
		for (int i=0; i<size(); i++) {
			int j = stochastics.getUnif(i, size());
			if (j!=i) {
				longTemp = longArray[i];
				longArray[i] = longArray[j];
				longArray[j] = longTemp;
			}
		}
	}
	
	private void ensureCapacity(int cap) {
		if (longArray==null || longArray.length<=cap) {
			long[] newLong;
			switch (mode) {
			case Exact:
				newLong = new long[cap];
				break;
			case NextMultiplicativeOf2:
				int nm = 1;
				for (;nm<cap; nm<<=1);
				newLong = new long[nm];
				break;
			default:
				throw new RuntimeException("Mode "+mode.toString()+" not supported!");
			}
			System.arraycopy(longArray, 0, newLong, 0, longArray.length);
			longArray = newLong;
		}
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i=0; i<size(); i++)
			sb.append(longArray[i]+",");
		if (sb.length()>1) sb.deleteCharAt(sb.length()-1);
		sb.append("]");
		return sb.toString();
	}
	
	public String toHistogramString(boolean printZero) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<size(); i++)
			if (printZero||longArray[i]>0) {
				sb.append(i);
				sb.append("\t");
				sb.append(longArray[i]);
				sb.append("\n");
			}
		return sb.toString();
	}

	public long[] toLongArray() {
		long[] re = new long[count];
		System.arraycopy(longArray, 0, re, 0, count);
		return re;
	}
	
	public short[] toShortArray() {
		short[] re = new short[count];
		for (int i=0; i<re.length; i++)
			re[i] = GeneralUtils.checkedLongToShort(longArray[i]);
		return re;
	}
	
	public int[] toIntArray() {
		int[] re = new int[count];
		for (int i=0; i<re.length; i++)
			re[i] = GeneralUtils.checkedLongToInt(longArray[i]);
		return re;
	}
	

	public double[] toDoubleArray() {
		double[] re = new double[count];
		for (int i=0; i<re.length; i++)
			re[i] = longArray[i];
		return re;
	}
	
	public double[] toDoubleArray(int size) {
		double[] re = new double[size];
		for (int i=0; i<Math.min(size,count); i++)
			re[i] = longArray[i];
		return re;
	}

	public LongArrayList clone() {
		LongArrayList re = new LongArrayList(longArray);
		re.count = count;
		return re;
	}

	public void getLongs(int start, int end, long[] destination, int dstOffset) {
		System.arraycopy(longArray, start, destination, dstOffset, end-start);
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
	public int binarySearch(long index) {
		return Arrays.binarySearch(longArray, 0, count, index);
	}

	public void sort() {
		Arrays.sort(longArray,0,count);
	}
	
	public void unique() {
		count = ArrayUtils.unique(longArray,0,count);
	}

	public long totalCount() {
		long re = 0;
		for (long i : longArray)
			re+=i;
		return re;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof LongArrayList)) return false;
		LongArrayList o = (LongArrayList) obj;
		return count==o.count && ArrayUtils.equals(longArray, 0, count, o.longArray, 0, count);
	}

	@Override
	public int hashCode() {
        int result = 1;
        for (int i=0; i<count; i++)
            result = 31 * result + Long.hashCode(longArray[i]);

        return result;
	}

	public boolean isSorted() {
		for (int i=1; i<count; i++)
			if (longArray[i-1]>longArray[i])
				return false;
		return true;
	}


	
}
