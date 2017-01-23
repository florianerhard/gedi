package gedi.util.datastructure.collections.bitcollections;

import cern.colt.bitvector.BitVector;
import gedi.util.functions.ExtendedIterator;

public interface BitIterator extends ExtendedIterator<Boolean> {

	default Boolean next() {
		return nextBoolean();
	}
	
	
	public boolean nextBoolean();

	public static class EmptyBitIterator implements BitIterator {
		@Override
		public boolean nextBoolean() {
			return false;
		}

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public Boolean next() {
			return null;
		}

		@Override
		public void remove() {
		}
		
	}
	
	public static class ArrayIterator implements BitIterator {
		private int next;
		private boolean[] a;
		private int end;
		
		public ArrayIterator(boolean[] a) {
			this(a,0,a.length);
		}
		public ArrayIterator(boolean[] a,int start, int end) {
			this.a = a;
			this.end = end;
			this.next = start;
		}

		@Override
		public boolean hasNext() {
			return next<end;
		}

		@Override
		public Boolean next() {
			return a[next++];
		}

		@Override
		public boolean nextBoolean() {
			return a[next++];
		}

	}
	
	public static class BitVectorIterator implements BitIterator {
		private int next;
		private BitVector a;
		private int end;
		
		public BitVectorIterator(BitVector a) {
			this(a,0,a.size());
		}
		public BitVectorIterator(BitVector a,int start, int end) {
			this.a = a;
			this.end = end;
			this.next = start;
		}

		@Override
		public boolean hasNext() {
			return next<end;
		}

		@Override
		public Boolean next() {
			return a.getQuick(next++);
		}

		@Override
		public boolean nextBoolean() {
			return a.getQuick(next++);
		}

	}
}
