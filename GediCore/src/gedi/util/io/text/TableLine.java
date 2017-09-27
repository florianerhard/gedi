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

package gedi.util.io.text;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


public class TableLine<T> implements Map<Integer,T>{

	private T[] values;
	private HeaderLine nameIndex; 
	
	public TableLine(T[] values, HeaderLine nameIndex) {
		this.values = values;
		this.nameIndex = nameIndex;
	}
	
	@Override
	public int size() {
		return values.length;
	}

	@Override
	public boolean isEmpty() {
		return values.length==0;
	}

	@Override
	public boolean containsKey(Object key) {
		if (key instanceof String) {
			Integer ind = nameIndex.apply((String)key);
			if (ind==null || ind<0 || ind>=values.length)
				return false;
			return true;
		}
		else if (key instanceof Integer) {
			Integer ind = (Integer) key; 
			if (ind==null || ind<0 || ind>=values.length)
				return false;
			return true;
		}
		return false;
	}

	@Override
	public boolean containsValue(Object value) {
		for (int i=0; i<values.length; i++)
			if (values[i].equals(value))
				return true;
		return false;
	}
	
	
	public <V> V getObject(Object key) {
		return (V) get(key);
	}
	
	public String getString(Object key) {
		return get(key).toString();
	}
	
	public int getInt(Object key) {
		T re = get(key);
		if (re instanceof Number) return ((Number)re).intValue();
		return Integer.parseInt(re.toString());
	}

	public double getDouble(Object key) {
		T re = get(key);
		if (re instanceof Number) return ((Number)re).doubleValue();
		return Double.parseDouble(re.toString());
	}

	@Override
	public T get(Object key) {
		if (key instanceof String) {
			Integer ind = nameIndex.apply((String)key);
			if (ind!=null && ind>=0 && ind<values.length)
				return values[ind];
			return null;
		}
		else if (key instanceof Integer) {
			Integer ind = (Integer) key; 
			if (ind!=null && ind>=0 && ind<values.length)
				return values[ind];
			return null;
		}
		return null;
	}

	@Override
	public T put(Integer key, T value) {
		if (key!=null && key>=0 && key<values.length) {
			T re = values[key];
			values[key] = value;
			return re;
		}
		throw new IllegalArgumentException(key+" not allowed!");
	}
	
	public T put(String key, T value) {
		Integer ind = nameIndex.apply((String)key);
		if (ind!=null && ind>=0 && ind<values.length) {
			T re = values[ind];
			values[ind] = value;
			return re;
		}
		throw new IllegalArgumentException(key+" not allowed!");
	}

	@Override
	public T remove(Object key) {
		if (key instanceof String) 
			return put((String) key,null);
		else if (key instanceof Integer) 
			return put((Integer) key,null);
		return null;
	}

	@Override
	public void putAll(Map<? extends Integer, ? extends T> m) {
		for (Integer k : m.keySet())
			put(k,m.get(k));
	}
	
	public void putAll(T[] array) {
		values = array;
	}

	@Override
	public void clear() {
		Arrays.fill(values, null);
	}

	@Override
	public Set<Integer> keySet() {
		return new KeySet();
	}

	@Override
	public Collection<T> values() {
		return new ValueSet();
	}

	@Override
	public Set<java.util.Map.Entry<Integer, T>> entrySet() {
		return new EntrySet();
	}
	
	/**
     * Compares the specified object with this map for equality.  Returns
     * <tt>true</tt> if the given object is also a map and the two maps
     * represent the same mappings.  More formally, two maps <tt>m1</tt> and
     * <tt>m2</tt> represent the same mappings if
     * <tt>m1.entrySet().equals(m2.entrySet())</tt>.  This ensures that the
     * <tt>equals</tt> method works properly across different implementations
     * of the <tt>Map</tt> interface.
     *
     * <p>This implementation first checks if the specified object is this map;
     * if so it returns <tt>true</tt>.  Then, it checks if the specified
     * object is a map whose size is identical to the size of this map; if
     * not, it returns <tt>false</tt>.  If so, it iterates over this map's
     * <tt>entrySet</tt> collection, and checks that the specified map
     * contains each mapping that this map contains.  If the specified map
     * fails to contain such a mapping, <tt>false</tt> is returned.  If the
     * iteration completes, <tt>true</tt> is returned.
     *
     * @param o object to be compared for equality with this map
     * @return <tt>true</tt> if the specified object is equal to this map
     */
    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (!(o instanceof Map))
            return false;
        Map<Integer,String> m = (Map<Integer,String>) o;
        if (m.size() != size())
            return false;

        try {
            Iterator<java.util.Map.Entry<Integer,T>> i = entrySet().iterator();
            while (i.hasNext()) {
            	java.util.Map.Entry<Integer,T> e = i.next();
                Integer key = e.getKey();
                T value = e.getValue();
                if (value == null) {
                    if (!(m.get(key)==null && m.containsKey(key)))
                        return false;
                } else {
                    if (!value.equals(m.get(key)))
                        return false;
                }
            }
        } catch (ClassCastException unused) {
            return false;
        } catch (NullPointerException unused) {
            return false;
        }

