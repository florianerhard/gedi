package gedi.core.region.utils;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Forwards all calls to put to the underlying collection, ignoring the value.
 * @author erhard
 *
 * @param <K>
 * @param <V>
 */
public class CollectionToMap<K,V> implements Map<K,V>{

	private Collection<K> collection;

	public CollectionToMap(Collection<K> collection) {
		this.collection = collection;
	}

	@Override
	public int size() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEmpty() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsKey(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V get(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public V put(K key, V value) {
		collection.add(key);
		return null;
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
		throw new UnsupportedOperationException();
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
