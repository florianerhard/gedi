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


import gedi.util.ArrayUtils;
import gedi.util.FunctorUtils;
import gedi.util.GeneralUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;

import cern.colt.bitvector.BitVector;

/**
 * Fixed size, typed
 * @author erhard
 *
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class MutableTuple implements Comparable<MutableTuple>, Mutable {
	
	protected Class[] types;
	protected Object[] values;
	protected BitVector set;
	
	
	public MutableTuple(Class... types) {
		this.types = types;
		this.values = new Object[types.length];
		this.set = new BitVector(types.length);
	}
	public MutableTuple(Class[] types, Object[] values) {
		this(types,values,full(values.length));
	}
	private static BitVector full(int length) {
		BitVector re = new BitVector(length);
		re.not();
		return re;
	}
	public MutableTuple(Class[] types, Object[] values, BitVector set) {
		if (types.length!=values.length || types.length!=set.size()) throw new RuntimeException("Sizes do not match!");
		this.types = types;
		this.values = values;
		this.set = set;
	}
	public MutableTuple setQuick(int index, Object v) {
		values[index] = v;
		set.putQuick(index, true);
		return this;
	}
	
	@Override
	public MutableTuple toMutableTuple(Class...types) {
		if (this.types.length!=types.length) throw new IndexOutOfBoundsException();
		for (int i=0; i<types.length; i++)
			if (values[i]!=null && !types[i].isInstance(values[i]))
				throw new ClassCastException();
		return new MutableTuple(types, values, set.copy());
	}
	
	public MutableTuple set(int index, Object v) {
		if (index<0 || index>=types.length) throw new IndexOutOfBoundsException();
		if (v!=null && !types[index].isInstance(v)) throw new ClassCastException("Expected "+types[index].getName()+", got "+v.getClass().getName());
		values[index] = v;
		set.putQuick(index, true);
		return this;
	}
	
	public MutableTuple unset(int index) {
		if (index<0 || index>=types.length) throw new IndexOutOfBoundsException();
		values[index] = null;
		set.putQuick(index, false);
		return this;
	}
	
	public boolean isset(int index) {
		if (index<0 || index>=types.length) throw new IndexOutOfBoundsException();
		return set.getQuick(index);
	}
	
	public MutableTuple set(MutableTuple t) {
		if (t.size()!=size()) throw new IndexOutOfBoundsException();
		if (!Arrays.equals(types, t.types)) throw new ClassCastException();
		System.arraycopy(t.values, 0, values, 0, values.length);
		set.replaceFromToWith(0, set.size()-1, t.set, 0);
		return this;
	}
	
	public MutableTuple concat(MutableTuple o) {
		Class[] types = new Class[size()+o.size()];
		Object[] values = new Object[size()+o.size()];
		BitVector set = new BitVector(size()+o.size());
		
		System.arraycopy(this.types, 0, types, 0, this.types.length);
		System.arraycopy(o.types, 0, types, this.types.length, o.types.length);
		
		System.arraycopy(this.values, 0, values, 0, this.values.length);
		System.arraycopy(o.values, 0, values, this.values.length, o.values.length);
		
		set.replaceFromToWith(0, size()-1, this.set, 0);
		set.replaceFromToWith(size(), set.size()-1, o.set, 0);
		
		return new MutableTuple(types, values, set);
	}

	public <T> T get(int index) {
		return (T) values[index];
	}
	
	public Object[] getArray() {
		return values;
	}
	
	public Class[] getTypes() {
		return types;
	}
	
	public int size() {
		return types.length;
	}
	
	public boolean isComplete() {
		return set.cardinality()==set.size();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i=0; i<size(); i++) {
			if (sb.length()>1) sb.append(",");
			sb.append(String.valueOf(values[i]));
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
			if (values[i]!=null)
				re+=31*re+values[i].hashCode();
		}
		return re;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj==this) return true;
		else if (obj instanceof MutableTuple) {
			MutableTuple p = (MutableTuple) obj;
			
			for (int i=0; i<size(); i++) {
				if (!GeneralUtils.isEqual(p.values[i],values[i]))
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
			if (values[i] instanceof Comparable) {
				re = ((Comparable)values[i]).compareTo(o.values[i]);
				if (re!=0) return re;
			} else if (values[i] instanceof Collection) {
				re = FunctorUtils.compareCollections((Collection)values[i],(Collection)o.values[i]);
				if (re!=0) return re;
			}
		}
		return re;
	}
	
	public MutableTuple clone() {
		return new MutableTuple(types,values.clone(),set.copy());
	}
	
	
	public void clear() {
		Arrays.fill(values, null);
		set.clear();
	}
	
	public static Comparator<MutableTuple> comparator(int component) {
		return (a,b)->((Comparable) a.get(component)).compareTo(b.get(component));
	}
	public static Comparator<MutableTuple> comparator(int...components) {
		Comparator re = (a,b)->0;
		for (int c : components)
			re = re.thenComparing(comparator(c));
		return re;
	}
	
	public MutableTuple restrict(BitVector use) {
		return new MutableTuple(ArrayUtils.restrict(types, use), ArrayUtils.restrict(values, use), ArrayUtils.restrict(set, use));
	}
	
	public MutableTuple exclude(int... components) {
		BitVector bv = new BitVector(size());
		bv.not();
		for (int i: components)
			bv.putQuick(i, false);
		return restrict(bv);
	}
	
	public MutableTuple restrictTo(int... components) {
		BitVector bv = new BitVector(size());
		for (int i: components)
			bv.putQuick(i, true);
		return restrict(bv);
	}
}
