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

import java.io.Serializable;
import java.util.Arrays;


public class MaskedMutableSextuple<T1, T2, T3, T4, T5, T6> extends MutableSextuple<T1, T2, T3, T4, T5, T6> {
	
	private static final int FULL_MASK = (1<<6)-1;
	
	private int mask;
	
	public MaskedMutableSextuple() {
		this(null,null,null,null,null,null,FULL_MASK);
	}
	public MaskedMutableSextuple(T1 i1, T2 i2, T3 i3, T4 i4, T5 i5, T6 i6) {
		this(i1,i2,i3,i4,i5,i6,FULL_MASK);
	}
	public MaskedMutableSextuple(T1 i1, T2 i2, T3 i3, T4 i4, T5 i5, T6 i6, int mask) {
		super(i1, i2, i3, i4, i5, i6);
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
		return re;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object obj) {
		if (obj==this) return true;
		else if (obj instanceof MaskedMutableSextuple) {
			MaskedMutableSextuple p = (MaskedMutableSextuple) obj;
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
			return re;
		}
		else return false;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public int compareTo(MutableSextuple<T1,T2,T3,T4,T5,T6> o) {
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
		return re;
	}
	
	public MaskedMutableSextuple<T1,T2,T3,T4,T5,T6> clone() {
		return new MaskedMutableSextuple<T1,T2,T3,T4,T5,T6>(Item1,Item2,Item3,Item4,Item5,Item6,mask);
	}
}
