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

import java.util.AbstractCollection;
import java.util.Arrays;

public abstract class AbstractIntCollection extends AbstractCollection<Integer> implements IntCollection {

	@Override
	public abstract IntIterator iterator();


	@Override
	public boolean contains(int o) {
		IntIterator it = iterator();
		while (it.hasNext()) {
			if (o==it.nextInt()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int[] toIntArray() {
		// Estimate size of array; be prepared to see more or fewer elements
		int[] r = new int[size()];
		IntIterator it = iterator();
		for (int i = 0; i < r.length; i++) {
			if (! it.hasNext()) // fewer elements than expected
				return Arrays.copyOf(r, i);
			r[i] = it.nextInt();
		}
		return it.hasNext() ? finishToArray(r, it) : r;
	}

	private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
	private static int[] finishToArray(int[] r, IntIterator it) {
		int i = r.length;
		while (it.hasNext()) {
			int cap = r.length;
			if (i == cap) {
				int newCap = cap + (cap >> 1) + 1;
				// overflow-conscious code
				if (newCap - MAX_ARRAY_SIZE > 0)
					newCap = hugeCapacity(cap + 1);
				r = Arrays.copyOf(r, newCap);
			}
			r[i++] = it.nextInt();
		}
		// trim if overallocated
		return (i == r.length) ? r : Arrays.copyOf(r, i);
	}

	private static int hugeCapacity(int minCapacity) {
		if (minCapacity < 0) // overflow
			throw new OutOfMemoryError
			("Required array size too large");
		return (minCapacity > MAX_ARRAY_SIZE) ?
				Integer.MAX_VALUE :
					MAX_ARRAY_SIZE;
	}

	@Override
	public boolean add(int e) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(int o) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean add(Integer e) {
		return add(e.intValue());
	}

	
	@Override
	public boolean remove(Object e) {
		if (e instanceof Integer)
			return remove(((Integer) e).intValue());
		return false;
	}
	
	
	@Override
	public boolean contains(Object e) {
		if (e instanceof Integer)
			return contains(((Integer) e).intValue());
		return false;
	}

	@Override
	public boolean containsAll(IntCollection c) {
		IntIterator it = c.iterator();
		while (it.hasNext()) {
			if (!contains(it.nextInt()))
				return false;
		}
		return true;
	}

	@Override
	public boolean addAll(IntCollection c) {
		boolean modified = false;
		IntIterator it = c.iterator();
		while(it.hasNext())
			if (add(it.nextInt()))
				modified = true;
		return modified;
	}

	@Override
	public boolean removeAll(IntCollection c) {
		boolean modified = false;
		IntIterator it = iterator();
		while (it.hasNext()) {
			if (c.contains(it.nextInt())) {
				it.remove();
				modified = true;
			}
		}
		return modified;
	}

	@Override
	public boolean retainAll(IntCollection c) {
		boolean modified = false;
		IntIterator it = iterator();
		while (it.hasNext()) {
			if (!c.contains(it.nextInt())) {
				it.remove();
				modified = true;
			}
		}
		return modified;
	}

}
