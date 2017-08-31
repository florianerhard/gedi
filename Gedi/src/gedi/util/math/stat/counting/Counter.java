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

package gedi.util.math.stat.counting;

import gedi.util.ArrayUtils;
import gedi.util.FunctorUtils;
import gedi.util.ReflectionUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.dataframe.DataColumn;
import gedi.util.datastructure.dataframe.DataFrame;
import gedi.util.datastructure.dataframe.DataFrameBuilder;
import gedi.util.datastructure.dataframe.IntegerDataColumn;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.math.stat.binning.IntegerBinning;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;


public class Counter<T>  {

	private LinkedHashMap<T,int[]> map = new LinkedHashMap<T, int[]>();
	private int[] total;
	
	private int dim;
	private String[] dimNames = null;
	private String elementName;
	
	public Counter() {
		this("Element",1);
	}
	public Counter(String elementName, int dim) {
		this.dim = dim;
		this.elementName = elementName;
		this.total = new int[dim];
	}
	public Counter(String elementName, String... dimNames) {
		this.dimNames = dimNames;
		this.elementName = elementName;
		this.dim = dimNames.length;
		this.total = new int[dim];
	}
	
	public void clear() {
		Arrays.fill(total, 0);
		for (int[] v : map.values())
			Arrays.fill(v, 0);
	}
	
	/**
	 * Inits an element (i.e. its count is 0). Do this this if you want a specific reporting order!
	 * Does nothing if the element is already present!
	 * @param o
	 */
	public void init(T o) {
		map.computeIfAbsent(o, k->new int[dim]);
	}
	
	/**
	 * Inits multiple elements (i.e. its count is 0). Do this this if you want a specific reporting order!
	 * Does nothing if the element is already present!
	 * @param o
	 */
	public void init(Iterator<T> o) {
		while (o.hasNext())
			init(o.next());
	}
	
	
	public String getName(int d) {
		if (dimNames==null && dim==1) return "Count";
		return dimNames==null?"Count "+d:dimNames[d];
	}
	
	public Counter<T> sortByCount() {
		Comparator<int[]> ac = FunctorUtils.intArrayComparator();
		return sort((a,b)->ac.compare(get(a), get(b)));
	}
	public Counter<T> sort() {
		return sort((Comparator<T>)FunctorUtils.naturalComparator());
	}
	public Counter<T> sort(Comparator<T> comp) {
		T[] keys = map.keySet().toArray((T[])new Object[0]);
		Arrays.sort(keys,comp);
		LinkedHashMap<T,int[]> map2 = new LinkedHashMap<T, int[]>();
		for (T k : keys)
			map2.put(k, map.get(k));
		map = map2;
		return this;
	}
	
	public void cum() {
		int[] last = null;
		for (int[] c : map.values()) {
			if (last!=null)
				ArrayUtils.add(c, last);
			last = c;
		}
	}
	
	public Set<T> elements() {
		return map.keySet();
	}
	
	public T first() {
		Map.Entry<T, int[]> head;
		try {
			head = ReflectionUtils.get(map, "head");
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException("Cannot get head from map!");
		}
		return head.getKey();
	}
	
	public T last() {
		Map.Entry<T, int[]> tail;
		try {
			tail = ReflectionUtils.get(map, "tail");
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException("Cannot get tail from map!");
		}
		return tail.getKey();
	}
	
	/**
	 * Adds a new element to this bag in all dimensions
	 * @param o
	 */
	public boolean add(T o) {
		int[] a = map.computeIfAbsent(o, k->new int[dim]);
		for (int d = 0; d<dim; d++) {
			a[d]++;
			total[d]++;
		}
		return true;
	}
	
	/**
	 * Adds a new element to this bag in all dimensions greater or equal to d
	 * @param o
	 */
	public boolean addAtLeast(T o, int d) {
		int[] a = map.computeIfAbsent(o, k->new int[dim]);
		for (int d2 = d; d2<dim; d2++) {
			a[d2]++;
			total[d2]++;
		}
		return true;
	}
	
