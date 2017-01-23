package gedi.util.datastructure.collections.intcollections;

import gedi.util.FunctorUtils.FilteredIntIterator;
import gedi.util.FunctorUtils.PeekIntIterator;
import gedi.util.functions.ExtendedIterator;

import java.util.function.IntBinaryOperator;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

public interface IntIterator extends ExtendedIterator<Integer> {
	
	
	default IntIterator filterInt(IntPredicate predicate) {
		return new FilteredIntIterator(this,predicate);
	}
	
	default PeekIntIterator peekInt() {
		return new PeekIntIterator(this);
	}

	default Integer next() {
		return nextInt();
	}
	
	public int nextInt();

	public static class EmptyIntIterator implements IntIterator {
		@Override
		public int nextInt() {
			return -1;
		}

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public Integer next() {
			return null;
		}

		@Override
		public void remove() {
		}
		
	}
	
	public static class ArrayIterator implements IntIterator {
		private int next;
		private int[] a;
		private int end;
		
		public ArrayIterator(int[] a) {
			this(a,0,a.length);
		}
		public ArrayIterator(int[] a,int start, int end) {
			this.a = a;
			this.end = end;
			this.next = start;
		}

		@Override
		public boolean hasNext() {
			return next<end;
		}

		@Override
		public Integer next() {
			return a[next++];
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int nextInt() {
			return a[next++];
		}

	}
	
	
	default int sum() {
		int  s = 0;
		while(hasNext())
			s+=nextInt();
		return s;
	}
	
	default int max() {
		return stat(Integer.MIN_VALUE,Math::max);
	}
	
	default int min() {
		return stat(Integer.MAX_VALUE,Math::min);
	}
	
	default int absmax() {
		return stat(0,(a,b)->Math.abs(a)>Math.abs(b)?a:b);
	}
	
	default int absmin() {
		return stat(Integer.MAX_VALUE,(a,b)->Math.abs(a)<Math.abs(b)?a:b);
	}

	
	default int stat(int init, IntBinaryOperator op) {
		int s = init;
		while(hasNext())
			s=op.applyAsInt(s, nextInt());
		return s;
	}
	
}