        return true;
    }

    /**
     * Returns the hash code value for this map.  The hash code of a map is
     * defined to be the sum of the hash codes of each entry in the map's
     * <tt>entrySet()</tt> view.  This ensures that <tt>m1.equals(m2)</tt>
     * implies that <tt>m1.hashCode()==m2.hashCode()</tt> for any two maps
     * <tt>m1</tt> and <tt>m2</tt>, as required by the general contract of
     * {@link Object#hashCode}.
     *
     * <p>This implementation iterates over <tt>entrySet()</tt>, calling
     * {@link Map.Entry#hashCode hashCode()} on each element (entry) in the
     * set, and adding up the results.
     *
     * @return the hash code value for this map
     * @see Map.Entry#hashCode()
     * @see Object#equals(Object)
     * @see Set#equals(Object)
     */
    public int hashCode() {
        int h = 0;
        Iterator<java.util.Map.Entry<Integer,T>> i = entrySet().iterator();
        while (i.hasNext())
            h += i.next().hashCode();
        return h;
    }

    /**
     * Returns a string representation of this map.  The string representation
     * consists of a list of key-value mappings in the order returned by the
     * map's <tt>entrySet</tt> view's iterator, enclosed in braces
     * (<tt>"{}"</tt>).  Adjacent mappings are separated by the characters
     * <tt>", "</tt> (comma and space).  Each key-value mapping is rendered as
     * the key followed by an equals sign (<tt>"="</tt>) followed by the
     * associated value.  Keys and values are converted to strings as by
     * {@link String#valueOf(Object)}.
     *
     * @return a string representation of this map
     */
    public String toString() {
        Iterator<java.util.Map.Entry<Integer,T>> i = entrySet().iterator();
        if (! i.hasNext())
            return "{}";

        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (;;) {
        	java.util.Map.Entry<Integer,T> e = i.next();
            Integer key = e.getKey();
            T value = e.getValue();
            sb.append(nameIndex.get(key));
            sb.append('=');
            sb.append(value == this ? "(this Map)" : value);
            if (! i.hasNext())
                return sb.append('}').toString();
            sb.append(',').append(' ');
        }
    }

	private final class KeySet extends AbstractSet<Integer> {
        public Iterator<Integer> iterator() {
            return new KeyIterator();
        }
        public int size() {
            return values.length;
        }
        public boolean contains(Object o) {
            return containsKey(o);
        }
        public boolean remove(Object o) {
            return TableLine.this.remove(o) != null;
        }
        public void clear() {
            TableLine.this.clear();
        }
    }
	
	private final class ValueSet extends AbstractSet<T> {
        public Iterator<T> iterator() {
            return new ValueIterator();
        }
        public int size() {
            return values.length;
        }
        public boolean contains(Object o) {
            return containsValue(o);
        }
        public boolean remove(Object o) {
        	for (int i=0; i<values.length; i++)
    			if (values[i].equals(o)) {
    				values[i] = null;
    				return true;
    			}
            return false;
        }
        public void clear() {
            TableLine.this.clear();
        }
    }
	
	private final class EntrySet extends AbstractSet<java.util.Map.Entry<Integer, T>> {
        public Iterator<java.util.Map.Entry<Integer, T>> iterator() {
            return new EntryIterator();
        }
        public int size() {
            return values.length;
        }
        public boolean contains(Object o) {
            return containsValue(o);
        }
        public boolean remove(Object o) {
        	return TableLine.this.remove(((Entry)o).key)!=null;
        }
        public void clear() {
            TableLine.this.clear();
        }
    }
	
	
	private class IteratorBase {
		protected int index;
		public boolean hasNext() {
			return index<values.length;
		}
		public void remove() {
			if (index-1>=0 && index-1<values.length)
				values[index-1] = null;
			else throw new IllegalArgumentException();
		}
	}
	private final class KeyIterator extends IteratorBase implements Iterator<Integer> {
		@Override
		public Integer next() {
			return index++;
		}
	}
	private final class ValueIterator extends IteratorBase implements Iterator<T> {
		@Override
		public T next() {
			return values[index++];
		}
	}
	private final class EntryIterator extends IteratorBase implements Iterator<java.util.Map.Entry<Integer, T>> {
		@Override
		public Entry next() {
			return new Entry(index++);
		}
	}
	
	class Entry implements Map.Entry<Integer,T> {
        final int key;

        /**
         * Creates new entry.
         */
        Entry(int k) {
            key = k;
        }

        public final Integer getKey() {
            return key;
        }

        public final T getValue() {
            return values[key];
        }

        public final T setValue(T newValue) {
            T oldValue = values[key];
            values[key] = newValue;
            return oldValue;
        }

        public final boolean equals(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry e = (Map.Entry)o;
            Object k1 = getKey();
            Object k2 = e.getKey();
            if (k1 == k2 || (k1 != null && k1.equals(k2))) {
                Object v1 = getValue();
                Object v2 = e.getValue();
                if (v1 == v2 || (v1 != null && v1.equals(v2)))
                    return true;
            }
            return false;
        }

        public final int hashCode() {
            return key;
        }

        public final String toString() {
            return getKey() + "=" + getValue();
        }
    }
}
