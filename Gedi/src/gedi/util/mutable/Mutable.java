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

package gedi.util.mutable;

import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Function;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public interface Mutable {

	
	/**
	 * Converts this mutable to a MutableTuple with the given classes. A {@link ClassCastException} is thrown if this is not possible!
	 * @param types
	 * @return
	 */
	default MutableTuple toMutableTuple(Class...types) {
		if (types.length!=size()) throw new IndexOutOfBoundsException();
		
		Object[] o = new Object[size()];
		for (int i=0; i<o.length; i++) 
			o[i] = types[i].cast(get(0));
		
		return new MutableTuple(types, o);
	}
	
	/**
	 * Gets the number of not null elements
	 * @return
	 */
	default int count() {
		int re = 0;
		for (int i=0; i<size(); i++)
			if (get(i)!=null)
				re++;
		return re;
	}
	
	/**
	 * Gets the number of slots
	 * @return
	 */
	int size();
	/**
	 * Gets the element in slot index
	 * @param index
	 * @return
	 */
	<T> T get(int index);
	/**
	 * Sets the element in slot index and returns the previous
	 * @param index
	 * @return
	 */
	<T> T set(int index, T ele);
	
	public static <M,I> Class<M> getMutable(Class<I> immutableClass) {
		if (immutableClass==Byte.class) return (Class<M>) MutableByte.class;
		if (immutableClass==Short.class) return (Class<M>) MutableShort.class;
		if (immutableClass==Integer.class) return (Class<M>) MutableInteger.class;
		if (immutableClass==Long.class) return (Class<M>) MutableLong.class;
		if (immutableClass==Float.class) return (Class<M>) MutableFloat.class;
		if (immutableClass==Double.class) return (Class<M>) MutableDouble.class;
		if (immutableClass==String.class) return (Class<M>) MutableString.class;
		return null;
	}
	
	public static <M,I> Function<I,M> getConverter(Class<I> immutableClass) {
		if (immutableClass==Byte.class) return i->(M)new MutableByte((Byte)i);
		if (immutableClass==Short.class) return i->(M)new MutableShort((Short)i);
		if (immutableClass==Integer.class) return i->(M)new MutableInteger((Integer)i);
		if (immutableClass==Long.class) return i->(M)new MutableLong((Long)i);
		if (immutableClass==Float.class) return i->(M)new MutableFloat((Float)i);
		if (immutableClass==Double.class) return i->(M)new MutableDouble((Double)i);
		if (immutableClass==String.class) return i->(M)new MutableString((String)i);
		return null;
	}
	
	/**
	 * A view on this as a list; adding elements will put them into the next free slot, if there is none, an exception will be thrown
	 * Every operation on the list that involves an index (e.g. get, remove,...) operates on the Mutable position!
	 * @return
	 */
	default List<Object> asList() {
		return asList(Object.class);
	}
	
		
	/**
	 * A view on this as a list; adding elements will put them into the next free slot, if there is none, an exception will be thrown
	 * Every operation on the list that involves an index (e.g. get, remove,...) operates on the Mutable position!
	 * @return
	 */
	default <T> List<T> asList(Class<T> cls) {
		return new List<T>() {
			
			@Override
			public T get(int index) {
				return Mutable.this.get(index);
			}
			
			public int size() {
				return count();
			}
			@Override
			public boolean isEmpty() {
				return size()==0;
			}
			@Override
			public boolean contains(Object o) {
				if (o==null) return false;
				for (int i=0; i<size(); i++)
					if (o.equals(get(i)))
						return true;
				return false;
			}
			@Override
			public ExtendedIterator<T> iterator() {
				return EI.wrap(size(), i->(T)get(i)).removeNulls();
			}
			@Override
			public Object[] toArray() {
				return iterator().toArray();
			}
			@Override
			public <C> C[] toArray(C[] a) {
				if (a.length!=size()) 
					a = (C[]) Array.newInstance(a.getClass().getComponentType(), size());
				return (C[])iterator().toArray((T[])a);
			}
			@Override
			public boolean add(T e) {
				for (int i=0; i<Mutable.this.size(); i++)
					if (Mutable.this.get(i)==null) {
						Mutable.this.set(i,e);
						return true;
					}
				throw new IllegalArgumentException("Elements already set when adding element "+e+": "+toString());
			}
			
			@Override
			public String toString() {
				return "["+iterator().concat(",")+"]";
			}
			@Override
			public boolean remove(Object o) {
				if (o==null) return false;
				for (int i=0; i<Mutable.this.size(); i++)
					if (o.equals(Mutable.this.get(i))) {
						Mutable.this.set(i,null);
						return true;
					}
				return false;
			}
			@Override
			public boolean containsAll(Collection<?> c) {
				for (Object o : c)
					if (!contains(o))
						return false;
				return true;
			}
			@Override
			public boolean addAll(Collection<? extends T> c) {
				for (T p : c)
					add(p);
				return true;
			}
			@Override
			public boolean addAll(int index, Collection<? extends T> c) {
				return addAll(c);
			}
			@Override
			public boolean removeAll(Collection<?> c) {
				boolean re = false;
				for (Object o : c)
					re|=remove(o);
				return re;
			}
			@Override
			public boolean retainAll(Collection<?> c) {
				boolean changed = false;
				for (int i=0; i<Mutable.this.size(); i++)
					if (c.contains(Mutable.this.get(i))) {
						Mutable.this.set(i, null);
						changed = true;
					}
				return changed;
			}
			@Override
			public void clear() {
				for (int i=0; i<Mutable.this.size(); i++)
					Mutable.this.set(i, null);
			}
			
			@Override
			public T set(int index, T element) {
				return Mutable.this.set(index, element);
			}
			@Override
			public void add(int index, T element) {
				set(index,element);
			}
			@Override
			public T remove(int index) {
				return set(index,null);
			}
			@Override
			public int indexOf(Object o) {
				if (o==null) return -1;
				for (int i=0; i<Mutable.this.size(); i++)
					if (o.equals(Mutable.this.get(i)))
						return i;
				return -1;
			}
			@Override
			public int lastIndexOf(Object o) {
				if (o==null) return -1;
				for (int i=Mutable.this.size()-1; i>=0; i--)
					if (o.equals(Mutable.this.get(i)))
						return i;
				return -1;
			}
			@Override
			public ListIterator<T> listIterator() {
				throw new NotImplementedException();
			}
			@Override
			public ListIterator<T> listIterator(int index) {
				throw new NotImplementedException();
			}
			@Override
			public List<T> subList(int fromIndex, int toIndex) {
				throw new NotImplementedException();
			}
			
		};
	}
	
}
