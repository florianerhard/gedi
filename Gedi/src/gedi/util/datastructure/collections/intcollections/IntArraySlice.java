package gedi.util.datastructure.collections.intcollections;

/**
 * Mutable w.r.t. to position (not to array)
 * @author erhard
 *
 */
public class IntArraySlice extends AbstractIntCollection {

	private int[] array;
	private int start;
	private int end;
	
	/**
	 * Array is not copied!
	 * @param array
	 * @param start
	 * @param end
	 */
	public IntArraySlice(int[] array, int start, int end) {
		this.array = array;
		this.start = start;
		this.end = end;
	}
	
	public IntArraySlice setSlice(int start,int end) {
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
	public IntIterator iterator() {
		return new IntIterator.ArrayIterator(array, start, end);
	}
	
	public int set(int index, int element) {
		array[start+index] = element;
		return element;
	}
	
	public int get(int index) {
		return array[start+index];
	}
	
	public int setInt(int index, int element) {
		array[start+index] = element;
		return element;
	}
	
	public int getInt(int index) {
		return array[start+index];
	}

	@Override
	public int size() {
		return end-start;
	}

	@Override
	public boolean contains(int o) {
		for (int i=start; i<end; i++)
			if (array[i]==o) return true;
		return false;
	}

	@Override
	public int[] toIntArray() {
		int[] re = new int[size()];
		System.arraycopy(array, start, re, 0, re.length);
		return re;
	}

	@Override
	public boolean add(int e) {
		 throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsAll(IntCollection c) {
		IntIterator it = c.iterator();
		while (it.hasNext())
			if (!contains(it.nextInt()))
				return false;
		return true;
	}

	@Override
	public boolean addAll(IntCollection c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(IntCollection c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(IntCollection c) {
		throw new UnsupportedOperationException();
	}

}
