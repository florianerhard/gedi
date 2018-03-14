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

import gedi.util.FunctorUtils;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class FunctionMap<K,V> implements Map<K, V> {

	private Set<K> keys;
	private Function<K,V> fun;
	
	@Override
	public int size() {
		return keys.size();
	}
	@Override
	public boolean isEmpty() {
		return keys.isEmpty();
	}
	@Override
	public boolean containsKey(Object key) {
		return keys.contains(key);
	}
	@Override
	public boolean containsValue(Object value) {
		for (K k : keys)
			if (fun.apply(k).equals(value)) return true;
		return false;
	}
	@Override
	public V get(Object key) {
		return fun.apply((K) key);
	}
	@Override
	public V put(K key, V value) {
		throw new UnsupportedOperationException();
	}
	@Override
	public V remove(Object key) {
		throw new UnsupportedOperationException();
	}
	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		throw new UnsupportedOperationException();
	}
	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}
	@Override
	public Set<K> keySet() {
		return keys;
	}
	@Override
	public Collection<V> values() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		throw new UnsupportedOperationException();
	}
	
	
	
}
