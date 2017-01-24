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


public class MutableQuintuple<T1, T2, T3, T4, T5> implements Comparable<MutableQuintuple<T1,T2,T3,T4,T5>>, Mutable {
	
	
	public T1 Item1;
	public T2 Item2;
	public T3 Item3;
	public T4 Item4;
	public T5 Item5;
	
	public MutableQuintuple() {
		this(null,null,null,null,null);
	}
	public MutableQuintuple(T1 i1, T2 i2, T3 i3, T4 i4, T5 i5) {
		Item1 = i1;
		Item2 = i2;
		Item3 = i3;
		Item4 = i4;
		Item5 = i5;
	}
	
	public MutableQuintuple<T1,T2,T3,T4,T5> set(T1 i1, T2 i2, T3 i3, T4 i4, T5 i5) {
		Item1 = i1;
		Item2 = i2;
		Item3 = i3;
		Item4 = i4;
		Item5 = i5;
		return this;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(String.valueOf(Item1)+",");
		sb.append(String.valueOf(Item2)+",");
		sb.append(String.valueOf(Item3)+",");
		sb.append(String.valueOf(Item4)+",");
		sb.append(String.valueOf(Item5));
		sb.append("]");
		return sb.toString();
	}
	
	@Override
	public int hashCode() {
		int re = 0;
		if (Item1!=null)
			re+=Item1.hashCode();
		if (Item2!=null)
			re=31*re+Item2.hashCode();
		if (Item3!=null)
			re=31*re+Item3.hashCode();
		if (Item4!=null)
			re=31*re+Item4.hashCode();
		if (Item5!=null)
			re=31*re+Item5.hashCode();
		return re;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object obj) {
		if (obj==this) return true;
		else if (obj instanceof MutableQuintuple) {
			MutableQuintuple p = (MutableQuintuple) obj;
			
			boolean re = true;
			re &= GeneralUtils.isEqual(p.Item1,Item1);
			re &= GeneralUtils.isEqual(p.Item2,Item2);
			re &= GeneralUtils.isEqual(p.Item3,Item3);
			re &= GeneralUtils.isEqual(p.Item4,Item4);
			re &= GeneralUtils.isEqual(p.Item5,Item5);
			return re;
		}
		else return false;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(MutableQuintuple<T1,T2,T3,T4,T5> o) {
		int re = 0;
		if (Item1 instanceof Comparable)
			re = ((Comparable<T1>)Item1).compareTo(o.Item1);
		if (re!=0) return re;
		if (Item2 instanceof Comparable)
			re = ((Comparable<T2>)Item2).compareTo(o.Item2);
		if (re!=0) return re;
		if (Item3 instanceof Comparable)
			re = ((Comparable<T3>)Item3).compareTo(o.Item3);
		if (re!=0) return re;
		if (Item4 instanceof Comparable)
			re = ((Comparable<T4>)Item4).compareTo(o.Item4);
		if (re!=0) return re;
		if (Item5 instanceof Comparable)
			re = ((Comparable<T5>)Item5).compareTo(o.Item5);
		return re;
	}
	
	public MutableQuintuple<T1,T2,T3,T4,T5> clone() {
		return new MutableQuintuple<T1,T2,T3,T4,T5>(Item1,Item2,Item3,Item4,Item5);
	}
	

	@Override
	public int size() {
		return 5;
	}
	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(int index) {
		if (index==0) return (T)Item1;
		if (index==1) return (T)Item2;
		if (index==2) return (T)Item3;
		if (index==3) return (T)Item4;
		if (index==4) return (T)Item5;
		throw new IndexOutOfBoundsException();
	}
	
	@Override
	public <T> T set(int index, T ele) {
		if (index==0) {
			T re = (T)Item1;
			Item1 = (T1)ele;
			return re;
		}
		if (index==1) {
			T re = (T)Item2;
			Item2 = (T2)ele;
			return re;
		}
		if (index==2) {
			T re = (T)Item3;
			Item3 = (T3)ele;
			return re;
		}
		if (index==2) {
			T re = (T)Item3;
			Item3 = (T3)ele;
			return re;
		}
		if (index==3) {
			T re = (T)Item4;
			Item4 = (T4)ele;
			return re;
		}
		if (index==4) {
			T re = (T)Item5;
			Item5 = (T5)ele;
			return re;
		}
		throw new IndexOutOfBoundsException();
	}
}
