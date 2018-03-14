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

import gedi.util.GeneralUtils;

import java.util.List;


/**
 * List semantics are as follows: If Item=null, the list is empty; it has a single entry otherwise. Adding another element will throw an exception.
 * @author erhard
 *
 * @param <T1>
 */
public class MutableMonad<T1> implements Comparable<MutableMonad<T1>>, Mutable {
	
	
	public T1 Item;
	
	public MutableMonad() {
		this(null);
	}
	public MutableMonad(T1 item) {
		Item = item;
	}
	
	public MutableMonad<T1> set(T1 i1) {
		Item = i1;
		return this;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(String.valueOf(Item));
		sb.append("]");
		return sb.toString();
	}
	
	@Override
	public int hashCode() {
		int re = 0;
		if (Item!=null)
			re+=Item.hashCode();
		return re;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object obj) {
		if (obj==this) return true;
		else if (obj instanceof MutableMonad) {
			MutableMonad p = (MutableMonad) obj;
			boolean re = true;
			re &= GeneralUtils.isEqual(p.Item,Item);
			return re;
		}
		else return false;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(MutableMonad<T1> o) {
		int re = 0;
		if (Item instanceof Comparable)
			re = ((Comparable<T1>)Item).compareTo(o.Item);
		return re;
	}
	
	public MutableMonad<T1> clone() {
		return new MutableMonad<T1>(Item);
	}
	@Override
	public int size() {
		return 1;
	}
	@Override
	public T1 get(int index) {
		if (index!=0) throw new IndexOutOfBoundsException();
		return Item;
	}
	@Override
	public <T> T set(int index, T ele) {
		if (index==0) {
			T re = (T)Item;
			Item = (T1)ele;
			return re;
		}
		throw new IndexOutOfBoundsException();
	}
	public List<T1> asMonadList() {
		return (List)asList();
	}
	
	
}
