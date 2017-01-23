package gedi.util.datastructure.collections.doublecollections;

import java.util.AbstractCollection;
import java.util.Arrays;

public abstract class AbstractDoubleCollection extends AbstractCollection<Double> implements DoubleCollection {

	@Override
	public abstract DoubleIterator iterator();


	@Override
	public boolean contains(double o) {
		DoubleIterator it = iterator();
		while (it.hasNext()) {
			if (o==it.nextDouble()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public double[] toDoubleArray() {
		// Estimate size of array; be prepared to see more or fewer elements
		double[] r = new double[size()];
		DoubleIterator it = iterator();
		for (int i = 0; i < r.length; i++) {
			if (! it.hasNext()) // fewer elements than expected
				return Arrays.copyOf(r, i);
			r[i] = it.nextDouble();
		}
		return it.hasNext() ? finishToArray(r, it) : r;
	}

	private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
	private static double[] finishToArray(double[] r, DoubleIterator it) {
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
			r[i++] = it.nextDouble();
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
	public boolean add(double e) {
		return add(new Double(e));
	}

	@Override
	public boolean remove(double o) {
		DoubleIterator it = iterator();
		while (it.hasNext()) {
			if (o==it.nextDouble()) {
				it.remove();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean containsAll(DoubleCollection c) {
		DoubleIterator it = c.iterator();
		while (it.hasNext()) {
			if (!contains(it.nextDouble()))
				return false;
		}
		return true;
	}

	@Override
	public boolean addAll(DoubleCollection c) {
		boolean modified = false;
		DoubleIterator it = c.iterator();
		while(it.hasNext())
			if (add(it.nextDouble()))
				modified = true;
		return modified;
	}

	@Override
	public boolean removeAll(DoubleCollection c) {
		boolean modified = false;
		DoubleIterator it = iterator();
		while (it.hasNext()) {
			if (c.contains(it.nextDouble())) {
				it.remove();
				modified = true;
			}
		}
		return modified;
	}

	@Override
	public boolean retainAll(DoubleCollection c) {
		boolean modified = false;
		DoubleIterator it = iterator();
		while (it.hasNext()) {
			if (!c.contains(it.nextDouble())) {
				it.remove();
				modified = true;
			}
		}
		return modified;
	}

}
