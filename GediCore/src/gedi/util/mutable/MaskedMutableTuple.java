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
