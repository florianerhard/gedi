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
