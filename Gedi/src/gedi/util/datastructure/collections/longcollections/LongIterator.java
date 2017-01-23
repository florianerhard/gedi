package gedi.util.datastructure.collections.longcollections;

import java.util.Iterator;

public interface LongIterator extends Iterator<Long> {

	public long nextLong();

	public static class EmptyLongIterator implements LongIterator {
		@Override
		public long nextLong() {
			return -1;
		}

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public Long next() {
			return null;
		}

		@Override
		public void remove() {
		}
		
	}
	
	public static class ArrayIterator implements LongIterator {
		private int next;
		private long[] a;
		private int end;
		
		public ArrayIterator(long[] a) {
			this(a,0,a.length);
		}
		public ArrayIterator(long[] a,int start, int end) {
			this.a = a;
			this.end = end;
			this.next = start;
		}

		@Override
		public boolean hasNext() {
			return next<end;
		}

		@Override
		public Long next() {
			return a[next++];
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public long nextLong() {
			return a[next++];
		}

	}
}
