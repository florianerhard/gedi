package gedi.util.math.stat.counting;

import gedi.util.FunctorUtils;
import gedi.util.StringUtils;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;


public class FractionalCounter<T>  {

	private LinkedHashMap<T,double[]> map = new LinkedHashMap<T, double[]>();
	private double[] total;
	private Comparator<T> sorted = null;
	
	private int dim;
	private String[] dimNames = null;
	
	public FractionalCounter() {
		this(1);
	}
	public FractionalCounter(int dim) {
		this.dim = dim;
		this.total = new double[dim];
	}
	public FractionalCounter(String... dimNames) {
		this.dimNames = dimNames;
		this.dim = dimNames.length;
		this.total = new double[dim];
	}
	
	/**
	 * Inits an element (i.e. its count is 0). Do this this if you want a specific reporting order!
	 * Does nothing if the element is already present!
	 * @param o
	 */
	public void init(T o) {
		map.computeIfAbsent(o, k->new double[dim]);
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
	
	/**
	 * The iterator will return elements in sorted order
	 * @return
	 */
	public FractionalCounter<T> sorted() {
		this.sorted = (Comparator<T>)FunctorUtils.naturalComparator();
		return this;
	}
	
	public String getName(int d) {
		return dimNames==null?"Count "+d:dimNames[d];
	}
	/**
	 * The iterator will return elements in sorted order
	 * @return
	 */
	public FractionalCounter<T> sorted(Comparator<T> comparator) {
		this.sorted = comparator;
		return this;
	}
	
	public FractionalCounter<T> sort() {
		return sort((Comparator<T>)FunctorUtils.naturalComparator());
	}
	public FractionalCounter<T> sort(Comparator<T> comp) {
		T[] keys = map.keySet().toArray((T[])new Object[0]);
		Arrays.sort(keys);
		LinkedHashMap<T,double[]> map2 = new LinkedHashMap<T, double[]>();
		for (T k : keys)
			map2.put(k, map.get(k));
		map = map2;
		return this;
	}
	
	/**
	 * Adds a new element to this bag in all dimensions
	 * @param o
	 */
	public boolean add(T o, double count) {
		double[] a = map.computeIfAbsent(o, k->new double[dim]);
		for (int d = 0; d<dim; d++) {
			a[d]+=count;
			total[d]+=count;
		}
		return true;
	}
	
	/**
	 * Adds a new element to this bag in all dimensions greater or equal to d
	 * @param o
	 */
	public boolean addAtLeast(T o, int d, double count) {
		double[] a = map.computeIfAbsent(o, k->new double[dim]);
		for (int d2 = d; d2<dim; d2++) {
			a[d2]+=count;
			total[d2]+=count;
		}
		return true;
	}
	
	/**
	 * Adds a new element to this bag in all dimensions less or equal to d
	 * @param o
	 */
	public boolean addAtMost(T o, int d,double count) {
		double[] a = map.computeIfAbsent(o, k->new double[dim]);
		for (int d2 = 0; d2<=d; d2++) {
			a[d2]+=count;
			total[d2]+=count;
		}
		return true;
	}
	
	
	/**
	 * Adds a new element to this bag
	 * @param o
	 */
	public boolean add(T o, int d, double count) {
		map.computeIfAbsent(o, k->new double[dim])[d]+=count;
		total[d]+=count;
		return true;
	}
	
	
	public double[] get(T o) {
		double[] re = map.get(o);
		if (re==null) return new double[dim];
		return re;
	}
	
	public double get(T o, int d) {
		double[] re = map.get(o);
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
		FractionalCounter<T> other = (FractionalCounter<T>) obj;
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
		sb.append("Element");
		for (int i=0; i<dim; i++)
			sb.append("\t").append(getName(i));
		sb.append("\n");
		
		for (T e : map.keySet())
			sb.append(e.toString()).append("\t").append(StringUtils.concat("\t",map.get(e))).append("\n");
		sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}
	
	public double[] total() {
		return total;
	}
	public boolean contains(T o) {
		return map.containsKey(o);
	}
	public ExtendedIterator<FractionalItemCount<T>> iterator() {
		ExtendedIterator<T> it = EI.wrap(map.keySet());
		if (sorted!=null)
			it = it.sort(sorted);
		return it.map(item->new FractionalItemCount<T>(item,map.get(item)));
	}
	
	
}
