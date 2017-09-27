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

package gedi.util.datastructure.collections.bitcollections;

import java.util.AbstractCollection;
import java.util.Arrays;

import cern.colt.bitvector.BitVector;

public abstract class AbstractBitCollection extends AbstractCollection<Boolean> implements BitCollection {

	@Override
	public abstract BitIterator iterator();


	@Override
	public boolean contains(boolean o) {
		BitIterator it = iterator();
		while (it.hasNext()) {
			if (o==it.nextBoolean()) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public BitVector toBitVector() {
		// Estimate size of array; be prepared to see more or fewer elements
		BitVector r = new BitVector(size());
		BitIterator it = iterator();
		for (int i = 0; i < r.size(); i++) {
			if (! it.hasNext()) {// fewer elements than expected
				r.setSize(i);
				return r;
			}
				
			r.putQuick(i, it.nextBoolean());
		}
		return it.hasNext() ? finishToBitVector(r, it) : r;
	}
	
	@Override
	public boolean[] toBooleanArray() {
		// Estimate size of array; be prepared to see more or fewer elements
		boolean[] r = new boolean[size()];
		BitIterator it = iterator();
		for (int i = 0; i < r.length; i++) {
			if (! it.hasNext()) // fewer elements than expected
				return Arrays.copyOf(r, i);
			r[i] = it.nextBoolean();
		}
		return it.hasNext() ? finishToArray(r, it) : r;
	}

	private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
	private static boolean[] finishToArray(boolean[] r, BitIterator it) {
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
			r[i++] = it.nextBoolean();
		}
		// trim if overallocated
		return (i == r.length) ? r : Arrays.copyOf(r, i);
	}

	private static BitVector finishToBitVector(BitVector r, BitIterator it) {
		int i = r.size();
		while (it.hasNext()) {
			int cap = r.size();
			if (i == cap) {
				int newCap = cap + (cap >> 1) + 1;
				// overflow-conscious code
				if (newCap - MAX_ARRAY_SIZE > 0)
					newCap = hugeCapacity(cap + 1);
				r.setSize(newCap);
			}
			r.putQuick(i++, it.nextBoolean());
		}
		// trim if overallocated
		if (i<r.size())
			r.setSize(i);
		return r;
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
	public boolean add(boolean e) {
		return add(new Boolean(e));
	}

	@Override
	public boolean remove(boolean o) {
		BitIterator it = iterator();
		while (it.hasNext()) {
			if (o==it.nextBoolean()) {
				it.remove();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsAll(BitCollection c) {
		BitIterator it = c.iterator();
		while (it.hasNext()) {
			if (!contains(it.nextBoolean()))
				return false;
		}
		return true;
	}

	@Override
	public boolean addAll(BitCollection c) {
		boolean modified = false;
		BitIterator it = c.iterator();
		while(it.hasNext())
			if (add(it.nextBoolean()))
				modified = true;
		return modified;
	}

	@Override
	public boolean removeAll(BitCollection c) {
		boolean modified = false;
		BitIterator it = iterator();
		while (it.hasNext()) {
			if (c.contains(it.nextBoolean())) {
				it.remove();
				modified = true;
			}
		}
		return modified;
	}

	@Override
	public boolean retainAll(BitCollection c) {
		boolean modified = false;
		BitIterator it = iterator();
		while (it.hasNext()) {
			if (!c.contains(it.nextBoolean())) {
				it.remove();
				modified = true;
			}
		}
		return modified;
	}

}
