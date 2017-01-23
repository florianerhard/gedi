package gedi.util.mutable;

import gedi.util.GeneralUtils;

import java.io.Serializable;
import java.util.Arrays;


public class MaskedMutableOctuple<T1, T2, T3, T4, T5, T6, T7, T8> extends MutableOctuple<T1, T2, T3, T4, T5, T6, T7, T8> {
	
	private static final int FULL_MASK = (1<<8)-1;
	
	private int mask;
	
	public MaskedMutableOctuple() {
		this(null,null,null,null,null,null,null,null,FULL_MASK);
	}
	public MaskedMutableOctuple(T1 i1, T2 i2, T3 i3, T4 i4, T5 i5, T6 i6, T7 i7, T8 i8) {
		this(i1,i2,i3,i4,i5,i6,i7,i8,FULL_MASK);
	}
	public MaskedMutableOctuple(T1 i1, T2 i2, T3 i3, T4 i4, T5 i5, T6 i6, T7 i7, T8 i8, int mask) {
		super(i1, i2, i3, i4, i5, i6, i7, i8);
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
		if ((mask&8)==8)
			sb.append(String.valueOf(Item4)+",");
		if ((mask&16)==16)
			sb.append(String.valueOf(Item5)+",");
		if ((mask&32)==32)
			sb.append(String.valueOf(Item6)+",");
		if ((mask&64)==64)
			sb.append(String.valueOf(Item7)+",");
		if ((mask&128)==128)
			sb.append(String.valueOf(Item8)+",");
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
		if ((mask&8)==8 && Item4!=null)
			re=31*re+Item4.hashCode();
		if ((mask&16)==16 && Item5!=null)
			re=31*re+Item5.hashCode();
		if ((mask&32)==32 && Item6!=null)
			re=31*re+Item6.hashCode();
		if ((mask&64)==64 && Item7!=null)
			re=31*re+Item7.hashCode();
		if ((mask&128)==128 && Item8!=null)
			re=31*re+Item8.hashCode();
		return re;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object obj) {
		if (obj==this) return true;
		else if (obj instanceof MaskedMutableOctuple) {
			MaskedMutableOctuple p = (MaskedMutableOctuple) obj;
			if (p.mask!=mask) return false;
			if (mask==0) return true;
			
			boolean re = true;
			if ((mask&1)==1)
				re &= GeneralUtils.isEqual(p.Item1,Item1);
			if ((mask&2)==2)
				re &= GeneralUtils.isEqual(p.Item2,Item2);
			if ((mask&4)==4)
				re &= GeneralUtils.isEqual(p.Item3,Item3);
			if ((mask&8)==8)
				re &= GeneralUtils.isEqual(p.Item4,Item4);
			if ((mask&16)==16)
				re &= GeneralUtils.isEqual(p.Item5,Item5);
			if ((mask&32)==32)
				re &= GeneralUtils.isEqual(p.Item6,Item6);
			if ((mask&64)==64)
				re &= GeneralUtils.isEqual(p.Item7,Item7);
			if ((mask&128)==128)
				re &= GeneralUtils.isEqual(p.Item8,Item8);
			return re;
		}
		else return false;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(MutableOctuple<T1,T2,T3,T4,T5,T6,T7,T8> o) {
		int re = 0;
		if ((mask&1)==1 && Item1 instanceof Comparable)
			re = ((Comparable<T1>)Item1).compareTo(o.Item1);
		if (re!=0) return re;
		if ((mask&2)==2 && Item2 instanceof Comparable)
			re = ((Comparable<T2>)Item2).compareTo(o.Item2);
		if (re!=0) return re;
		if ((mask&4)==4 && Item3 instanceof Comparable)
			re = ((Comparable<T3>)Item3).compareTo(o.Item3);
		if (re!=0) return re;
		if ((mask&8)==8 && Item4 instanceof Comparable)
			re = ((Comparable<T4>)Item4).compareTo(o.Item4);
		if (re!=0) return re;
		if ((mask&16)==16 && Item5 instanceof Comparable)
			re = ((Comparable<T5>)Item5).compareTo(o.Item5);
		if (re!=0) return re;
		if ((mask&32)==32 && Item6 instanceof Comparable)
			re = ((Comparable<T6>)Item6).compareTo(o.Item6);
		if (re!=0) return re;
		if ((mask&64)==64 && Item7 instanceof Comparable)
			re = ((Comparable<T7>)Item7).compareTo(o.Item7);
		if (re!=0) return re;
		if ((mask&128)==128 && Item8 instanceof Comparable)
			re = ((Comparable<T8>)Item8).compareTo(o.Item8);
		return re;
	}
	
	public MaskedMutableOctuple<T1,T2,T3,T4,T5,T6,T7,T8> clone() {
		return new MaskedMutableOctuple<T1,T2,T3,T4,T5,T6,T7,T8>(Item1,Item2,Item3,Item4,Item5,Item6,Item7,Item8,mask);
	}
}
