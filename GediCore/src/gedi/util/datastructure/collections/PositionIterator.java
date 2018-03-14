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
package gedi.util.datastructure.collections;

import gedi.util.datastructure.collections.intcollections.IntIterator;
import gedi.util.functions.ExtendedIterator;

public interface PositionIterator<T> extends IntIterator {

	T getData();
	
	default T nextData() {
		nextInt();
		return getData();
	}
	
	default ExtendedIterator<T> data() {
		return new ExtendedIterator<T>() {

			@Override
			public boolean hasNext() {
				return PositionIterator.this.hasNext();
			}

			@Override
			public T next() {
				return PositionIterator.this.nextData();
			}
			
		};
	}
	
}