	/**
	 * Adds a new element to this bag in all dimensions less or equal to d
	 * @param o
	 */
	public boolean addAtMost(T o, int d) {
		int[] a = map.computeIfAbsent(o, k->new int[dim]);
		for (int d2 = 0; d2<=d; d2++) {
			a[d2]++;
			total[d2]++;
		}
		return true;
	}
	
	
	/**
	 * Adds a new element to this bag
	 * @param o
	 */
	public boolean add(T o, int d) {
		map.computeIfAbsent(o, k->new int[dim])[d]++;
		total[d]++;
		return true;
	}
	
	/**
	 * Adds a new element to this bag
	 * @param o
	 */
	public boolean add(T o, int[] d) {
		ArrayUtils.add(map.computeIfAbsent(o, k->new int[dim]),d);
		ArrayUtils.add(total,d);
		return true;
	}
	
	
	public int[] get(T o) {
		int[] re = map.get(o);
		if (re==null) return new int[dim];
		return re;
	}
	
	public int get(T o, int d) {
		int[] re = map.get(o);
		if (re==null) return 0;
		return re[d];
	}
	
	public int getDimensions() {
		return dim;
	}

	public Set<T> getObjects() {
		return map.keySet();
	}
	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((map == null) ? 0 : map.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Counter<T> other = (Counter<T>) obj;
		if (map == null) {
			if (other.map != null)
				return false;
		} else if (!map.equals(other.map))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(elementName);
		for (int i=0; i<dim; i++)
			sb.append("\t").append(getName(i));
		sb.append("\n");
		
		for (T e : map.keySet())
			sb.append(e.toString()).append("\t").append(StringUtils.concat("\t",map.get(e))).append("\n");
		sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}
	
	public String toSingleLine() {
		if (map.isEmpty()) return "";
		
		StringBuilder sb = new StringBuilder();
		for (T e : map.keySet())
			sb.append(e.toString()).append(":").append(StringUtils.concat(",",map.get(e))).append(" ");
		sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}
	
	public int[] total() {
		return total;
	}
	public boolean contains(T o) {
		return map.containsKey(o);
	}
	public ExtendedIterator<ItemCount<T>> iterator() {
		ExtendedIterator<T> it = EI.wrap(map.keySet());
		return it.map(item->new ItemCount<T>(item,map.get(item)));
	}
	
	public DataFrame toDataFrame() {
		DataFrame re = new DataFrame();
		re.add(DataColumn.fromCollectionAsFactor(elementName, EI.wrap(map.keySet()).map(a->StringUtils.toString(a))));
		for (int i=0; i<dim; i++) {
			int ui = i;
			re.add(DataColumn.fromCollection(getName(i), EI.wrap(map.keySet()).mapToInt(a->map.get(a)[ui])));
		}
		return re;
	}
	public String getElementName() {
		return elementName;
	}
	public <K> Counter<K> bin(Function<T,K> binner) {
		Counter<K> re = new Counter<>(elementName, dim);
		for (T k : map.keySet()) 
			re.add(binner.apply(k), map.get(k));
		return re;
	}
	
	public double entropy(int dim) {
		double sum = 0;
		for (int[] s : map.values()) {
			if (s[dim]>0) {
				double p = s[dim]/(double)total[dim];
				sum+=p*Math.log(p);
			}
		}
		return -sum/Math.log(2);
	}
	
	/**
	 * Just as for sequence logos (2 is max, 0 is no information)
	 * @param dim
	 * @return
	 */
	public double informationContent(int dim) {
		double corr = 1/Math.log(2)*(map.values().size()-1)/(2*total[dim]);
		return Math.log(map.values().size())/Math.log(2)-(entropy(dim)+corr);
	}
	
	public double uncorrectedInformationContent(int dim) {
		return Math.log(map.values().size())/Math.log(2)-(entropy(dim));
	}
	
}
