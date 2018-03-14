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

/**
 * Mutable w.r.t. to position (not to array)
 * @author erhard
 *
 */
public class LongArraySlice extends AbstractLongCollection {

	private long[] array;
	private int start;
	private int end;
	
	/**
	 * Array is not copied!
	 * @param array
	 * @param start
	 * @param end
	 */
	public LongArraySlice(long[] array, int start, int end) {
		this.array = array;
		this.start = start;
		this.end = end;
	}
	
	public LongArraySlice setSlice(int start,int end) {
		this.start = start;
		this.end = end;
		return this;
	}
	
	public int getStart() {
		return start;
	}
	
	public int getEnd() {
		return end;
	}

	@Override
	public LongIterator iterator() {
		return new LongIterator.ArrayIterator(array, start, end);
	}
	
	public long set(int index, long element) {
		array[start+index] = element;
		return element;
	}
	
	public long get(int index) {
		return array[start+index];
	}
	
	public long setInt(int index, long element) {
		array[start+index] = element;
		return element;
	}
	
	public long getLong(int index) {
		return array[start+index];
	}

	@Override
	public int size() {
		return end-start;
	}

	@Override
	public boolean contains(long o) {
		for (int i=start; i<end; i++)
			if (array[i]==o) return true;
		return false;
	}

	@Override
	public long[] toLongArray() {
		long[] re = new long[size()];
		System.arraycopy(array, start, re, 0, re.length);
		return re;
	}

	@Override
	public boolean add(long e) {
		 throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsAll(LongCollection c) {
		LongIterator it = c.iterator();
		while (it.hasNext())
			if (!contains(it.nextLong()))
				return false;
		return true;
	}

	@Override
	public boolean addAll(LongCollection c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(LongCollection c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(LongCollection c) {
		throw new UnsupportedOperationException();
	}

}
