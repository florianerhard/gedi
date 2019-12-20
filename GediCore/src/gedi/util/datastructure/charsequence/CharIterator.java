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
package gedi.util.datastructure.charsequence;

import gedi.util.FunctorUtils.FilteredIntIterator;
import gedi.util.FunctorUtils.PeekIntIterator;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.functions.ExtendedIterator;

import java.util.function.IntBinaryOperator;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

public interface CharIterator extends ExtendedIterator<Character> {
	
	
	default Character next() {
		return nextChar();
	}
	
	public char nextChar();
	

	public static class EmptyCharIterator implements CharIterator {
		@Override
		public char nextChar() {
			return 0;
		}

		@Override
		public boolean hasNext() {
			return false;
		}

		@Override
		public Character next() {
			return null;
		}

		@Override
		public void remove() {
		}
		
	}
	
	public static class ArrayIterator implements CharIterator {
		private int next;
		private char[] a;
		private int end;
		
		public ArrayIterator(char[] a) {
			this(a,0,a.length);
		}
		public ArrayIterator(char[] a,int start, int end) {
			this.a = a;
			this.end = end;
			this.next = start;
		}

		@Override
		public boolean hasNext() {
			return next<end;
		}

		@Override
		public Character next() {
			return a[next++];
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public char nextChar() {
			return a[next++];
		}

	}
	
	public static class ReverseArrayIterator implements CharIterator {
		private int next;
		private char[] a;
		private int end;
		
		public ReverseArrayIterator(char[] a) {
			this(a,0,a.length);
		}
		public ReverseArrayIterator(char[] a,int start, int end) {
			this.a = a;
			this.end = start-1;
			this.next = end-1;
		}

		@Override
		public boolean hasNext() {
			return next>end;
		}

		@Override
		public Character next() {
			return a[next--];
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public char nextChar() {
			return a[next--];
		}

	}
	
	public static CharSequenceIterator fromCharSequence(CharSequence s) {
		return new CharSequenceIterator(s);
	}
	
	public static class CharSequenceIterator implements CharIterator {
		private int next;
		private CharSequence s;
		
		public CharSequenceIterator(CharSequence s) {
			this.s = s;
			this.next = 0;
		}

		@Override
		public boolean hasNext() {
			return next<s.length();
		}

		@Override
		public Character next() {
			return s.charAt(next++);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public char nextChar() {
			return s.charAt(next++);
		}

	}
	
}
