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
