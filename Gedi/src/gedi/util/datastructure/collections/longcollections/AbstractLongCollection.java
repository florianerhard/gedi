package gedi.util.datastructure.collections.longcollections;

import java.util.AbstractCollection;
import java.util.Arrays;

public abstract class AbstractLongCollection extends AbstractCollection<Long> implements LongCollection {

	@Override
	public abstract LongIterator iterator();


	@Override
	public boolean contains(long o) {
		LongIterator it = iterator();
		while (it.hasNext()) {
			if (o==it.nextLong()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public long[] toLongArray() {
		// Estimate size of array; be prepared to see more or fewer elements
		long[] r = new long[size()];
		LongIterator it = iterator();
		for (int i = 0; i < r.length; i++) {
			if (! it.hasNext()) // fewer elements than expected
				return Arrays.copyOf(r, i);
			r[i] = it.nextLong();
		}
		return it.hasNext() ? finishToArray(r, it) : r;
	}

	private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
	private static long[] finishToArray(long[] r, LongIterator it) {
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
			r[i++] = it.nextLong();
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
	public boolean add(long e) {
		return add(new Long(e));
	}

	@Override
	public boolean remove(long o) {
		LongIterator it = iterator();
		while (it.hasNext()) {
			if (o==it.nextLong()) {
				it.remove();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsAll(LongCollection c) {
		LongIterator it = c.iterator();
		while (it.hasNext()) {
			if (!contains(it.nextLong()))
				return false;
		}
		return true;
	}

	@Override
	public boolean addAll(LongCollection c) {
		boolean modified = false;
		LongIterator it = c.iterator();
		while(it.hasNext())
			if (add(it.nextLong()))
				modified = true;
		return modified;
	}

	@Override
	public boolean removeAll(LongCollection c) {
		boolean modified = false;
		LongIterator it = iterator();
		while (it.hasNext()) {
			if (c.contains(it.nextLong())) {
				it.remove();
				modified = true;
			}
		}
		return modified;
	}

	@Override
	public boolean retainAll(LongCollection c) {
		boolean modified = false;
		LongIterator it = iterator();
		while (it.hasNext()) {
			if (!c.contains(it.nextLong())) {
				it.remove();
				modified = true;
			}
		}
		return modified;
	}

}
