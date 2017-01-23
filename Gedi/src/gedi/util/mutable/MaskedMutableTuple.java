package gedi.util.mutable;


import gedi.util.GeneralUtils;

import java.util.Arrays;

import cern.colt.bitvector.BitVector;

/**
 * Fixed size, typed
 * @author erhard
 *
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class MaskedMutableTuple extends MutableTuple {
	
	private BitVector mask;
	
	
	public MaskedMutableTuple(Class[] types) {
		super(types);
		this.mask = new BitVector(types.length);
		this.mask.not();
	}
	public MaskedMutableTuple(Class[] types, BitVector mask) {
		super(types);
		if (types.length!=mask.size()) throw new RuntimeException("Sizes do not match!");
		this.mask = mask;
	}
	public MaskedMutableTuple(Class[] types, Object[] values, BitVector set,  BitVector mask) {
		super(types,values, set);
		if (types.length!=mask.size() || types.length!=values.length) throw new RuntimeException("Sizes do not match!");
		this.mask = mask;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i=0; i<size(); i++) {
			if (mask.get(i)) {
				if (sb.length()>1) sb.append(",");
				sb.append(String.valueOf(values[i]));
			}
		}
		if (sb.length()==1)
			return super.toString();
		sb.append("]");
		return sb.toString();
	}
	
	@Override
	public int hashCode() {
		int re = 0;
		for (int i=0; i<size(); i++) {
			if (mask.get(i)) {
				if (mask.get(i) && values[i]!=null)
					re+=31*re+values[i].hashCode();
			}
		}
		return re;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj==this) return true;
		else if (obj instanceof MutableOctuple) {
			MaskedMutableTuple p = (MaskedMutableTuple) obj;
			if (!p.mask.equals(mask)) return false;
			
			for (int i=0; i<size(); i++) {
				if (mask.get(i) && !GeneralUtils.isEqual(p.values[i],values[i]))
					return false;	
			}
			return true;
		}
		else return false;
	}
	
	@Override
	public int compareTo(MutableTuple o) {
		int re = 0;
		for (int i=0; i<size(); i++) {
			if (mask.get(i) && values[i] instanceof Comparable) {
				re = ((Comparable)values[i]).compareTo(o.values[i]);
				if (re!=0) return re;
			}
		}
		return re;
	}
	
	public MaskedMutableTuple clone() {
		return new MaskedMutableTuple(types,values.clone(),set.copy(),mask);
	}
	
	
	public void clear() {
		Arrays.fill(values, null);
		set.clear();
	}
}
