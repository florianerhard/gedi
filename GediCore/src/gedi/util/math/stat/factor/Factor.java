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
package gedi.util.math.stat.factor;

import gedi.util.ArrayUtils;
import gedi.util.datastructure.array.IntegerArray;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.functions.EI;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.WeakHashMap;
import java.util.function.UnaryOperator;

public class Factor implements Comparable<Factor> {
	
	
	private String[] names;
	private HashMap<String,Integer> nameToIndex;

	Factor below;
	Factor above;
	Factor[] levels;
	int index;
	
	private Factor() {}
	
	Factor(Factor proto, int index, Factor below, Factor above) {
		this.names = proto.names;
		this.index = index;
		this.nameToIndex = proto.nameToIndex;
		this.levels = proto.levels;
		if (index>=0 && index<levels.length)
			this.levels[index] = this;
		this.above = above;
		this.below = below;
	}
	
	public int getIndex(String name) {
		return nameToIndex.get(name);
	}
	
	public Factor get(String name) {
		return levels[nameToIndex.get(name)];
	}
	
	public Factor get(int index) {
		if (index<0) return below;
		if (index>=levels.length) return above;
		return levels[index];
	}
	
	public int getIndex() {
		return index;
	}
	
	public int length() {
		return levels.length;
	}
	
	public String[] getNames() {
		return names;
	}
	
	public Factor[] getLevels() {
		return levels;
	}
	
	public String name() {
		if (index<0) 
			return "<";
		if (index>=names.length) 
			return ">";
		return names[index];
	}
	
	@Override
	public int compareTo(Factor o) {
		if (o.levels!=levels) throw new RuntimeException("Do not compare different factors!");
		return Integer.compare(this.index, o.index);
	}
	
	@Override
	public int hashCode() {
		return Integer.hashCode(index);
	}
	
	@Override
	public boolean equals(Object obj) {
		return this==obj;
	}
	
	@Override
	public String toString() {
		return name();
	}

	/**
	 * Creates a factor for the given names
	 * @param names
	 * @return
	 */
	public static Factor create(String... names) {
		return create(names,null,t->t);
	}
	
	/**
	 * Creates a factor for the given names
	 * @param names
	 * @return
	 */
	public static Factor create(Comparator<String> sorting, String... names) {
		return create(names,sorting,t->t);
	}
	
	/**
	 * Creates a factor for the given names
	 * @param names
	 * @return
	 */
	public static Factor createSorted(String... names) {
		return create(names,(a,b)->a.compareTo(b),t->t);
	}
	
	
	public static Factor create(String[] names, UnaryOperator<Factor> fun) {
		return create(names,null,fun);
	}
	public static Factor create(String[] names, Comparator<String> sorting, UnaryOperator<Factor> fun) {
		HashMap<String,Integer> nameToIndex = ArrayUtils.createIndexMap(names);
		if (sorting!=null) {
			String[] k = nameToIndex.keySet().toArray(new String[0]);
			Arrays.sort(k,sorting);
			for (int i=0; i<k.length; i++)
				nameToIndex.put(k[i], i);
		}
		Factor[] a = new Factor[names.length];
		Factor proto = new Factor();
		proto.names = names;
		proto.nameToIndex = nameToIndex;
		proto.levels = a;
		Factor below = new Factor(proto,-1, null, null);
		Factor above = new Factor(proto,a.length, null, null);
		below.below = above.below = below;
		below.above = above.above = above;
		for (int i=0; i<a.length; i++)
			a[i] = fun.apply(new Factor(proto,i,below, above));
		
		return a[0];
	}
	
	public static Factor FALSE = create("false","true");
	public static Factor TRUE = FALSE.get(1);

	/**
	 * Converts a list of strings into a list of factors
	 * @param list
	 * @return
	 */
	public static List<Factor> fromStrings(Collection<String> list) {
		Factor proto = create(new LinkedHashSet<>(list).toArray(new String[0]));
		return EI.wrap(list).map(proto::get).toCollection(new ArrayList<>());
	}
	
	public static Factor[] fromStrings(String[] list) {
		Factor proto = create(list);
		return EI.wrap(list).map(proto::get).toArray(Factor.class);
	}
	
	public static Factor[] fromStringsSorted(String[] list) {
		Factor proto = createSorted(list);
		return EI.wrap(list).map(proto::get).toArray(Factor.class);
	}

	
	private static WeakHashMap<NumericArray, Factor> numericArrayCache = new WeakHashMap<>();
	public static Factor fromNumericArray(NumericArray array) {
		return numericArrayCache.computeIfAbsent(array, Factor::createNumericArrayFactor);
	}

	private static Factor createNumericArrayFactor(NumericArray array) {
		if (array.isIntegral()) return create(array.intIterator().sort().map(i->i+"").toArray(String.class));
		return create(array.doubleIterator().sort().map(i->i+"").unique(true).toArray(String.class));
	}

	public static void serialize(BinaryWriter out, Factor[] data) throws IOException {
		if (data.length==0) {
			out.putCInt(data.length);
			return;
		}
		
		out.putCInt(data.length);
		Factor p = data[0];
		out.putCInt(p.names.length);
		for (String s : p.names)
			out.putString(s);
		for (Factor f : data)
			out.putCInt(f.getIndex());
	}

	public static Factor[] deserialize(BinaryReader in) throws IOException {
		Factor[] re = new Factor[in.getCInt()];
		String[] names = new String[in.getCInt()];
		for (int i = 0; i < names.length; i++) 
			names[i] = in.getString();
		Factor p = create(names);
		for (int i = 0; i < re.length; i++) 
			re[i] = p.get(in.getCInt());
		return re;
	}
	
	
	
}
