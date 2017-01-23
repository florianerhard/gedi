package gedi.util.mutable;

import gedi.util.GeneralUtils;

import java.io.Serializable;
import java.util.Arrays;


public class MaskedMutableMonad<T1> extends MutableMonad<T1> {
	
	private static final int FULL_MASK = (1<<1)-1;
	
	private int mask;
	
	public MaskedMutableMonad() {
		this(null,FULL_MASK);
	}
	public MaskedMutableMonad(T1 item) {
		this(item,FULL_MASK);
	}
	public MaskedMutableMonad(T1 item, int mask) {
		super(item);
		this.mask = mask&FULL_MASK;
	}
	
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		if ((mask&1)==1)
			sb.append(Item.toString()+",");
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
		if ((mask&1)==1 && Item!=null)
			re+=Item.hashCode();
		return re;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object obj) {
		if (obj==this) return true;
		else if (obj instanceof MaskedMutableMonad) {
			MaskedMutableMonad p = (MaskedMutableMonad) obj;
			if (p.mask!=mask) return false;
			if (mask==0) return true;
			
			boolean re = true;
			if ((mask&1)==1)
				re &= GeneralUtils.isEqual(p.Item,Item);
			return re;
		}
		else return false;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(MutableMonad<T1> o) {
		int re = 0;
		if ((mask&1)==1 && Item instanceof Comparable)
			re = ((Comparable<T1>)Item).compareTo(o.Item);
		return re;
	}
	
	public MaskedMutableMonad<T1> clone() {
		return new MaskedMutableMonad<T1>(Item,mask);
	}
}
