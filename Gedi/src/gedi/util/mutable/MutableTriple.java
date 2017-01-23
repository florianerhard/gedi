package gedi.util.mutable;

import gedi.util.GeneralUtils;


public class MutableTriple<T1, T2, T3> implements Comparable<MutableTriple<T1,T2,T3>>, Mutable {
	
	
	public T1 Item1;
	public T2 Item2;
	public T3 Item3;
	
	public MutableTriple() {
		this(null,null,null);
	}
	public MutableTriple(T1 i1, T2 i2, T3 i3) {
		Item1 = i1;
		Item2 = i2;
		Item3 = i3;
	}
	
	public MutableTriple<T1,T2,T3> set(T1 i1, T2 i2, T3 i3) {
		Item1 = i1;
		Item2 = i2;
		Item3 = i3;
		return this;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(String.valueOf(Item1)+",");
		sb.append(String.valueOf(Item2)+",");
		sb.append(String.valueOf(Item3));
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
		return re;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object obj) {
		if (obj==this) return true;
		else if (obj instanceof MutableTriple) {
			MutableTriple p = (MutableTriple) obj;
			boolean re = true;
			re &= GeneralUtils.isEqual(p.Item1,Item1);
			re &= GeneralUtils.isEqual(p.Item2,Item2);
			re &= GeneralUtils.isEqual(p.Item3,Item3);
			return re;
		}
		else return false;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(MutableTriple<T1,T2,T3> o) {
		int re = 0;
		if (Item1 instanceof Comparable)
			re = ((Comparable<T1>)Item1).compareTo(o.Item1);
		if (re!=0) return re;
		if (Item2 instanceof Comparable)
			re = ((Comparable<T2>)Item2).compareTo(o.Item2);
		if (re!=0) return re;
		if (Item3 instanceof Comparable)
			re = ((Comparable<T3>)Item3).compareTo(o.Item3);
		return re;
	}
	
	public MutableTriple<T1,T2,T3> clone() {
		return new MutableTriple<T1,T2,T3>(Item1,Item2,Item3);
	}
	
	@Override
	public int size() {
		return 3;
	}
	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(int index) {
		if (index==0) return (T)Item1;
		if (index==1) return (T)Item2;
		if (index==2) return (T)Item3;
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
		throw new IndexOutOfBoundsException();
	}
	
}
