package gedi.util.mutable;

import gedi.util.GeneralUtils;

import java.io.Serializable;
import java.util.Arrays;


public class MaskedMutableTriple<T1, T2, T3> extends MutableTriple<T1, T2, T3> {
	
	private static final int FULL_MASK = (1<<3)-1;
	private int mask;
	
	public MaskedMutableTriple() {
		this(null,null,null,FULL_MASK);
	}
	public MaskedMutableTriple(T1 i1, T2 i2, T3 i3) {
		this(i1,i2,i3,FULL_MASK);
	}
	public MaskedMutableTriple(T1 i1, T2 i2, T3 i3, int mask) {
		super(i1, i2, i3);
		this.mask = mask&FULL_MASK;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		if ((mask&1)==1)
			sb.append(String.valueOf(Item1)+",");
		if ((mask&2)==2)
			sb.append(String.valueOf(Item2)+",");
		if ((mask&4)==4)
			sb.append(String.valueOf(Item3)+",");
		if (sb.length()==1)
			return super.toString();
		sb.deleteCharAt(sb.length()-1);
		sb.append("]");
		return sb.toString();
	}
	
	@Override
	public int hashCode() {
		if (mask==0)
			return 0;
		int re = 0;
		if ((mask&1)==1 && Item1!=null)
			re+=Item1.hashCode();
		if ((mask&2)==2 && Item2!=null)
			re=31*re+Item2.hashCode();
		if ((mask&4)==4 && Item3!=null)
			re=31*re+Item3.hashCode();
		return re;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object obj) {
		if (obj==this) return true;
		else if (obj instanceof MaskedMutableTriple) {
			MaskedMutableTriple p = (MaskedMutableTriple) obj;
			if (p.mask!=mask) return false;
			if (mask==0) return true;
			
			boolean re = true;
			if ((mask&1)==1)
				re &= GeneralUtils.isEqual(p.Item1,Item1);
			if ((mask&2)==2)
				re &= GeneralUtils.isEqual(p.Item2,Item2);
			if ((mask&4)==4)
				re &= GeneralUtils.isEqual(p.Item3,Item3);
			return re;
		}
		else return false;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(MutableTriple<T1,T2,T3> o) {
		int re = 0;
		if ((mask&1)==1 && Item1 instanceof Comparable)
			re = ((Comparable<T1>)Item1).compareTo(o.Item1);
		if (re!=0) return re;
		if ((mask&2)==2 && Item2 instanceof Comparable)
			re = ((Comparable<T2>)Item2).compareTo(o.Item2);
		if (re!=0) return re;
		if ((mask&4)==4 && Item3 instanceof Comparable)
			re = ((Comparable<T3>)Item3).compareTo(o.Item3);
		return re;
	}
	
	public MaskedMutableTriple<T1,T2,T3> clone() {
		return new MaskedMutableTriple<T1,T2,T3>(Item1,Item2,Item3,mask);
	}
}
