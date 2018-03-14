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
package gedi.plot.scale.transformer;

import java.util.HashMap;

import gedi.util.ArrayUtils;

public interface DiscreteInvertibleTransformer<T> {

	T transform(int v);
	int inverse(T v);
	
	public static <T> DiscreteInvertibleTransformer<T> array(T[] a) {
		HashMap<T, Integer> index = ArrayUtils.createIndexMap(a);
		return new DiscreteInvertibleTransformer<T>() {
			@Override
			public T transform(int v) {
				return a[v];
			}

			@Override
			public int inverse(T v) {
				return index.get(v);
			}
		};
	};

	

}
