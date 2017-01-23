package gedi.util;

import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.functions.TriFunction;
import gedi.util.mutable.MutablePair;
import hep.aida.ref.Histogram1D;
import hep.aida.ref.Histogram2D;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.RandomAccess;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiPredicate;
import java.util.function.DoubleBinaryOperator;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Exception;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

import org.apache.commons.math3.distribution.RealDistribution;

import cern.colt.bitvector.BitVector;


public class ArrayUtils {


	public static int find(char[] a,
			char item) {
		for (int i=0; i<a.length; i++)
			if (a[i]==item)
				return i;
		return -1;
	}

	public static int find(int[] a,
			int item) {
		for (int i=0; i<a.length; i++)
			if (a[i]==item)
				return i;
		return -1;
	}

	public static <T> int find(T[] a,
			T item) {
		return find (a,0,a.length, item);
	}

	public static <T> int find(T[] a, int start, int stop, 
			T item) {
		start = Math.max(start,0);
		stop = Math.min(stop,a.length);
		for (int i=start; i<stop; i++)
			if (item.equals(a[i]))
				return i;
		return -1;
	}



	public static <T> int find(T[] a,
			Predicate<T> predicate) {
		return find(a,0,a.length,predicate);
	}


	public static <T> int find(T[] a, int start, int stop, 
			Predicate<T> predicate) {
		start = Math.max(start,0);
		stop = Math.min(stop,a.length);
		for (int i=start; i<stop; i++)
			if (predicate.test(a[i]))
				return i;
		return -1;
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] removeAll(T[] from, T[] remove) {
		Set<T> set = new HashSet<T>(Arrays.asList(remove));
		ArrayList<T> re = new ArrayList<T>();
		for (T f : from)
			if(!set.contains(f))
				re.add(f);
		return re.toArray((T[])Array.newInstance(from.getClass().getComponentType(),re.size()));
	}

	public static int[] invertMap(int[] map) {
		int[] re = new int[max(map)+1];
		Arrays.fill(re,-1);
		for (int i=0; i<map.length; i++)
			re[map[i]] = i;
		return re;
	}

	public static <T> int count(T[] a, T i) {
		int re = 0;
		for (T c : a)
			if (i.equals(c))
				re++;
		return re;
	}
	
	public static int count(boolean[] a, boolean b) {
		int re = 0;
		for (boolean c : a)
			if (c==b)
				re++;
		return re;
	}

	public static <T> int count(T[] a, Predicate<T> pred) {
		int re = 0;
		for (T c : a)
			if (pred.test(c))
				re++;
		return re;
	}

	public static int count(int[] a, int i) {
		int re = 0;
		for (int c : a)
			if (i==c)
				re++;
		return re;
	}

	public static int count(char[] a, char i) {
		int re = 0;
		for (int c : a)
			if (i==c)
				re++;
		return re;
	}

	public static double[] doubleCollectionToPrimitiveArray(Collection<Double> list) {
		double[] re = new double[list.size()];
		int index = 0;
		for (Double i : list)
			re[index++]=i;
		return re;
	}

	public static float[] floatCollectionToPrimitiveArray(Collection<Float> list) {
		float[] re = new float[list.size()];
		int index = 0;
		for (Float i : list)
			re[index++]=i;
		return re;
	}

	public static int[] intCollectionToPrimitiveArray(Collection<Integer> list) {
		int[] re = new int[list.size()];
		int index = 0;
		for (Integer i : list)
			re[index++]=i;
		return re;
	}

	public static long[] longCollectionToPrimitiveArray(Collection<Long> list) {
		long[] re = new long[list.size()];
		int index = 0;
		for (Long i : list)
			re[index++]=i;
		return re;
	}

	@SuppressWarnings("unchecked")
	public static <T,S> MutablePair<T, S>[] createPairArray(T[] a1, S[] a2) {
		MutablePair<T,S>[] re = new MutablePair[Math.max(a1.length, a2.length)];
		for (int i=0; i<re.length; i++)
			re[i] = new MutablePair<T, S>(a1.length>i?a1[i]:null,a2.length>i?a2[i]:null);
		return re;
	}


	@SuppressWarnings("unchecked")
	public static <T,S> MutablePair<T[], S[]> createParallelArrays(MutablePair<T,S>[] a, Class<T> cls1, Class<S> cls2) {
		T[] a1 = (T[]) Array.newInstance(cls1, a.length);
		S[] a2 = (S[]) Array.newInstance(cls2, a.length);

		for (int i=0; i<a.length; i++) {
			a1[i] = a[i].Item1;
			a2[i] = a[i].Item2;
		}

		return new MutablePair<T[], S[]>(a1,a2);
	}

	public static Histogram1D createHistogram(long[] a, int bins) {
		long[] minmax = minmax(a);
		Histogram1D re = new Histogram1D(null,bins,minmax[0],minmax[1]+1);
		for (long i : a)
			re.fill(i);
		return re;
	}

	public static Histogram1D createHistogram(int[] a, int bins) {
		int[] minmax = minmax(a);
		Histogram1D re = new Histogram1D(null,bins,minmax[0],minmax[1]+1);
		for (int i : a)
			re.fill(i);
		return re;
	}



	public static int argmax(int[] a) {
		int re = Integer.MIN_VALUE;
		int arg = -1;
		for (int i=0; i<a.length; i++) {
			if (a[i]>re) {
				re = a[i];
				arg = i;
			}
		}
		return arg;
	}

	public static int argmin(int[] a) {
		int re = Integer.MAX_VALUE;
		int arg = -1;
		for (int i=0; i<a.length; i++) {
			if (a[i]<re) {
				re = a[i];
				arg = i;
			}
		}
		return arg;
	}

	public static int argmin(float[] a) {
		float re = Float.POSITIVE_INFINITY;
		int arg = -1;
		for (int i=0; i<a.length; i++) {
			if (a[i]<re) {
				re = a[i];
				arg = i;
			}
		}
		return arg;
	}

	public static int max(int[] a) {
		int re = Integer.MIN_VALUE;
		for (int i : a)
			re = Math.max(re, i);
		return re;
	}

	public static int max(int[] a, int start, int end) {
		int re = Integer.MIN_VALUE;
		for (int i=start; i<end; i++)
			re = Math.max(re, a[i]);
		return re;
	}

	public static long max(long[] a, int start, int end) {
		long re = Long.MIN_VALUE;
		for (int i=start; i<end; i++)
			re = Math.max(re, a[i]);
		return re;
	}

	public static double max(double[] a, int start, int end) {
		double re = Double.NEGATIVE_INFINITY;
		for (int i=start; i<end; i++)
			re = Math.max(re, a[i]);
		return re;
	}

	public static int max(int[][] m) {
		int re = Integer.MIN_VALUE;
		for (int[] a : m)
			for (int i : a)
				re = Math.max(re, i);
		return re;
	}


	public static float max(float[] a) {
		float re = a[0];
		for (float i : a)
			re = Math.max(re, i);
		return re;
	}


	public static double[] minmax(double[] a) {
		double[] re = {a[0],a[0]};
		for (double i : a) {
			if (i>re[1])
				re[1] = i;
			else if (i<re[0])
				re[0] = i;
		}
		return re;
	}

	public static float[] minmax(float[] a) {
		float[] re = {a[0],a[0]};
		for (float i : a) {
			if (i>re[1])
				re[1] = i;
			else if (i<re[0])
				re[0] = i;
		}
		return re;
	}

	public static int[] minmax(int[] a) {
		int[] re = {Integer.MAX_VALUE,Integer.MIN_VALUE};
		for (int i : a) {
			if (i>re[1])
				re[1] = i;
			else if (i<re[0])
				re[0] = i;
		}
		return re;
	}

	public static long[] minmax(long[] a) {
		long[] re = {Long.MAX_VALUE,Long.MIN_VALUE};
		for (long i : a) {
			if (i>re[1])
				re[1] = i;
			else if (i<re[0])
				re[0] = i;
		}
		return re;
	}

	public static int min(int[] a) {
		int re = Integer.MAX_VALUE;
		for (int i : a)
			re = Math.min(re, i);
		return re;
	}

	public static int sum(int[] a) {
		int re = 0;
		for (int i : a)
			re+=i;
		return re;
	}

	public static double sum(double[][] m) {
		double re = 0;
		for (double[] a : m)
			for (double i : a)
				re+=i;
		return re;
	}

	public static long sum(long[] a) {
		long re = 0;
		for (long i : a)
			re+=i;
		return re;
	}

	public static float sum(float[] a) {
		float re = 0;
		for (float i : a)
			re+=i;
		return re;
	}

	public static float sum(float[] a, int start, int end) {
		float re = 0;
		for (int i=start; i<end; i++)
			re+=a[i];
		return re;
	}


	@SuppressWarnings("unchecked")
	public static <T> T[] concat(T[]... arrays) {
		ArrayList<T> re = new ArrayList<T>();
		for (T[] a : arrays)
			re.addAll(Arrays.asList(a));
		return re.toArray((T[]) Array.newInstance(arrays.getClass().getComponentType().getComponentType(), re.size()));
	}

	public static double[] concat(double[]... arrays) {
		int n = 0;
		for (double[] a : arrays)
			n+=a.length;
		double[] re = new double[n];
		int index = 0;
		for (double[] a : arrays) {
			System.arraycopy(a, 0, re, index, a.length);
			index+=a.length;
		}
		return re;
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] create(T item, int length) {
		return create(()->item,(Class<T>)item.getClass(),length);
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] create(Supplier<T> fac, Class<T> cls, int length) {
		T[] re = (T[]) Array.newInstance(cls, length);
		for (int i=0; i<re.length; i++)
			re[i] = fac.get();
		return re;
	}

	@SuppressWarnings("unchecked")
	public static <I,O> O[] transformArray(I[] array,Function<I,O> transformer, Class<O> cls) {
		O[] re = (O[]) Array.newInstance(cls, array.length);
		for (int i=0; i<re.length; i++)
			re[i] = transformer.apply(array[i]);
		return re;
	}

	public static <T> void swap(T[] a, int i, int j) {
		T temp = a[i];
		a[i] = a[j];
		a[j] = temp;
	}

	public static void swap(char[] a, int i, int j) {
		char temp = a[i];
		a[i] = a[j];
		a[j] = temp;
	}

	public static void swap(int[] a, int i, int j) {
		int temp = a[i];
		a[i] = a[j];
		a[j] = temp;
	}

	/**
	 * Increments the array para if possible. Incrementing an array means that the items
	 * are interpreted as digits of a number with variable radix.
	 * <p>Example with radix = 3,2: (0,0)->(1,0)->(2,0)->(0,1)->(1,1)->(2,1)->false
	 * 
	 * @param para the current number
	 * @param radix the radix array
	 * @return if the array could be incremented
	 */
	public static boolean increment(int[] para, int[] radix) {
		return increment(para,i->radix[i]);
//		int i;
//		for (i=0; i<para.length && para[i]>=radix[i]-1; i++) {
//			para[i]= 0;
//		}
//		if (i<para.length)
//			para[i]++;
//		return i<para.length;
	}

	
	/**
	 * Increments the array para if possible. Incrementing an array means that the items
	 * are interpreted as digits of a number with variable radix.
	 * <p>Example with radix = 3,2: (0,0)->(1,0)->(2,0)->(0,1)->(1,1)->(2,1)->false
	 * 
	 * @param para the current number
	 * @param radix the radix array
	 * @return if the array could be incremented
	 */
	public static boolean increment(int[] para, IntUnaryOperator radix) {
		int i;
		for (i=0; i<para.length && para[i]>=radix.applyAsInt(i)-1; i++) {
			para[i]= 0;
		}
		if (i<para.length)
			para[i]++;
		return i<para.length;
	}

	public static <T> T[] slice(T[] a, int start) {
		return slice(a,start,a.length);
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] slice(T[] a, int start, int end) {
		T[] re = (T[]) Array.newInstance(a.getClass().getComponentType(), end-start);
		System.arraycopy(a, start, re, 0, end-start);
		return re;
	}

	public static float[] slice(float[] a, int start, int end) {
		float[] re = new float[end-start];
		System.arraycopy(a, start, re, 0, end-start);
		return re;
	}

	public static int[] slice(int[] a, int start, int end) {
		int[] re = new int[end-start];
		System.arraycopy(a, start, re, 0, end-start);
		return re;
	}

	public static char[] slice(char[] a, int start, int end) {
		char[] re = new char[end-start];
		System.arraycopy(a, start, re, 0, end-start);
		return re;
	}

	public static byte[] slice(byte[] a, int start, int end) {
		byte[] re = new byte[end-start];
		System.arraycopy(a, start, re, 0, end-start);
		return re;
	}

	public static double[] slice(double[] a, int start, int end) {
		double[] re = new double[end-start];
		System.arraycopy(a, start, re, 0, end-start);
		return re;
	}


	@SuppressWarnings("unchecked")
	public static <T> T[][] deepToArray(Class<T> cls, List<? extends List<? extends T>> list) {
		T[] dummy = (T[]) Array.newInstance(cls, 0);
		T[][] re = (T[][]) Array.newInstance(dummy.getClass(), list.size());
		int index = 0;
		for (List<? extends T> sub : list) {
			T[] inner = (T[]) Array.newInstance(cls, sub.size());
			inner = sub.toArray(inner);
			re[index++] = inner;
		}
		return re;
	}

	/**
	 * Indices have to be sorted!
	 * @param indices
	 * @return
	 */
	public static BitVector createIndexSet(int[] indices) {
		if (indices.length==0)
			return new BitVector(0);

		BitVector bv = new BitVector(indices[indices.length-1]+1);
		for (int index : indices)
			bv.putQuick(index, true);
		return bv;
	}

	public static <T> HashMap<T, Integer> createIndexMap(T[] array) {
		HashMap<T,Integer> re = new HashMap<T, Integer>();
		for (int i=0; i<array.length; i++)
			re.put(array[i], i);
		return re;
	}


	public static <A,K> HashMap<K, A> index(Iterable<A> a, Function<? super A,? extends K> toKey) {
		return index(a.iterator(),toKey,v->v,false);
	}
	public static <A,K> HashMap<K, A> index(Iterable<A> a, Function<? super A,? extends K> toKey, boolean force) {
		return index(a.iterator(),toKey,v->v,force);
	}
	public static <A,K> HashMap<K, A> index(A[] a, Function<? super A,? extends K> toKey) {
		return index(FunctorUtils.arrayIterator(a),toKey,v->v,false);
	}
	public static <A,K> HashMap<K, A> index(A[] a, Function<? super A,? extends K> toKey, boolean force) {
		return index(FunctorUtils.arrayIterator(a),toKey,v->v,force);
	}
	public static <A,K> HashMap<K, A> index(Iterator<A> it, Function<? super A,? extends K> toKey) {
		return index(it,toKey,v->v,false);
	}
	public static <A,K> HashMap<K, A> index(Iterator<A> it, Function<? super A,? extends K> toKey, boolean force) {
		return index(it,toKey,v->v,force);
	}
	public static <A,K,V> HashMap<K, V> index(Iterable<A> a, Function<? super A,? extends K> toKey,Function<? super A,? extends V> toVal) {
		return index(a.iterator(),toKey,toVal,false);
	}
	public static <A,K,V> HashMap<K, V> index(Iterable<A> a, Function<? super A,? extends K> toKey,Function<? super A,? extends V> toVal, boolean force) {
		return index(a.iterator(),toKey,toVal,force);
	}
	public static <A,K,V> HashMap<K, V> index(A[] a, Function<? super A,? extends K> toKey,Function<? super A,? extends V> toVal) {
		return index(FunctorUtils.arrayIterator(a),toKey,toVal,false);
	}
	public static <A,K,V> HashMap<K, V> index(A[] a, Function<? super A,? extends K> toKey,Function<? super A,? extends V> toVal, boolean force) {
		return index(FunctorUtils.arrayIterator(a),toKey,toVal,force);
	}
	public static <A,K,V> HashMap<K, V> index(Iterator<A> it, Function<? super A,? extends K> toKey,Function<? super A,? extends V> toVal) {
		return index(it,toKey,toVal,false);
	}
	public static <A,K,V> HashMap<K, V> index(Iterator<A> it, Function<? super A,? extends K> toKey,Function<? super A,? extends V> toVal, boolean force) {
		HashMap<K,V> re = new HashMap<K, V>();
		while (it.hasNext()) {
			A n = it.next();
			K key = toKey.apply(n);
			V val = toVal.apply(n);
			if (!force && re.containsKey(key))
				throw new RuntimeException("Key "+key+" not unique!");
			re.put(key,val);
		}
		return re;
	}


	public static <A,K,V> HashMap<K, ArrayList<V>> indexMulti(Iterable<A> a, Function<? super A,? extends K> toKey,Function<? super A,? extends V> toVal) {
		return indexMulti(a.iterator(),toKey,toVal,k->new ArrayList<V>());
	}


	public static <A,K,V> HashMap<K, ArrayList<V>> indexMulti(A[] a, Function<? super A,? extends K> toKey,Function<? super A,? extends V> toVal) {
		return indexMulti(FunctorUtils.arrayIterator(a),toKey,toVal,k->new ArrayList<V>());
	}

	public static <A,K,V> HashMap<K, ArrayList<V>> indexMulti(Iterator<A> it, Function<? super A,? extends K> toKey,Function<? super A,? extends V> toVal) {
		return indexMulti(it,toKey,toVal,k->new ArrayList<V>());
	}

	public static <A,K,V,C extends Collection<? super V>> HashMap<K, C> indexMulti(Iterable<A> a, Function<? super A,? extends K> toKey,Function<? super A,? extends V> toVal, Function<? super K,? extends C> multi) {
		return indexMulti(a.iterator(),toKey,toVal,multi);
	}


	public static <A,K,V,C extends Collection<? super V>> HashMap<K, C> indexMulti(A[] a, Function<? super A,? extends K> toKey,Function<? super A,? extends V> toVal, Function<? super K,? extends C> multi) {
		return indexMulti(FunctorUtils.arrayIterator(a),toKey,toVal,multi);
	}

	public static <A,K,V,C extends Collection<? super V>> HashMap<K, C> indexMulti(Iterator<A> it, Function<? super A,? extends K> toKey,Function<? super A,? extends V> toVal, Function<? super K,? extends C> multi) {
		HashMap<K,C> re = new HashMap<K, C>();
		while (it.hasNext()) {
			A n = it.next();
			K key = toKey.apply(n);
			V val = toVal.apply(n);
			re.computeIfAbsent(key, multi).add(val);
		}
		return re;
	}
	
	public static <A,K,V> HashMap<K, V> indexCombine(Iterator<A> it, Function<? super A,? extends K> toKey,Function<? super A,? extends V> toVal, TriFunction<? super K,? super V, ? super V, ? extends V> combiner) {
		HashMap<K,V> re = new HashMap<K, V>();
		while (it.hasNext()) {
			A n = it.next();
			K key = toKey.apply(n);
			V val = toVal.apply(n);
			if (re.containsKey(key)) {
				V p = re.get(key);
				re.put(key,combiner.apply(key,val,p));
			}
			else
				re.put(key,val);
		}
		return re;
	}
	
	public static <A,K,V> HashMap<K, V> indexAdapt(Iterator<A> it, Function<? super A,? extends K> toKey,Function<? super A,? extends V> toVal, TriFunction<Integer,? super A,? super K,? extends K> newKey) {
		HashMap<K,V> re = new HashMap<K, V>();
		while (it.hasNext()) {
			A n = it.next();
			K key = toKey.apply(n);
			V val = toVal.apply(n);
			int ind = 0;
			while (re.containsKey(key)) {
				key = newKey.apply(ind++,n,key);
			}
			re.put(key,val);
		}
		return re;
	}

	public static <A,K,V> HashMap<K, V> indexSmallest(Iterator<A> it, Function<? super A,? extends K> toKey,Function<? super A,? extends V> toVal, Comparator<V> takeSmallest) {
		return indexCombine(it, toKey, toVal, (k,vn,vo)->takeSmallest.compare(vn, vo)<=0?vn:vo);
	}

	public static <T> HashMap<T, Integer> createIndexMap(Collection<T> coll) {
		HashMap<T,Integer> re = new HashMap<T, Integer>();
		int i=0;
		for (T item : coll)
			re.put(item, i++);
		return re;
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] redimPreserve(T[] a, int length) {
		T[] re = (T[]) Array.newInstance(a.getClass().getComponentType(), length);
		System.arraycopy(a, 0, re, 0, Math.min(a.length,re.length));
		return re;
	}

	public static double[] redimPreserve(double[] a, int length) {
		double[] re = new double[length];
		System.arraycopy(a, 0, re, 0, Math.min(a.length,re.length));
		return re;
	}

	public static char[] redimPreserve(char[] a, int length) {
		char[] re = new char[length];
		System.arraycopy(a, 0, re, 0, Math.min(a.length,re.length));
		return re;
	}

	public static float[] redimPreserve(float[] a, int length) {
		float[] re = new float[length];
		System.arraycopy(a, 0, re, 0, Math.min(a.length,re.length));
		return re;
	}

	public static int[] redimPreserve(int[] a, int length) {
		int[] re = new int[length];
		System.arraycopy(a, 0, re, 0, Math.min(a.length,re.length));
		return re;
	}

	public static long[] redimPreserve(long[] a, int length) {
		long[] re = new long[length];
		System.arraycopy(a, 0, re, 0, Math.min(a.length,re.length));
		return re;
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] cast(Object[] a,	Class<T> cls) {
		if (a==null) return (T[]) Array.newInstance(cls, 0);

		T[] re = (T[]) Array.newInstance(cls, a.length);
		for (int i=0; i<a.length; i++)
			re[i] = (T)a[i];
		return re;
	}

	public static <T> void reverse(Collection<T> p) {
		Object[] a = p.toArray();
		reverse(a);
		p.clear();
		for (Object e : a)
			p.add((T) e);
	}

	public static void reverse(double[] p) {
		double tmp;
		int n = p.length-1;
		for (int i=0; i<p.length/2; i++) {
			tmp = p[i];
			p[i] = p[n-i];
			p[n-i] = tmp;
		}
	}

	public static void reverse(short[] p) {
		short tmp;
		int n = p.length-1;
		for (int i=0; i<p.length/2; i++) {
			tmp = p[i];
			p[i] = p[n-i];
			p[n-i] = tmp;
		}
	}

	public static void reverse(long[] p) {
		long tmp;
		int n = p.length-1;
		for (int i=0; i<p.length/2; i++) {
			tmp = p[i];
			p[i] = p[n-i];
			p[n-i] = tmp;
		}
	}

	public static void reverse(char[] p) {
		char tmp;
		int n = p.length-1;
		for (int i=0; i<p.length/2; i++) {
			tmp = p[i];
			p[i] = p[n-i];
			p[n-i] = tmp;
		}
	}

	public static void reverse(float[] p) {
		float tmp;
		int n = p.length-1;
		for (int i=0; i<p.length/2; i++) {
			tmp = p[i];
			p[i] = p[n-i];
			p[n-i] = tmp;
		}
	}

	/**
	 * from inclusice, to exclusive
	 * @param p
	 * @param from
	 * @param to
	 */
	public static void reverse(double[] p, int from, int to) {
		double tmp;
		int l = to-1;
		int k = from;
		for (; k<l; k++, l--) {
			tmp = p[k];
			p[k] = p[l];
			p[l] = tmp;
		}
	}

	/**
	 * from inclusice, to exclusive
	 * @param p
	 * @param from
	 * @param to
	 */
	public static void reverse(int[] p, int from, int to) {
		int tmp;
		int l = to-1;
		int k = from;
		for (; k<l; k++, l--) {
			tmp = p[k];
			p[k] = p[l];
			p[l] = tmp;
		}
	}

	/**
	 * from inclusice, to exclusive
	 * @param p
	 * @param from
	 * @param to
	 */
	public static void reverse(long[] p, int from, int to) {
		long tmp;
		int l = to-1;
		int k = from;
		for (; k<l; k++, l--) {
			tmp = p[k];
			p[k] = p[l];
			p[l] = tmp;
		}
	}

	/**
	 * from inclusice, to exclusive
	 * @param p
	 * @param from
	 * @param to
	 */
	public static void reverse(char[] p, int from, int to) {
		char tmp;
		int l = to-1;
		int k = from;
		for (; k<l; k++, l--) {
			tmp = p[k];
			p[k] = p[l];
			p[l] = tmp;
		}
	}

	/**
	 * from inclusice, to exclusive
	 * @param p
	 * @param from
	 * @param to
	 */
	public static void reverse(float[] p, int from, int to) {
		float tmp;
		int l = to-1;
		int k = from;
		for (; k<l; k++, l--) {
			tmp = p[k];
			p[k] = p[l];
			p[l] = tmp;
		}
	}

	public static double[] seq(double start, double end, double step) {
		double[] re = new double[(int) Math.floor((end-start)/step+1)];
		int index = 0;
		for (double i=start; start<end ? i<=end : i>=end; i+=step)
			re[index++]=i;
		if (index<re.length) re[index] = end;

		return re;
	}

	public static int[] seq(int start, int stop, int step) {
		int[] re = new int[(stop-start+1)/step];
		int index = 0;
		for (int i=start; i<=stop; i+=step)
			re[index++]=i;
		return re;
	}

	public static <T> void shuffleSlice(T[] a, int i, int j) {
		Random rnd = new Random();
		for (int k=i; k<j-1; k++) {
			int m =rnd.nextInt(j-k)+k;
			T tmp = a[k];
			a[k] = a[m];
			a[m] = tmp;
		}
	}

	public static void shuffleSlice(double[] a, int i, int j) {
		Random rnd = new Random();
		for (int k=i; k<j-1; k++) {
			int m =rnd.nextInt(j-k)+k;
			double tmp = a[k];
			a[k] = a[m];
			a[m] = tmp;
		}
	}

	public static void shuffleSlice(int[] a, int i, int j) {
		Random rnd = new Random();
		for (int k=i; k<j-1; k++) {
			int m =rnd.nextInt(j-k)+k;
			int tmp = a[k];
			a[k] = a[m];
			a[m] = tmp;
		}
	}

	public static void shuffleSlice(float[] a, int i, int j) {
		Random rnd = new Random();
		for (int k=i; k<j-1; k++) {
			int m =rnd.nextInt(j-k)+k;
			float tmp = a[k];
			a[k] = a[m];
			a[m] = tmp;
		}
	}

	public static void shuffleSlice(char[] a, int i, int j) {
		Random rnd = new Random();
		for (int k=i; k<j-1; k++) {
			int m =rnd.nextInt(j-k)+k;
			char tmp = a[k];
			a[k] = a[m];
			a[m] = tmp;
		}
	}



	/**
	 * Concatenates an array using {@link Object#toString()} for each item
	 * and glue as separator.
	 * 
	 * @param glue the string that is inserted between each two items
	 * @param array the array
	 * @return concatenation of the array
	 */
	public static String concat(String glue, Object[] array) {
		return concat(glue, array, o->o.toString());
	}

	/**
	 * Concatenates an array using {@link Object#toString()} for each item
	 * and glue as separator.
	 * 
	 * @param glue the string that is inserted between each two items
	 * @param array the array
	 * @return concatenation of the array
	 */
	public static String concat(String glue, Object[] array, int offset, int end) {
		return concat(glue, array, o->o.toString(), offset, end);
	}


	/**
	 * Concatenates an array using {@link Object#toString()} for each item
	 * and glue as separator.
	 * 
	 * @param glue the string that is inserted between each two items
	 * @param array the array
	 * @return concatenation of the array
	 */
	public static <T> String concat(String glue, T[] array, Function<T,String> transformer) {
		return concat(glue, array, transformer, 0, array.length);
	}

	/**
	 * Concatenates an array using {@link Object#toString()} for each item
	 * and glue as separator.
	 * 
	 * @param glue the string that is inserted between each two items
	 * @param array the array
	 * @return concatenation of the array
	 */
	public static <T> String concat(String glue, T[] array, Function<T,String> transformer, int offset, int end) {
		if (array.length==0) return "";
		StringBuffer sb = new StringBuffer();
		for (int i=offset; i<end-1; i++) {
			sb.append(transformer.apply(array[i]));
			sb.append(glue);
		}
		sb.append(transformer.apply(array[end-1]));
		return sb.toString();
	}


	/**
	 * Concatenates an twodimensional array using for each item
	 * and glue as separator.
	 * 
	 * @param glue the string that is inserted between each two items
	 * @param array the array
	 * @return concatenation of the array
	 */
	public static <T> String concatMatrix(String glueLines, String glueColumns, T[][] array) {
		return concatMatrix(glueLines,glueColumns,array,o->o.toString());
	}

	/**
	 * Concatenates an twodimensional array using for each item
	 * and glue as separator.
	 * 
	 * @param glue the string that is inserted between each two items
	 * @param array the array
	 * @return concatenation of the array
	 */
	public static <T> String concatMatrix(String glueLines, String glueColumns, T[][] array, Function<T,String> transformer) {
		if (array.length==0) return "";
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<array.length; i++) {
			if (i>0)sb.append(glueLines);
			for (int j=0; j<array[i].length; j++) {
				if (j>0) sb.append(glueColumns);
				sb.append(transformer.apply(array[i][j]));
			}
		}
		return sb.toString();
	}

	/**
	 * Concatenates an twodimensional array using for each item
	 * and glue as separator.
	 * 
	 * @param glue the string that is inserted between each two items
	 * @param array the array
	 * @return concatenation of the array
	 */
	public static <T> String concatMatrix(String glueLines, String glueColumns, int[][] array) {
		if (array.length==0) return "";
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<array.length; i++) {
			if (i>0)sb.append(glueLines);
			for (int j=0; j<array[i].length; j++) {
				if (j>0) sb.append(glueColumns);
				sb.append(array[i][j]);
			}
		}
		return sb.toString();
	}

	/**
	 * Concatenates an twodimensional array using for each item
	 * and glue as separator.
	 * 
	 * @param glue the string that is inserted between each two items
	 * @param array the array
	 * @return concatenation of the array
	 */
	public static <T> String concatMatrix(String glueLines, String glueColumns, double[][] array) {
		if (array.length==0) return "";
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<array.length; i++) {
			if (i>0)sb.append(glueLines);
			for (int j=0; j<array[i].length; j++) {
				if (j>0) sb.append(glueColumns);
				sb.append(array[i][j]);
			}
		}
		return sb.toString();
	}


	/**
	 * Concatenates an twodimensional array using for each item
	 * and glue as separator.
	 * 
	 * @param glue the string that is inserted between each two items
	 * @param array the array
	 * @return concatenation of the array
	 */
	public static <T> String concatMatrix(String glueLines, String glueColumns, float[][] array) {
		if (array.length==0) return "";
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<array.length; i++) {
			if (i>0)sb.append(glueLines);
			for (int j=0; j<array[i].length; j++) {
				if (j>0) sb.append(glueColumns);
				sb.append(array[i][j]);
			}
		}
		return sb.toString();
	}

	/**
	 * Removes an item from an array and returns a new array.
	 * @param <T> the class
	 * @param array the array
	 * @param index the index of the item to remove
	 * @return the new array without the item
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] removeItemFromArray(T[] array,int index) {
		T[] re = (T[]) Array.newInstance(array.getClass().getComponentType(), Array.getLength(array)-1);
		System.arraycopy(array, 0, re, 0, index);
		System.arraycopy(array, index+1, re, index, re.length-index);
		return re;
	}

	/**
	 * Removes an item from an array and returns a new array.
	 * @param <T> the class
	 * @param array the array
	 * @param index the index of the item to remove
	 * @return the new array without the item
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] removeItemFromArray(T[] array,T item) {
		int index = find(array, item);
		if (index==-1) throw new IllegalArgumentException("Item not in array!");
		T[] re = (T[]) Array.newInstance(array.getClass().getComponentType(), Array.getLength(array)-1);
		System.arraycopy(array, 0, re, 0, index);
		System.arraycopy(array, index+1, re, index, re.length-index);
		return re;
	}

	/**
	 * Removes an item from an array and returns a new array.
	 * @param <T> the class
	 * @param array the array
	 * @param index the index of the item to remove
	 * @return the new array without the item
	 */
	public static int[] removeFromArray(int[] array,int index) {
		if (index==-1) throw new IllegalArgumentException("Item not in array!");
		int[] re = new int[array.length-1];
		System.arraycopy(array, 0, re, 0, index);
		System.arraycopy(array, index+1, re, index, re.length-index);
		return re;
	}

	/**
	 * Inserts an item into an array and returns a new array
	 * @param <T> the class
	 * @param array the array
	 * @param index the index where to insert
	 * @param item the item to insert
	 * @return the new array
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] insertItemToArray(T[] array, int index, T item) {
		T[] re = (T[]) Array.newInstance(array.getClass().getComponentType(), Array.getLength(array)+1);
		System.arraycopy(array, 0, re, 0, index);
		re[index] = item;
		System.arraycopy(array, index, re, index+1, array.length-index);
		return re;
	}

	public static int[] insertToArray(int[] array, int index, int item) {
		int[] re = new int[array.length+1];
		System.arraycopy(array, 0, re, 0, index);
		re[index] = item;
		System.arraycopy(array, index, re, index+1, array.length-index);
		return re;
	}

	/**
	 * Returns a new array with the elements of array and the given length if length>array.length
	 * or else the given array.
	 * @param <T>
	 * @param array
	 * @param length
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] ensureArrayLength(T[] array, int length) {
		if (array.length>=length)
			return array;

		T[] re = (T[]) Array.newInstance(array.getClass().getComponentType(), length);
		System.arraycopy(array, 0, re, 0, array.length);
		return re;
	}


	/**
	 * Returns a new array with the elements of array and the given length if length>array.length
	 * or else the given array.
	 * @param <T>
	 * @param array
	 * @param length
	 * @return
	 */
	public static int[] ensureArrayLength(int[] array, int length) {
		if (array.length>=length)
			return array;

		int[] re = new int[length];
		System.arraycopy(array, 0, re, 0, array.length);
		return re;
	}



	/**
	 * Computes the sum of the given array.
	 * @param array the array
	 * @return the sum
	 */
	public static double sum(double[] array) {
		if (array==null)
			return Double.NaN;
		double re = 0;
		for (double i : array)
			re+=i;
		return re;
	}

	public static double sum(double[] array, int start, int end) {
		if (array==null)
			return Double.NaN;
		double re = 0;
		for (int i=start; i<end; i++)
			re+=array[i];
		return re;
	}


	/**
	 * Computes the product of the given array.
	 * @param array the array
	 * @return the product
	 */
	public static double product(double[] array) {
		double re = 1.0;
		for (double i : array)
			re*=i;
		return re;
	}

	/**
	 * Computes the max of the given array.
	 * @param array the array
	 * @return the max
	 */
	public static double max(double[] array) {
		double re = array[0];
		for (int i=1; i<array.length; i++)
			re = Math.max(re, array[i]);
		return re;
	}

	/**
	 * Computes the min of the given array.
	 * @param array the array
	 * @return the main
	 */
	public static double min(double[] array) {
		double re = array[0];
		for (int i=1; i<array.length; i++)
			re = Math.min(re, array[i]);
		return re;
	}

	/**
	 * Computes the argmax of the given array.
	 * @param array the array
	 * @return the argmax
	 */
	public static int argmax(double[] array) {
		double max = array[0];
		int re = 0;
		for (int i=1; i<array.length; i++) {
			if (array[i]>max) {
				max = array[i];
				re = i;
			}
		}
		return re;
	}

	/**
	 * Computes the argmax of the given array.
	 * @param array the array
	 * @param from start index (inclusive)
	 * @param to end index (exclusive)
	 * @return the argmax
	 */
	public static int argmax(double[] array, int from, int to) {
		if (from>=to) return -1;
		double max = array[from];
		int re = from;
		for (int i=from+1; i<to; i++) {
			if (array[i]>max) {
				max = array[i];
				re = i;
			}
		}
		return re;
	}

	/**
	 * Computes the argmin of the given array.
	 * @param array the array
	 * @param from start index (inclusive)
	 * @param to end index (exclusive)
	 * @return the argmin
	 */
	public static int argmin(double[] array, int from, int to) {
		if (from>=to) return -1;
		double max = array[from];
		int re = from;
		for (int i=from+1; i<to; i++) {
			if (array[i]<max) {
				max = array[i];
				re = i;
			}
		}
		return re;
	}

	/**
	 * Computes the argmin of the given array.
	 * @param array the array
	 * @return the argmin
	 */
	public static int argmin(double[] array) {
		double min = array[0];
		int re = 0;
		for (int i=1; i<array.length; i++) {
			if (array[i]<min) {
				min = array[i];
				re = i;
			}
		}
		return re;
	}


	/**
	 * Computes the argmax of the given array.
	 * @param array the array
	 * @return the argmax
	 */
	public static int argmax(float[] array) {
		float max = array[0];
		int re = 0;
		for (int i=1; i<array.length; i++) {
			if (array[i]>max) {
				max = array[i];
				re = i;
			}
		}
		return re;
	}


	/**
	 * Converts the int array to an IntArrayList.
	 * @param array the array
	 * @return an int array list
	 */
	public static List<Integer> asList(int[] array) {
		return new IntList(array);
	}

	private static class IntList extends AbstractList<Integer>{
		private final int[] array;

		IntList(int[] array){
			this.array = array;
		}

		@Override
		public Integer get(int index) {
			return array[index];
		}

		@Override
		public int size() {
			return array.length;
		}

	}

	public static List<Character> asList(char[] array) {
		return new CharArrayList(array);
	}

	private static class CharArrayList extends AbstractList<Character>{
		private final char[] array;

		CharArrayList(char[] array){
			this.array = array;
		}

		@Override
		public Character get(int index) {
			return array[index];
		}

		@Override
		public int size() {
			return array.length;
		}

	}


	/**
	 * Converts the int array to an IntArrayList.
	 * @param array the array
	 * @return an int array list
	 */
	public static List<Long> asList(long[] array) {
		return new LongArrayList(array);
	}

	private static class LongArrayList extends AbstractList<Long>{
		private final long[] array;

		LongArrayList(long[] array){
			this.array = array;
		}

		@Override
		public Long get(int index) {
			return array[index];
		}

		@Override
		public int size() {
			return array.length;
		}

	}

	/**
	 * Converts the int array to an IntArrayList.
	 * @param array the array
	 * @return an int array list
	 */
	public static List<Float> asList(float[] array) {
		return new FloatArrayList(array);
	}

	private static class FloatArrayList extends AbstractList<Float>{
		private final float[] array;

		FloatArrayList(float[] array){
			this.array = array;
		}

		@Override
		public Float get(int index) {
			return array[index];
		}

		@Override
		public int size() {
			return array.length;
		}

	}

	/**
	 * Converts the int array to an IntArrayList.
	 * @param array the array
	 * @return an int array list
	 */
	public static List<Double> asList(double[] array) {
		return new DoubleArrayList(array);
	}

	private static class DoubleArrayList extends AbstractList<Double>{
		private final double[] array;

		DoubleArrayList(double[] array){
			this.array = array;
		}

		@Override
		public Double get(int index) {
			return array[index];
		}

		@Override
		public int size() {
			return array.length;
		}


	}


	/**
	 * Searches the array from the beginning to the end until the element is found (by equals!).
	 * Returns -1 if the element could not be found.
	 * @param <T>
	 * @param array the array
	 * @param item the item to search for
	 * @return
	 */
	public static <T> int linearSearch(T[] array, T item) {
		for (int i=0; i<array.length; i++)
			if (array[i].equals(item))
				return i;
		return -1;
	}

	/**
	 * Searches the array from the beginning to the end until an element matching 
	 * the predicate is found.
	 * Returns -1 if the element could not be found.
	 * @param <T>
	 * @param array the array
	 * @param item the item to search for
	 * @return
	 */
	public static <T> int linearSearch(T[] array, Predicate<T> predicate) {
		for (int i=0; i<array.length; i++)
			if (predicate.test(array[i]))
				return i;
		return -1;
	}

	/**
	 * Searches the array from the beginning to the end until the element is found (by equals!).
	 * Returns -1 if the element could not be found.
	 * @param <T>
	 * @param array the array
	 * @param item the item to search for
	 * @return
	 */
	public static <T> int linearSearch(T[] array, int start, int end, T item) {
		for (int i=start; i<end; i++)
			if (array[i].equals(item))
				return i;
		return -1;
	}

	/**
	 * Reverses the elements of an array (inplace!)
	 * @param <T>
	 * @param array 
	 * @return the given array for chaining
	 */
	public static <T> T[] reverse(T[] array) {
		for (int i=0; i<array.length/2;i++) {
			T s = array[i];
			array[i] = array[array.length-1-i];
			array[array.length-1-i] = s;
		}
		return array;
	}

	/**
	 * Reverses the elements of an int array
	 * @param array
	 */
	public static void reverse(int[] array) {
		for (int i=0; i<array.length/2;i++) {
			int s = array[i];
			array[i] = array[array.length-1-i];
			array[array.length-1-i] = s;
		}
	}


	/**
	 * Transforms a list to a string representation.
	 * @param <T> the list item type
	 * @param list the list
	 * @return string representation
	 */
	public static <T> String arrayToString(T[] list) {
		return arrayToString(list, o->o.toString());
	}

	/**
	 * Transforms a list to a string representation applying the given stringer.
	 * 
	 * @param <T> the list item type
	 * @param list the list
	 * @param stringer the stringer
	 * @return string representation
	 */
	public static <T> String arrayToString(T[] list, Function<T,String> stringer) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (T i : list) {
			sb.append(stringer.apply(i));
			sb.append(",");
		}
		if (list.length>0)
			sb.deleteCharAt(sb.length()-1);
		sb.append("]");
		return sb.toString();
	}

	/**
	 * Chooses from a matrix an element from each row. Which element is selected is specified
	 * in choice.
	 * 
	 * @param <T>
	 * @param choice
	 * @param array
	 * @param cls
	 * @return the choice
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] choose(int[] choice, T[][] array, Class<T> cls) {
		T[] re = (T[]) Array.newInstance(cls,choice.length);
		for (int i=0; i<re.length; i++)
			re[i] = array[i][choice[i]];
		return re;
	}

	/**
	 * Concatenates arrays and returns a new array.
	 * @param <T> the element type
	 * @param cls the element type
	 * @param arrays the arrays
	 * @return a new array containing the elements of arrays
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] concatArrays(T[]... arrays) {
		int c = 0;
		Collection<Class<?>> classes = new HashSet<Class<?>>();
		for (T[] a : arrays) {
			c+=a.length;
			classes.add(a.getClass().getComponentType());
		}

		T[] re = (T[]) Array.newInstance(ReflectionUtils.getCommonSuperClass(classes), c);
		c=0;
		for (T[] a : arrays) {
			System.arraycopy(a, 0, re, c, a.length);
			c+=a.length;
		}
		return re;
	}




	/**
	 * Helper method for cloning arrays. Migrates the items from src to target and clones them, if deep=true.
	 * This is done by reflectively invoking the clone method on the item. If this fails, an exception is 
	 * thrown if throwExceptionOnUnCloneable=true
	 * <p>
	 * If the lengths of the two arrays are distinct, an exception is thrown! 
	 * @param <T>
	 * @param src
	 * @param target
	 * @param deep
	 * @param throwExceptionOnUnCloneable
	 */
	@SuppressWarnings("unchecked")
	public static <T> void migrateArray(T[] src, T[] target, boolean deep, boolean throwExceptionOnUnCloneable) {
		if (target.length!=src.length)
			throw new RuntimeException("Arrays must have identical size!");
		for (int i=0; i<src.length; i++) {
			T t = src[i];
			T clone = t;
			if (deep) {
				try {
					Method cloneMethod = t.getClass().getMethod("clone");
					clone = (T) cloneMethod.invoke(t);
				} catch (Exception e) {
					if (throwExceptionOnUnCloneable)
						throw new RuntimeException("Could not clone item of a monitored list!",e);
				}
			}
			target[i] = clone;
		}
	}

	public static int[][] cumSum(int[][] a, int xDir, int yDir) {
		int[][] re = new int[a.length][a[0].length];

		if (xDir>0) {
			for (int y=0; y<a[0].length; y++)
				re[0][y] = a[0][y];
			for (int x=1;x<re.length; x++) {
				for (int y=0; y<a[0].length; y++)
					re[x][y] = re[x-1][y]+a[x][y];
			}
		} else {
			for (int y=0; y<a[0].length; y++)
				re[re.length-1][y] = a[re.length-1][y];
			for (int x=re.length-2;x>=0; x--) {
				for (int y=0; y<a[0].length; y++)
					re[x][y] = re[x+1][y]+a[x][y];
			}
		}

		if (yDir>0) {
			for (int x=0;x<re.length; x++) {
				for (int y=1; y<a[0].length; y++)
					re[x][y] += re[x][y-1];
			}
		} else {
			for (int x=0;x<re.length; x++) {
				for (int y=a[0].length-2; y>=0; y--)
					re[x][y] += re[x][y+1];
			}
		}

		return re;
	}

	public static void cumSumInPlace(int[] a, int dir) {
		if (dir>0) {
			for (int i=1; i<a.length; i++)
				a[i] = a[i-1]+a[i];
		} else {
			for (int i=a.length-2; i>=0; i--)
				a[i] = a[i+1]+a[i];
		}
	}

	public static void cumSumInPlace(long[] a, int dir) {
		if (dir>0) {
			for (int i=1; i<a.length; i++)
				a[i] = a[i-1]+a[i];
		} else {
			for (int i=a.length-2; i>=0; i--)
				a[i] = a[i+1]+a[i];
		}
	}

	public static void cumSumInPlace(double[] a, int dir) {
		if (dir>0) {
			for (int i=1; i<a.length; i++)
				a[i] = a[i-1]+a[i];
		} else {
			for (int i=a.length-2; i>=0; i--)
				a[i] = a[i+1]+a[i];
		}
	}

	public static int[] cumSum(int[] a, int dir) {
		int[] re = new int[a.length];
		if (dir>0) {
			re[0] = a[0];
			for (int i=1; i<a.length; i++)
				re[i] = re[i-1]+a[i];
		} else {
			re[re.length-1] = a[re.length-1];
			for (int i=re.length-2; i>=0; i--)
				re[i] = re[i+1]+a[i];
		}
		return re;
	}

	public static double[] cumSum(double[] a, int dir) {
		double[] re = new double[a.length];
		if (dir>0) {
			re[0] = a[0];
			for (int i=1; i<a.length; i++)
				re[i] = re[i-1]+a[i];
		} else {
			re[re.length-1] = a[re.length-1];
			for (int i=re.length-2; i>=0; i--)
				re[i] = re[i+1]+a[i];
		}
		return re;
	}
	
	public static double[] cumSumAndNormalize(double[] a, int dir) {
		double[] re = new double[a.length];
		if (dir>0) {
			re[0] = a[0];
			for (int i=1; i<a.length; i++)
				re[i] = re[i-1]+a[i];
			for (int i=0; i<a.length; i++)
				re[i]/=re[re.length-1];
		} else {
			re[re.length-1] = a[re.length-1];
			for (int i=re.length-2; i>=0; i--)
				re[i] = re[i+1]+a[i];
			for (int i=0; i<a.length; i++)
				re[i]/=re[0];
		}
		return re;
	}


	public static int[] histoToArray(Histogram1D histo) {
		int[] re = new int[histo.xAxis().bins()+2];
		re[0] = histo.binEntries(Histogram1D.UNDERFLOW);
		for (int i=0; i<histo.xAxis().bins(); i++)
			re[i+1] = histo.binEntries(i);
		re[re.length-1] = histo.binEntries(Histogram1D.OVERFLOW);
		return re;
	}


	public static int[][] histoToArray(Histogram2D histo) {
		int[][] re = new int[histo.xAxis().bins()+2][histo.yAxis().bins()+2];
		re[0][0] = histo.binEntries(Histogram2D.UNDERFLOW, Histogram2D.UNDERFLOW);
		re[0][re[0].length-1] = histo.binEntries(Histogram2D.UNDERFLOW, Histogram2D.OVERFLOW);
		re[re.length-1][0] = histo.binEntries(Histogram2D.OVERFLOW, Histogram2D.UNDERFLOW);
		re[re.length-1][re[0].length-1] = histo.binEntries(Histogram2D.OVERFLOW, Histogram2D.OVERFLOW);

		for (int i=0; i<histo.xAxis().bins(); i++) {
			re[i+1][0] = histo.binEntries(i, Histogram2D.UNDERFLOW);
			re[i+1][re[0].length-1] = histo.binEntries(i, Histogram2D.OVERFLOW);
		}

		for (int i=0; i<histo.yAxis().bins(); i++) {
			re[0][i+1] = histo.binEntries(Histogram2D.UNDERFLOW,i);
			re[re[0].length-1][i+1] = histo.binEntries(Histogram2D.OVERFLOW,i);
		}

		for (int x=0; x<histo.xAxis().bins(); x++) 
			for (int y=0; y<histo.yAxis().bins(); y++) 
				re[x+1][y+1] = histo.binEntries(x, y);

		return re;
	}

	public static void copyValues(Object[] source, int[] indices, double[] target) {
		for (int i=0; i<indices.length; i++)
			target[i] = (Double)source[indices[i]];
	}


	/**
	 * Computes the median of a. If a.length is an even number, the mean of the upper and lower median is taken.
	 * Afterward, a will be sorted!
	 * @param a
	 * @return
	 */
	public static double median(double[] a) {
		Arrays.sort(a);
		if (a.length%2==1)
			return a[a.length/2];
		else
			return (a[a.length/2-1]+a[a.length/2])/2.0;
	}

	public static boolean isAscending(int[] a) {
		return isAscending(a,0,a.length);
	}

	public static boolean isDescending(int[] a) {
		return isDescending(a,0,a.length);
	}

	public static boolean isStrictAscending(int[] a) {
		return isStrictAscending(a,0,a.length);
	}

	public static boolean isStrictDescending(int[] a) {
		return isStrictDescending(a,0,a.length);
	}

	public static boolean isAscending(int[] a, int from, int to) {
		for (int i=from+1; i<to; i++)
			if (a[i-1]>a[i]) return false;
		return true;
	}

	public static boolean isDescending(int[] a, int from, int to) {
		for (int i=from+1; i<to; i++)
			if (a[i-1]<a[i]) return false;
		return true;
	}

	public static boolean isStrictAscending(int[] a, int from, int to) {
		for (int i=from+1; i<to; i++)
			if (a[i-1]>=a[i]) return false;
		return true;
	}

	public static boolean isStrictDescending(int[] a, int from, int to) {
		for (int i=from+1; i<to; i++)
			if (a[i-1]<=a[i]) return false;
		return true;
	}


	/**
	 * 0,0 is in the left bottom corner of the output!
	 * @param m
	 * @return
	 */
	public static String matrixToString(double[][] m) {
		StringBuilder sb = new StringBuilder();

		for (int i=0; i<m.length; i++) {
			for (int j=0; j<m[i].length; j++) {
				sb.append(m[i][j]);
				if (j<m[i].length-1)
					sb.append("\t");
			}
			if (i<m.length-1)
				sb.append("\n");
		}

		return sb.toString();
	}

	/**
	 * 0,0 is in the left bottom corner of the output!
	 * @param m
	 * @return
	 */
	public static <T> String matrixToString(T[][] m) {
		StringBuilder sb = new StringBuilder();

		for (int i=0; i<m.length; i++) {
			for (int j=0; j<m[i].length; j++) {
				sb.append(m[i][j]);
				if (j<m[i].length-1)
					sb.append("\t");
			}
			if (i<m.length-1)
				sb.append("\n");
		}

		return sb.toString();
	}

	/**
	 * 0,0 is in the left bottom corner of the output!
	 * @param m
	 * @return
	 */
	public static String matrixToString(double[][] m, String format) {
		StringBuilder sb = new StringBuilder();

		for (int i=0; i<m.length; i++) {
			for (int j=0; j<m[i].length; j++) {
				sb.append(String.format(Locale.US,format,m[i][j]));
				if (j<m[i].length-1)
					sb.append("\t");
			}
			if (i<m.length-1)
				sb.append("\n");
		}

		return sb.toString();
	}

	/**
	 * 0,0 is in the left bottom corner of the output!
	 * @param m
	 * @return
	 */
	public static String matrixToString(double[][] m, int rowfrom, int rowto, int colfrom, int colto, String format) {
		StringBuilder sb = new StringBuilder();

		for (int i=rowfrom; i<rowto; i++) {
			for (int j=colfrom; j<colto; j++) {
				sb.append(String.format(Locale.US,format,m[i][j]));
				if (j<colto-1)
					sb.append("\t");
			}
			if (i<rowto-1)
				sb.append("\n");
		}

		return sb.toString();
	}

	/**
	 * 0,0 is in the left bottom corner of the output!
	 * @param m
	 * @return
	 */
	public static String matrixToString(float[][] m) {
		StringBuilder sb = new StringBuilder();

		for (int i=0; i<m.length; i++) {
			for (int j=0; j<m[i].length; j++) {
				sb.append(m[i][j]);
				if (j<m[i].length-1)
					sb.append("\t");
			}
			if (i<m.length-1)
				sb.append("\n");
		}

		return sb.toString();
	}

	/**
	 * 0,0 is in the left bottom corner of the output!
	 * @param m
	 * @return
	 */
	public static String matrixToString(int[][] m) {
		StringBuilder sb = new StringBuilder();

		for (int i=0; i<m.length; i++) {
			for (int j=0; j<m[i].length; j++) {
				sb.append(m[i][j]);
				if (j<m[i].length-1)
					sb.append("\t");
			}
			if (i<m.length-1)
				sb.append("\n");
		}

		return sb.toString();
	}


	/**
	 * Returns the sorted permutation of a, i.e. re[0] contains the index of the smallest value in a,
	 * re[1] the second smallest and so on.
	 * @param <T>
	 * @param a
	 * @param comp
	 * @return
	 */
	public static <T> int[] sortedPermutation(final T[] a,final Comparator<? super T> comp) {
		Integer[] re = new Integer[a.length];
		for (int i=0; i<re.length; i++)
			re[i] = i;
		Arrays.sort(re,new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				return comp.compare(a[o1], a[o2]);
			}
		});
		return intCollectionToPrimitiveArray(Arrays.asList(re));
	}

	/**
	 * Returns the sorted permutation of a, i.e. re[0] contains the index of the smallest value in a,
	 * re[1] the second smallest and so on.
	 * @param <T>
	 * @param a
	 * @param comp
	 * @return
	 */
	public static int[] sortedPermutation(final double[] a) {
		Integer[] re = new Integer[a.length];
		for (int i=0; i<re.length; i++)
			re[i] = i;
		Arrays.sort(re,new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				return Double.compare(a[o1], a[o2]);
			}
		});
		return intCollectionToPrimitiveArray(Arrays.asList(re));
	}


	/// parallel sorting
	/**
	 * Sorts the specified array of longs into ascending numerical order.
	 * The sorting algorithm is a tuned quicksort, adapted from Jon
	 * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	 * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	 * 1993).  This algorithm offers n*log(n) performance on many data sets
	 * that cause other quicksorts to degrade to quadratic performance.
	 *
	 * @param a the array to be sorted
	 */
	public static <T> void parallelSort(long[] a, T[] a2) {
		if (a.length!=a2.length)
			throw new IllegalArgumentException("Arrays don't have same length!");
		sort1(a, a2,0, a.length);
	}

	/**
	 * Sorts the specified range of the specified array of longs into
	 * ascending numerical order.  The range to be sorted extends from index
	 * <tt>fromIndex</tt>, inclusive, to index <tt>toIndex</tt>, exclusive.
	 * (If <tt>fromIndex==toIndex</tt>, the range to be sorted is empty.)
	 *
	 * <p>The sorting algorithm is a tuned quicksort, adapted from Jon
	 * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	 * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	 * 1993).  This algorithm offers n*log(n) performance on many data sets
	 * that cause other quicksorts to degrade to quadratic performance.
	 *
	 * @param a the array to be sorted
	 * @param fromIndex the index of the first element (inclusive) to be
	 *        sorted
	 * @param toIndex the index of the last element (exclusive) to be sorted
	 * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	 * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	 * <tt>toIndex &gt; a.length</tt>
	 */
	public static <T> void parallelSort(long[] a, T[] a2, int fromIndex, int toIndex) {
		rangeCheck(a.length, fromIndex, toIndex);
		rangeCheck(a2.length, fromIndex, toIndex);
		sort1(a, a2, fromIndex, toIndex-fromIndex);
	}

	/**
	 * Sorts the specified array of longs into ascending numerical order.
	 * The sorting algorithm is a tuned quicksort, adapted from Jon
	 * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	 * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	 * 1993).  This algorithm offers n*log(n) performance on many data sets
	 * that cause other quicksorts to degrade to quadratic performance.
	 *
	 * @param a the array to be sorted
	 */
	public static <T,P> void parallelSort(T[] a, P[] a2, Comparator<T> comp) {
		if (a.length!=a2.length)
			throw new IllegalArgumentException("Arrays don't have same length!");
		sort1(a, a2, comp,0, a.length);
	}

	/**
	 * Sorts the specified range of the specified array of longs into
	 * ascending numerical order.  The range to be sorted extends from index
	 * <tt>fromIndex</tt>, inclusive, to index <tt>toIndex</tt>, exclusive.
	 * (If <tt>fromIndex==toIndex</tt>, the range to be sorted is empty.)
	 *
	 * <p>The sorting algorithm is a tuned quicksort, adapted from Jon
	 * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	 * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	 * 1993).  This algorithm offers n*log(n) performance on many data sets
	 * that cause other quicksorts to degrade to quadratic performance.
	 *
	 * @param a the array to be sorted
	 * @param fromIndex the index of the first element (inclusive) to be
	 *        sorted
	 * @param toIndex the index of the last element (exclusive) to be sorted
	 * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	 * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	 * <tt>toIndex &gt; a.length</tt>
	 */
	public static <T,P> void parallelSort(T[] a, int[] a2, Comparator<T> comp, int fromIndex, int toIndex) {
		rangeCheck(a.length, fromIndex, toIndex);
		rangeCheck(a2.length, fromIndex, toIndex);
		sort1(a, a2, comp, fromIndex, toIndex-fromIndex);
	}

	/**
	 * Sorts the specified array of longs into ascending numerical order.
	 * The sorting algorithm is a tuned quicksort, adapted from Jon
	 * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	 * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	 * 1993).  This algorithm offers n*log(n) performance on many data sets
	 * that cause other quicksorts to degrade to quadratic performance.
	 *
	 * @param a the array to be sorted
	 */
	public static <T,P> void parallelSort(T[] a, int[] a2, Comparator<T> comp) {
		if (a.length!=a2.length)
			throw new IllegalArgumentException("Arrays don't have same length!");
		sort1(a, a2, comp,0, a.length);
	}

	/**
	 * Sorts the specified range of the specified array of longs into
	 * ascending numerical order.  The range to be sorted extends from index
	 * <tt>fromIndex</tt>, inclusive, to index <tt>toIndex</tt>, exclusive.
	 * (If <tt>fromIndex==toIndex</tt>, the range to be sorted is empty.)
	 *
	 * <p>The sorting algorithm is a tuned quicksort, adapted from Jon
	 * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	 * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	 * 1993).  This algorithm offers n*log(n) performance on many data sets
	 * that cause other quicksorts to degrade to quadratic performance.
	 *
	 * @param a the array to be sorted
	 * @param fromIndex the index of the first element (inclusive) to be
	 *        sorted
	 * @param toIndex the index of the last element (exclusive) to be sorted
	 * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	 * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	 * <tt>toIndex &gt; a.length</tt>
	 */
	public static <T,P> void parallelSort(T[] a, P[] a2, Comparator<T> comp, int fromIndex, int toIndex) {
		rangeCheck(a.length, fromIndex, toIndex);
		rangeCheck(a2.length, fromIndex, toIndex);
		sort1(a, a2, comp, fromIndex, toIndex-fromIndex);
	}

	/**
	 * Sorts the specified array of ints into ascending numerical order.
	 * The sorting algorithm is a tuned quicksort, adapted from Jon
	 * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	 * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	 * 1993).  This algorithm offers n*log(n) performance on many data sets
	 * that cause other quicksorts to degrade to quadratic performance.
	 *
	 * @param a the array to be sorted
	 */
	public static <T> void parallelSort(int[] a, T[] a2) {
		if (a.length!=a2.length)
			throw new IllegalArgumentException("Arrays don't have same length!");
		sort1(a, a2, 0, a.length);
	}

	/**
	 * Sorts the specified range of the specified array of ints into
	 * ascending numerical order.  The range to be sorted extends from index
	 * <tt>fromIndex</tt>, inclusive, to index <tt>toIndex</tt>, exclusive.
	 * (If <tt>fromIndex==toIndex</tt>, the range to be sorted is empty.)<p>
	 *
	 * The sorting algorithm is a tuned quicksort, adapted from Jon
	 * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	 * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	 * 1993).  This algorithm offers n*log(n) performance on many data sets
	 * that cause other quicksorts to degrade to quadratic performance.
	 *
	 * @param a the array to be sorted
	 * @param fromIndex the index of the first element (inclusive) to be
	 *        sorted
	 * @param toIndex the index of the last element (exclusive) to be sorted
	 * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	 * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	 *	       <tt>toIndex &gt; a.length</tt>
	 */
	public static <T> void parallelSort(int[] a, T[] a2, int fromIndex, int toIndex) {
		rangeCheck(a.length, fromIndex, toIndex);
		rangeCheck(a2.length, fromIndex, toIndex);
		sort1(a, a2, fromIndex, toIndex-fromIndex);
	}


	/**
	 * Sorts the specified array of ints into ascending numerical order.
	 * The sorting algorithm is a tuned quicksort, adapted from Jon
	 * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	 * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	 * 1993).  This algorithm offers n*log(n) performance on many data sets
	 * that cause other quicksorts to degrade to quadratic performance.
	 *
	 * @param a the array to be sorted
	 */
	public static void parallelSort(int[] a, int[] a2) {
		if (a.length!=a2.length)
			throw new IllegalArgumentException("Arrays don't have same length!");
		sort1(a, a2, 0, a.length);
	}

	/**
	 * Sorts the specified range of the specified array of ints into
	 * ascending numerical order.  The range to be sorted extends from index
	 * <tt>fromIndex</tt>, inclusive, to index <tt>toIndex</tt>, exclusive.
	 * (If <tt>fromIndex==toIndex</tt>, the range to be sorted is empty.)<p>
	 *
	 * The sorting algorithm is a tuned quicksort, adapted from Jon
	 * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	 * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	 * 1993).  This algorithm offers n*log(n) performance on many data sets
	 * that cause other quicksorts to degrade to quadratic performance.
	 *
	 * @param a the array to be sorted
	 * @param fromIndex the index of the first element (inclusive) to be
	 *        sorted
	 * @param toIndex the index of the last element (exclusive) to be sorted
	 * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	 * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	 *	       <tt>toIndex &gt; a.length</tt>
	 */
	public static void parallelSort(int[] a, int[] a2, int fromIndex, int toIndex) {
		rangeCheck(a.length, fromIndex, toIndex);
		rangeCheck(a2.length, fromIndex, toIndex);
		sort1(a, a2, fromIndex, toIndex-fromIndex);
	}


	/**
	 * Sorts the specified array of shorts into ascending numerical order.
	 * The sorting algorithm is a tuned quicksort, adapted from Jon
	 * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	 * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	 * 1993).  This algorithm offers n*log(n) performance on many data sets
	 * that cause other quicksorts to degrade to quadratic performance.
	 *
	 * @param a the array to be sorted
	 */
	public static <T> void parallelSort(short[] a, T[] a2) {
		if (a.length!=a2.length)
			throw new IllegalArgumentException("Arrays don't have same length!");
		sort1(a, a2, 0, a.length);
	}

	/**
	 * Sorts the specified range of the specified array of shorts into
	 * ascending numerical order.  The range to be sorted extends from index
	 * <tt>fromIndex</tt>, inclusive, to index <tt>toIndex</tt>, exclusive.
	 * (If <tt>fromIndex==toIndex</tt>, the range to be sorted is empty.)<p>
	 *
	 * The sorting algorithm is a tuned quicksort, adapted from Jon
	 * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	 * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	 * 1993).  This algorithm offers n*log(n) performance on many data sets
	 * that cause other quicksorts to degrade to quadratic performance.
	 *
	 * @param a the array to be sorted
	 * @param fromIndex the index of the first element (inclusive) to be
	 *        sorted
	 * @param toIndex the index of the last element (exclusive) to be sorted
	 * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	 * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	 *	       <tt>toIndex &gt; a.length</tt>
	 */
	public static <T> void parallelSort(short[] a, T[] a2, int fromIndex, int toIndex) {
		rangeCheck(a.length, fromIndex, toIndex);
		rangeCheck(a2.length, fromIndex, toIndex);
		sort1(a, a2,fromIndex, toIndex-fromIndex);
	}

	/**
	 * Sorts the specified array of chars into ascending numerical order.
	 * The sorting algorithm is a tuned quicksort, adapted from Jon
	 * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	 * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	 * 1993).  This algorithm offers n*log(n) performance on many data sets
	 * that cause other quicksorts to degrade to quadratic performance.
	 *
	 * @param a the array to be sorted
	 */
	public static <T> void parallelSort(char[] a, T[] a2) {
		if (a.length!=a2.length)
			throw new IllegalArgumentException("Arrays don't have same length!");
		sort1(a, a2, 0, a.length);
	}

	/**
	 * Sorts the specified range of the specified array of chars into
	 * ascending numerical order.  The range to be sorted extends from index
	 * <tt>fromIndex</tt>, inclusive, to index <tt>toIndex</tt>, exclusive.
	 * (If <tt>fromIndex==toIndex</tt>, the range to be sorted is empty.)<p>
	 *
	 * The sorting algorithm is a tuned quicksort, adapted from Jon
	 * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	 * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	 * 1993).  This algorithm offers n*log(n) performance on many data sets
	 * that cause other quicksorts to degrade to quadratic performance.
	 *
	 * @param a the array to be sorted
	 * @param fromIndex the index of the first element (inclusive) to be
	 *        sorted
	 * @param toIndex the index of the last element (exclusive) to be sorted
	 * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	 * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	 *	       <tt>toIndex &gt; a.length</tt>
	 */
	public static <T> void parallelSort(char[] a, T[] a2, int fromIndex, int toIndex) {
		rangeCheck(a.length, fromIndex, toIndex);
		rangeCheck(a2.length, fromIndex, toIndex);
		sort1(a, a2, fromIndex, toIndex-fromIndex);
	}

	/**
	 * Sorts the specified array of bytes into ascending numerical order.
	 * The sorting algorithm is a tuned quicksort, adapted from Jon
	 * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	 * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	 * 1993).  This algorithm offers n*log(n) performance on many data sets
	 * that cause other quicksorts to degrade to quadratic performance.
	 *
	 * @param a the array to be sorted
	 */
	public static <T> void parallelSort(byte[] a, T[] a2) {
		if (a.length!=a2.length)
			throw new IllegalArgumentException("Arrays don't have same length!");
		sort1(a, a2, 0, a.length);
	}

	/**
	 * Sorts the specified range of the specified array of bytes into
	 * ascending numerical order.  The range to be sorted extends from index
	 * <tt>fromIndex</tt>, inclusive, to index <tt>toIndex</tt>, exclusive.
	 * (If <tt>fromIndex==toIndex</tt>, the range to be sorted is empty.)<p>
	 *
	 * The sorting algorithm is a tuned quicksort, adapted from Jon
	 * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	 * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	 * 1993).  This algorithm offers n*log(n) performance on many data sets
	 * that cause other quicksorts to degrade to quadratic performance.
	 *
	 * @param a the array to be sorted
	 * @param fromIndex the index of the first element (inclusive) to be
	 *        sorted
	 * @param toIndex the index of the last element (exclusive) to be sorted
	 * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	 * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	 *	       <tt>toIndex &gt; a.length</tt>
	 */
	public static <T> void parallelSort(byte[] a, T[] a2, int fromIndex, int toIndex) {
		rangeCheck(a.length, fromIndex, toIndex);
		rangeCheck(a2.length, fromIndex, toIndex);
		sort1(a, a2, fromIndex, toIndex-fromIndex);
	}

	/**
	 * Sorts the specified array of doubles into ascending numerical order.
	 * <p>
	 * The <code>&lt;</code> relation does not provide a total order on
	 * all floating-point values; although they are distinct numbers
	 * <code>-0.0 == 0.0</code> is <code>true</code> and a NaN value
	 * compares neither less than, greater than, nor equal to any
	 * floating-point value, even itself.  To allow the sort to
	 * proceed, instead of using the <code>&lt;</code> relation to
	 * determine ascending numerical order, this method uses the total
	 * order imposed by {@link Double#compareTo}.  This ordering
	 * differs from the <code>&lt;</code> relation in that
	 * <code>-0.0</code> is treated as less than <code>0.0</code> and
	 * NaN is considered greater than any other floating-point value.
	 * For the purposes of sorting, all NaN values are considered
	 * equivalent and equal.
	 * <p>
	 * The sorting algorithm is a tuned quicksort, adapted from Jon
	 * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	 * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	 * 1993).  This algorithm offers n*log(n) performance on many data sets
	 * that cause other quicksorts to degrade to quadratic performance.
	 *
	 * @param a the array to be sorted
	 */
	public static <T> void parallelSort(double[] a, T[] a2) {
		if (a.length!=a2.length)
			throw new IllegalArgumentException("Arrays don't have same length!");
		sort2(a, a2, 0, a.length);
	}

	/**
	 * Sorts the specified range of the specified array of doubles into
	 * ascending numerical order.  The range to be sorted extends from index
	 * <tt>fromIndex</tt>, inclusive, to index <tt>toIndex</tt>, exclusive.
	 * (If <tt>fromIndex==toIndex</tt>, the range to be sorted is empty.)
	 * <p>
	 * The <code>&lt;</code> relation does not provide a total order on
	 * all floating-point values; although they are distinct numbers
	 * <code>-0.0 == 0.0</code> is <code>true</code> and a NaN value
	 * compares neither less than, greater than, nor equal to any
	 * floating-point value, even itself.  To allow the sort to
	 * proceed, instead of using the <code>&lt;</code> relation to
	 * determine ascending numerical order, this method uses the total
	 * order imposed by {@link Double#compareTo}.  This ordering
	 * differs from the <code>&lt;</code> relation in that
	 * <code>-0.0</code> is treated as less than <code>0.0</code> and
	 * NaN is considered greater than any other floating-point value.
	 * For the purposes of sorting, all NaN values are considered
	 * equivalent and equal.
	 * <p>
	 * The sorting algorithm is a tuned quicksort, adapted from Jon
	 * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	 * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	 * 1993).  This algorithm offers n*log(n) performance on many data sets
	 * that cause other quicksorts to degrade to quadratic performance.
	 *
	 * @param a the array to be sorted
	 * @param fromIndex the index of the first element (inclusive) to be
	 *        sorted
	 * @param toIndex the index of the last element (exclusive) to be sorted
	 * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	 * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	 *	       <tt>toIndex &gt; a.length</tt>
	 */
	public static <T> void parallelSort(double[] a, T[] a2, int fromIndex, int toIndex) {
		rangeCheck(a.length, fromIndex, toIndex);
		rangeCheck(a2.length, fromIndex, toIndex);
		sort2(a, a2, fromIndex, toIndex);
	}

	public static void parallelSort(double[] a, double[] a2) {
		parallelSort(a, a2,0,a.length);
	}

	/**
	 * Sorts the specified range of the specified array of doubles into
	 * ascending numerical order.  The range to be sorted extends from index
	 * <tt>fromIndex</tt>, inclusive, to index <tt>toIndex</tt>, exclusive.
	 * (If <tt>fromIndex==toIndex</tt>, the range to be sorted is empty.)
	 * <p>
	 * The <code>&lt;</code> relation does not provide a total order on
	 * all floating-point values; although they are distinct numbers
	 * <code>-0.0 == 0.0</code> is <code>true</code> and a NaN value
	 * compares neither less than, greater than, nor equal to any
	 * floating-point value, even itself.  To allow the sort to
	 * proceed, instead of using the <code>&lt;</code> relation to
	 * determine ascending numerical order, this method uses the total
	 * order imposed by {@link Double#compareTo}.  This ordering
	 * differs from the <code>&lt;</code> relation in that
	 * <code>-0.0</code> is treated as less than <code>0.0</code> and
	 * NaN is considered greater than any other floating-point value.
	 * For the purposes of sorting, all NaN values are considered
	 * equivalent and equal.
	 * <p>
	 * The sorting algorithm is a tuned quicksort, adapted from Jon
	 * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	 * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	 * 1993).  This algorithm offers n*log(n) performance on many data sets
	 * that cause other quicksorts to degrade to quadratic performance.
	 *
	 * @param a the array to be sorted
	 * @param fromIndex the index of the first element (inclusive) to be
	 *        sorted
	 * @param toIndex the index of the last element (exclusive) to be sorted
	 * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	 * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	 *	       <tt>toIndex &gt; a.length</tt>
	 */
	public static void parallelSort(double[] a, double[] a2, int fromIndex, int toIndex) {
		rangeCheck(a.length, fromIndex, toIndex);
		rangeCheck(a2.length, fromIndex, toIndex);
		sort2(a, a2, fromIndex, toIndex);
	}

	public static void parallelSort(int[] a, double[] a2) {
		parallelSort(a, a2,0,a.length);
	}

	public static void parallelSort(char[] a, char[] a2) {
		parallelSort(a, a2,0,a.length);
	}

	public static void parallelSort(char[] a, char[] a2, int fromIndex, int toIndex) {
		rangeCheck(a.length, fromIndex, toIndex);
		rangeCheck(a2.length, fromIndex, toIndex);
		sort1(a, a2, fromIndex, toIndex);
	}

	/**
	 * Sorts the specified range of the specified array of doubles into
	 * ascending numerical order.  The range to be sorted extends from index
	 * <tt>fromIndex</tt>, inclusive, to index <tt>toIndex</tt>, exclusive.
	 * (If <tt>fromIndex==toIndex</tt>, the range to be sorted is empty.)
	 * <p>
	 * The <code>&lt;</code> relation does not provide a total order on
	 * all floating-point values; although they are distinct numbers
	 * <code>-0.0 == 0.0</code> is <code>true</code> and a NaN value
	 * compares neither less than, greater than, nor equal to any
	 * floating-point value, even itself.  To allow the sort to
	 * proceed, instead of using the <code>&lt;</code> relation to
	 * determine ascending numerical order, this method uses the total
	 * order imposed by {@link Double#compareTo}.  This ordering
	 * differs from the <code>&lt;</code> relation in that
	 * <code>-0.0</code> is treated as less than <code>0.0</code> and
	 * NaN is considered greater than any other floating-point value.
	 * For the purposes of sorting, all NaN values are considered
	 * equivalent and equal.
	 * <p>
	 * The sorting algorithm is a tuned quicksort, adapted from Jon
	 * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	 * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	 * 1993).  This algorithm offers n*log(n) performance on many data sets
	 * that cause other quicksorts to degrade to quadratic performance.
	 *
	 * @param a the array to be sorted
	 * @param fromIndex the index of the first element (inclusive) to be
	 *        sorted
	 * @param toIndex the index of the last element (exclusive) to be sorted
	 * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	 * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	 *	       <tt>toIndex &gt; a.length</tt>
	 */
	public static void parallelSort(int[] a, double[] a2, int fromIndex, int toIndex) {
		rangeCheck(a.length, fromIndex, toIndex);
		rangeCheck(a2.length, fromIndex, toIndex);
		sort1(a, a2, fromIndex, toIndex);
	}

	public static void parallelSort(double[] a, int[] a2) {
		parallelSort(a, a2,0,a.length);
	}

	/**
	 * Sorts the specified range of the specified array of doubles into
	 * ascending numerical order.  The range to be sorted extends from index
	 * <tt>fromIndex</tt>, inclusive, to index <tt>toIndex</tt>, exclusive.
	 * (If <tt>fromIndex==toIndex</tt>, the range to be sorted is empty.)
	 * <p>
	 * The <code>&lt;</code> relation does not provide a total order on
	 * all floating-point values; although they are distinct numbers
	 * <code>-0.0 == 0.0</code> is <code>true</code> and a NaN value
	 * compares neither less than, greater than, nor equal to any
	 * floating-point value, even itself.  To allow the sort to
	 * proceed, instead of using the <code>&lt;</code> relation to
	 * determine ascending numerical order, this method uses the total
	 * order imposed by {@link Double#compareTo}.  This ordering
	 * differs from the <code>&lt;</code> relation in that
	 * <code>-0.0</code> is treated as less than <code>0.0</code> and
	 * NaN is considered greater than any other floating-point value.
	 * For the purposes of sorting, all NaN values are considered
	 * equivalent and equal.
	 * <p>
	 * The sorting algorithm is a tuned quicksort, adapted from Jon
	 * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	 * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	 * 1993).  This algorithm offers n*log(n) performance on many data sets
	 * that cause other quicksorts to degrade to quadratic performance.
	 *
	 * @param a the array to be sorted
	 * @param fromIndex the index of the first element (inclusive) to be
	 *        sorted
	 * @param toIndex the index of the last element (exclusive) to be sorted
	 * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	 * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	 *	       <tt>toIndex &gt; a.length</tt>
	 */
	public static void parallelSort(double[] a, int[] a2, int fromIndex, int toIndex) {
		rangeCheck(a.length, fromIndex, toIndex);
		rangeCheck(a2.length, fromIndex, toIndex);
		sort2(a, a2, fromIndex, toIndex);
	}

	/**
	 * Sorts the specified array of floats into ascending numerical order.
	 * <p>
	 * The <code>&lt;</code> relation does not provide a total order on
	 * all floating-point values; although they are distinct numbers
	 * <code>-0.0f == 0.0f</code> is <code>true</code> and a NaN value
	 * compares neither less than, greater than, nor equal to any
	 * floating-point value, even itself.  To allow the sort to
	 * proceed, instead of using the <code>&lt;</code> relation to
	 * determine ascending numerical order, this method uses the total
	 * order imposed by {@link Float#compareTo}.  This ordering
	 * differs from the <code>&lt;</code> relation in that
	 * <code>-0.0f</code> is treated as less than <code>0.0f</code> and
	 * NaN is considered greater than any other floating-point value.
	 * For the purposes of sorting, all NaN values are considered
	 * equivalent and equal.
	 * <p>
	 * The sorting algorithm is a tuned quicksort, adapted from Jon
	 * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	 * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	 * 1993).  This algorithm offers n*log(n) performance on many data sets
	 * that cause other quicksorts to degrade to quadratic performance.
	 *
	 * @param a the array to be sorted
	 */
	public static <T> void parallelSort(float[] a, T[] a2) {
		if (a.length!=a2.length)
			throw new IllegalArgumentException("Arrays don't have same length!");
		sort2(a, a2, 0, a.length);
	}

	/**
	 * Sorts the specified range of the specified array of floats into
	 * ascending numerical order.  The range to be sorted extends from index
	 * <tt>fromIndex</tt>, inclusive, to index <tt>toIndex</tt>, exclusive.
	 * (If <tt>fromIndex==toIndex</tt>, the range to be sorted is empty.)
	 * <p>
	 * The <code>&lt;</code> relation does not provide a total order on
	 * all floating-point values; although they are distinct numbers
	 * <code>-0.0f == 0.0f</code> is <code>true</code> and a NaN value
	 * compares neither less than, greater than, nor equal to any
	 * floating-point value, even itself.  To allow the sort to
	 * proceed, instead of using the <code>&lt;</code> relation to
	 * determine ascending numerical order, this method uses the total
	 * order imposed by {@link Float#compareTo}.  This ordering
	 * differs from the <code>&lt;</code> relation in that
	 * <code>-0.0f</code> is treated as less than <code>0.0f</code> and
	 * NaN is considered greater than any other floating-point value.
	 * For the purposes of sorting, all NaN values are considered
	 * equivalent and equal.
	 * <p>
	 * The sorting algorithm is a tuned quicksort, adapted from Jon
	 * L. Bentley and M. Douglas McIlroy's "Engineering a Sort Function",
	 * Software-Practice and Experience, Vol. 23(11) P. 1249-1265 (November
	 * 1993).  This algorithm offers n*log(n) performance on many data sets
	 * that cause other quicksorts to degrade to quadratic performance.
	 *
	 * @param a the array to be sorted
	 * @param fromIndex the index of the first element (inclusive) to be
	 *        sorted
	 * @param toIndex the index of the last element (exclusive) to be sorted
	 * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	 * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	 *	       <tt>toIndex &gt; a.length</tt>
	 */
	public static <T> void parallelSort(float[] a, T[] a2, int fromIndex, int toIndex) {
		rangeCheck(a.length, fromIndex, toIndex);
		rangeCheck(a2.length, fromIndex, toIndex);
		sort2(a, a2, fromIndex, toIndex);
	}

	private static <T> void sort2(double a[], T[] a2, int fromIndex, int toIndex) {
		final long NEG_ZERO_BITS = Double.doubleToLongBits(-0.0d);
		/*
		 * The sort is done in three phases to avoid the expense of using
		 * NaN and -0.0 aware comparisons during the main sort.
		 */

		/*
		 * Preprocessing phase:  Move any NaN's to end of array, count the
		 * number of -0.0's, and turn them into 0.0's.
		 */
		int numNegZeros = 0;
		int i = fromIndex, n = toIndex;
		while(i < n) {
			if (a[i] != a[i]) {
				double swap = a[i];
				a[i] = a[--n];
				a[n] = swap;
				T swap2 = a2[i];
				a2[i] = a2[n];
				a2[n] = swap2;
			} else {
				if (a[i]==0 && Double.doubleToLongBits(a[i])==NEG_ZERO_BITS) {
					a[i] = 0.0d;
					numNegZeros++;
				}
				i++;
			}
		}

		// Main sort phase: quicksort everything but the NaN's
		sort1(a, a2, fromIndex, n-fromIndex);

		// Postprocessing phase: change 0.0's to -0.0's as required
		if (numNegZeros != 0) {
			int j = binarySearch0(a, fromIndex, n, 0.0d); // posn of ANY zero
			do {
				j--;
			} while (j>=0 && a[j]==0.0d);

			// j is now one less than the index of the FIRST zero
			for (int k=0; k<numNegZeros; k++)
				a[++j] = -0.0d;
		}
	}

	private static void sort2(double a[], double[] a2, int fromIndex, int toIndex) {
		final long NEG_ZERO_BITS = Double.doubleToLongBits(-0.0d);
		/*
		 * The sort is done in three phases to avoid the expense of using
		 * NaN and -0.0 aware comparisons during the main sort.
		 */

		/*
		 * Preprocessing phase:  Move any NaN's to end of array, count the
		 * number of -0.0's, and turn them into 0.0's.
		 */
		int numNegZeros = 0;
		int i = fromIndex, n = toIndex;
		while(i < n) {
			if (a[i] != a[i]) {
				double swap = a[i];
				a[i] = a[--n];
				a[n] = swap;
				double swap2 = a2[i];
				a2[i] = a2[n];
				a2[n] = swap2;
			} else {
				if (a[i]==0 && Double.doubleToLongBits(a[i])==NEG_ZERO_BITS) {
					a[i] = 0.0d;
					numNegZeros++;
				}
				i++;
			}
		}

		// Main sort phase: quicksort everything but the NaN's
		sort1(a, a2, fromIndex, n-fromIndex);

		// Postprocessing phase: change 0.0's to -0.0's as required
		if (numNegZeros != 0) {
			int j = binarySearch0(a, fromIndex, n, 0.0d); // posn of ANY zero
			do {
				j--;
			} while (j>=0 && a[j]==0.0d);

			// j is now one less than the index of the FIRST zero
			for (int k=0; k<numNegZeros; k++)
				a[++j] = -0.0d;
		}
	}



	private static void sort2(double a[], int[] a2, int fromIndex, int toIndex) {
		final long NEG_ZERO_BITS = Double.doubleToLongBits(-0.0d);
		/*
		 * The sort is done in three phases to avoid the expense of using
		 * NaN and -0.0 aware comparisons during the main sort.
		 */

		/*
		 * Preprocessing phase:  Move any NaN's to end of array, count the
		 * number of -0.0's, and turn them into 0.0's.
		 */
		int numNegZeros = 0;
		int i = fromIndex, n = toIndex;
		while(i < n) {
			if (a[i] != a[i]) {
				double swap = a[i];
				a[i] = a[--n];
				a[n] = swap;
				int swap2 = a2[i];
				a2[i] = a2[n];
				a2[n] = swap2;
			} else {
				if (a[i]==0 && Double.doubleToLongBits(a[i])==NEG_ZERO_BITS) {
					a[i] = 0.0d;
					numNegZeros++;
				}
				i++;
			}
		}

		// Main sort phase: quicksort everything but the NaN's
		sort1(a, a2, fromIndex, n-fromIndex);

		// Postprocessing phase: change 0.0's to -0.0's as required
		if (numNegZeros != 0) {
			int j = binarySearch0(a, fromIndex, n, 0.0d); // posn of ANY zero
			do {
				j--;
			} while (j>=0 && a[j]==0.0d);

			// j is now one less than the index of the FIRST zero
			for (int k=0; k<numNegZeros; k++)
				a[++j] = -0.0d;
		}
	}


	private static <T> void sort2(float a[], T[] a2, int fromIndex, int toIndex) {
		final int NEG_ZERO_BITS = Float.floatToIntBits(-0.0f);
		/*
		 * The sort is done in three phases to avoid the expense of using
		 * NaN and -0.0 aware comparisons during the main sort.
		 */

		/*
		 * Preprocessing phase:  Move any NaN's to end of array, count the
		 * number of -0.0's, and turn them into 0.0's.
		 */
		int numNegZeros = 0;
		int i = fromIndex, n = toIndex;
		while(i < n) {
			if (a[i] != a[i]) {
				float swap = a[i];
				a[i] = a[--n];
				a[n] = swap;
				T swap2 = a2[i];
				a2[i] = a2[n];
				a2[n] = swap2;
			} else {
				if (a[i]==0 && Float.floatToIntBits(a[i])==NEG_ZERO_BITS) {
					a[i] = 0.0f;
					numNegZeros++;
				}
				i++;
			}
		}

		// Main sort phase: quicksort everything but the NaN's
		sort1(a, a2, fromIndex, n-fromIndex);

		// Postprocessing phase: change 0.0's to -0.0's as required
		if (numNegZeros != 0) {
			int j = binarySearch0(a, fromIndex, n, 0.0f); // posn of ANY zero
			do {
				j--;
			} while (j>=0 && a[j]==0.0f);

			// j is now one less than the index of the FIRST zero
			for (int k=0; k<numNegZeros; k++)
				a[++j] = -0.0f;
		}
	}


	/*
	 * The code for each of the seven primitive types is largely identical.
	 * C'est la vie.
	 */

	/**
	 * Sorts the specified sub-array of longs into ascending order.
	 */
	private static <T> void sort1(long x[], T[] a2, int off, int len) {
		// Insertion sort on smallest arrays
		if (len < 7) {
			for (int i=off; i<len+off; i++)
				for (int j=i; j>off && x[j-1]>x[j]; j--)
					swap(x, a2, j, j-1);
			return;
		}

		// Choose a partition element, v
		int m = off + (len >> 1);       // Small arrays, middle element
		if (len > 7) {
			int l = off;
			int n = off + len - 1;
			if (len > 40) {        // Big arrays, pseudomedian of 9
				int s = len/8;
				l = med3(x, l,     l+s, l+2*s);
				m = med3(x, m-s,   m,   m+s);
				n = med3(x, n-2*s, n-s, n);
			}
			m = med3(x, l, m, n); // Mid-size, med of 3
		}
		long v = x[m];

		// Establish Invariant: v* (<v)* (>v)* v*
		int a = off, b = a, c = off + len - 1, d = c;
		while(true) {
			while (b <= c && x[b] <= v) {
				if (x[b] == v)
					swap(x,a2, a++, b);
				b++;
			}
			while (c >= b && x[c] >= v) {
				if (x[c] == v)
					swap(x,a2, c, d--);
				c--;
			}
			if (b > c)
				break;
			swap(x,a2, b++, c--);
		}

		// Swap partition elements back to middle
		int s, n = off + len;
		s = Math.min(a-off, b-a  );  vecswap(x,a2, off, b-s, s);
		s = Math.min(d-c,   n-d-1);  vecswap(x,a2, b,   n-s, s);

		// Recursively sort non-partition-elements
		if ((s = b-a) > 1)
			sort1(x,a2, off, s);
		if ((s = d-c) > 1)
			sort1(x,a2, n-s, s);
	}

	/**
	 * Swaps x[a] with x[b].
	 */
	private static <T> void swap(long x[], T[] a2, int a, int b) {
		long t = x[a];
		x[a] = x[b];
		x[b] = t;
		T t2 = a2[a];
		a2[a] = a2[b];
		a2[b] = t2;
	}

	/**
	 * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
	 */
	private static <T> void vecswap(long x[], T[] a2,int a, int b, int n) {
		for (int i=0; i<n; i++, a++, b++)
			swap(x,a2, a, b);
	}

	/**
	 * Returns the index of the median of the three indexed longs.
	 */
	private static int med3(long x[], int a, int b, int c) {
		return (x[a] < x[b] ?
				(x[b] < x[c] ? b : x[a] < x[c] ? c : a) :
					(x[b] > x[c] ? b : x[a] > x[c] ? c : a));
	}


	/**
	 * Sorts the specified sub-array of longs into ascending order.
	 */
	private static <T,P> void sort1(T x[], P[] a2, Comparator<T> comp, int off, int len) {
		// Insertion sort on smallest arrays
		if (len < 7) {
			for (int i=off; i<len+off; i++)
				for (int j=i; j>off && comp.compare(x[j-1],x[j])>0; j--)
					swap(x, a2, j, j-1);
			return;
		}

		// Choose a partition element, v
		int m = off + (len >> 1);       // Small arrays, middle element
		if (len > 7) {
			int l = off;
			int n = off + len - 1;
			if (len > 40) {        // Big arrays, pseudomedian of 9
				int s = len/8;
				l = med3(x,comp, l,     l+s, l+2*s);
				m = med3(x,comp, m-s,   m,   m+s);
				n = med3(x,comp, n-2*s, n-s, n);
			}
			m = med3(x,comp, l, m, n); // Mid-size, med of 3
		}
		T v = x[m];

		// Establish Invariant: v* (<v)* (>v)* v*
		int a = off, b = a, c = off + len - 1, d = c;
		while(true) {
			while (b <= c && comp.compare(x[b],v)<=0) {
				if (x[b] == v)
					swap(x,a2, a++, b);
				b++;
			}
			while (c >= b && comp.compare(x[c],v)>=0) {
				if (x[c] == v)
					swap(x,a2, c, d--);
				c--;
			}
			if (b > c)
				break;
			swap(x,a2, b++, c--);
		}

		// Swap partition elements back to middle
		int s, n = off + len;
		s = Math.min(a-off, b-a  );  vecswap(x,a2, off, b-s, s);
		s = Math.min(d-c,   n-d-1);  vecswap(x,a2, b,   n-s, s);

		// Recursively sort non-partition-elements
		if ((s = b-a) > 1)
			sort1(x,a2,comp, off, s);
		if ((s = d-c) > 1)
			sort1(x,a2,comp, n-s, s);
	}


	/**
	 * Sorts the specified sub-array of longs into ascending order.
	 */
	private static <T,P> void sort1(T x[], int[] a2, Comparator<T> comp, int off, int len) {
		// Insertion sort on smallest arrays
		if (len < 7) {
			for (int i=off; i<len+off; i++)
				for (int j=i; j>off && comp.compare(x[j-1],x[j])>0; j--)
					swap(x, a2, j, j-1);
			return;
		}

		// Choose a partition element, v
		int m = off + (len >> 1);       // Small arrays, middle element
		if (len > 7) {
			int l = off;
			int n = off + len - 1;
			if (len > 40) {        // Big arrays, pseudomedian of 9
				int s = len/8;
				l = med3(x,comp, l,     l+s, l+2*s);
				m = med3(x,comp, m-s,   m,   m+s);
				n = med3(x,comp, n-2*s, n-s, n);
			}
			m = med3(x,comp, l, m, n); // Mid-size, med of 3
		}
		T v = x[m];

		// Establish Invariant: v* (<v)* (>v)* v*
		int a = off, b = a, c = off + len - 1, d = c;
		while(true) {
			while (b <= c && comp.compare(x[b],v)<=0) {
				if (x[b] == v)
					swap(x,a2, a++, b);
				b++;
			}
			while (c >= b && comp.compare(x[c],v)>=0) {
				if (x[c] == v)
					swap(x,a2, c, d--);
				c--;
			}
			if (b > c)
				break;
			swap(x,a2, b++, c--);
		}

		// Swap partition elements back to middle
		int s, n = off + len;
		s = Math.min(a-off, b-a  );  vecswap(x,a2, off, b-s, s);
		s = Math.min(d-c,   n-d-1);  vecswap(x,a2, b,   n-s, s);

		// Recursively sort non-partition-elements
		if ((s = b-a) > 1)
			sort1(x,a2,comp, off, s);
		if ((s = d-c) > 1)
			sort1(x,a2,comp, n-s, s);
	}


	/**
	 * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
	 */
	private static <T,P> void vecswap(T x[], P[] a2,int a, int b, int n) {
		for (int i=0; i<n; i++, a++, b++)
			swap(x,a2, a, b);
	}

	/**
	 * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
	 */
	private static <T,P> void vecswap(T x[], int[] a2,int a, int b, int n) {
		for (int i=0; i<n; i++, a++, b++)
			swap(x,a2, a, b);
	}

	/**
	 * Returns the index of the median of the three indexed longs.
	 */
	private static <T> int med3(T x[], Comparator<T> comp, int a, int b, int c) {
		return (comp.compare(x[a],x[b])<0 ?
				(comp.compare(x[b],x[c])<0 ? b : comp.compare(x[a],x[c])<0 ? c : a) :
					(comp.compare(x[b],x[c])>0 ? b : comp.compare(x[a],x[c])>0 ? c : a));
	}

	/**
	 * Sorts the specified sub-array of integers into ascending order.
	 */
	private static <T> void sort1(int x[], T[] a2, int off, int len) {
		// Insertion sort on smallest arrays
		if (len < 7) {
			for (int i=off; i<len+off; i++)
				for (int j=i; j>off && x[j-1]>x[j]; j--)
					swap(x, a2, j, j-1);
			return;
		}

		// Choose a partition element, v
		int m = off + (len >> 1);       // Small arrays, middle element
		if (len > 7) {
			int l = off;
			int n = off + len - 1;
			if (len > 40) {        // Big arrays, pseudomedian of 9
				int s = len/8;
				l = med3(x, l,     l+s, l+2*s);
				m = med3(x, m-s,   m,   m+s);
				n = med3(x, n-2*s, n-s, n);
			}
			m = med3(x, l, m, n); // Mid-size, med of 3
		}
		int v = x[m];

		// Establish Invariant: v* (<v)* (>v)* v*
		int a = off, b = a, c = off + len - 1, d = c;
		while(true) {
			while (b <= c && x[b] <= v) {
				if (x[b] == v)
					swap(x,a2,a++, b);
				b++;
			}
			while (c >= b && x[c] >= v) {
				if (x[c] == v)
					swap(x, a2,c, d--);
				c--;
			}
			if (b > c)
				break;
			swap(x,a2, b++, c--);
		}

		// Swap partition elements back to middle
		int s, n = off + len;
		s = Math.min(a-off, b-a  );  vecswap(x,a2, off, b-s, s);
		s = Math.min(d-c,   n-d-1);  vecswap(x,a2, b,   n-s, s);

		// Recursively sort non-partition-elements
		if ((s = b-a) > 1)
			sort1(x, a2,off, s);
		if ((s = d-c) > 1)
			sort1(x, a2,n-s, s);
	}

	/**
	 * Sorts the specified sub-array of integers into ascending order.
	 */
	private static void sort1(int x[], double[] a2, int off, int len) {
		// Insertion sort on smallest arrays
		if (len < 7) {
			for (int i=off; i<len+off; i++)
				for (int j=i; j>off && x[j-1]>x[j]; j--)
					swap(x, a2, j, j-1);
			return;
		}

		// Choose a partition element, v
		int m = off + (len >> 1);       // Small arrays, middle element
		if (len > 7) {
			int l = off;
			int n = off + len - 1;
			if (len > 40) {        // Big arrays, pseudomedian of 9
				int s = len/8;
				l = med3(x, l,     l+s, l+2*s);
				m = med3(x, m-s,   m,   m+s);
				n = med3(x, n-2*s, n-s, n);
			}
			m = med3(x, l, m, n); // Mid-size, med of 3
		}
		int v = x[m];

		// Establish Invariant: v* (<v)* (>v)* v*
		int a = off, b = a, c = off + len - 1, d = c;
		while(true) {
			while (b <= c && x[b] <= v) {
				if (x[b] == v)
					swap(x,a2,a++, b);
				b++;
			}
			while (c >= b && x[c] >= v) {
				if (x[c] == v)
					swap(x, a2,c, d--);
				c--;
			}
			if (b > c)
				break;
			swap(x,a2, b++, c--);
		}

		// Swap partition elements back to middle
		int s, n = off + len;
		s = Math.min(a-off, b-a  );  vecswap(x,a2, off, b-s, s);
		s = Math.min(d-c,   n-d-1);  vecswap(x,a2, b,   n-s, s);

		// Recursively sort non-partition-elements
		if ((s = b-a) > 1)
			sort1(x, a2,off, s);
		if ((s = d-c) > 1)
			sort1(x, a2,n-s, s);
	}

	/**
	 * Sorts the specified sub-array of integers into ascending order.
	 */
	private static void sort1(int x[], int[] a2, int off, int len) {
		// Insertion sort on smallest arrays
		if (len < 7) {
			for (int i=off; i<len+off; i++)
				for (int j=i; j>off && x[j-1]>x[j]; j--)
					swap(x, a2, j, j-1);
			return;
		}

		// Choose a partition element, v
		int m = off + (len >> 1);       // Small arrays, middle element
		if (len > 7) {
			int l = off;
			int n = off + len - 1;
			if (len > 40) {        // Big arrays, pseudomedian of 9
				int s = len/8;
				l = med3(x, l,     l+s, l+2*s);
				m = med3(x, m-s,   m,   m+s);
				n = med3(x, n-2*s, n-s, n);
			}
			m = med3(x, l, m, n); // Mid-size, med of 3
		}
		int v = x[m];

		// Establish Invariant: v* (<v)* (>v)* v*
		int a = off, b = a, c = off + len - 1, d = c;
		while(true) {
			while (b <= c && x[b] <= v) {
				if (x[b] == v)
					swap(x,a2,a++, b);
				b++;
			}
			while (c >= b && x[c] >= v) {
				if (x[c] == v)
					swap(x, a2,c, d--);
				c--;
			}
			if (b > c)
				break;
			swap(x,a2, b++, c--);
		}

		// Swap partition elements back to middle
		int s, n = off + len;
		s = Math.min(a-off, b-a  );  vecswap(x,a2, off, b-s, s);
		s = Math.min(d-c,   n-d-1);  vecswap(x,a2, b,   n-s, s);

		// Recursively sort non-partition-elements
		if ((s = b-a) > 1)
			sort1(x, a2,off, s);
		if ((s = d-c) > 1)
			sort1(x, a2,n-s, s);
	}


	/**
	 * Swaps x[a] with x[b].
	 */
	private static <T> void swap(int x[], T[] a2, int a, int b) {
		int t = x[a];
		x[a] = x[b];
		x[b] = t;
		T t2 = a2[a];
		a2[a] = a2[b];
		a2[b] = t2;
	}

	/**
	 * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
	 */
	private static <T> void vecswap(int x[], T[] a2, int a, int b, int n) {
		for (int i=0; i<n; i++, a++, b++)
			swap(x, a2, a, b);
	}

	/**
	 * Swaps x[a] with x[b].
	 */
	private static <T> void swap(int x[], double[] a2, int a, int b) {
		int t = x[a];
		x[a] = x[b];
		x[b] = t;
		double t2 = a2[a];
		a2[a] = a2[b];
		a2[b] = t2;
	}

	/**
	 * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
	 */
	private static <T> void vecswap(int x[], double[] a2, int a, int b, int n) {
		for (int i=0; i<n; i++, a++, b++)
			swap(x, a2, a, b);
	}

	/**
	 * Swaps x[a] with x[b].
	 */
	private static  void swap(int x[], int[] a2, int a, int b) {
		int t = x[a];
		x[a] = x[b];
		x[b] = t;
		int t2 = a2[a];
		a2[a] = a2[b];
		a2[b] = t2;
	}

	/**
	 * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
	 */
	private static void vecswap(int x[], int[] a2, int a, int b, int n) {
		for (int i=0; i<n; i++, a++, b++)
			swap(x, a2, a, b);
	}

	/**
	 * Returns the index of the median of the three indexed integers.
	 */
	private static int med3(int x[], int a, int b, int c) {
		return (x[a] < x[b] ?
				(x[b] < x[c] ? b : x[a] < x[c] ? c : a) :
					(x[b] > x[c] ? b : x[a] > x[c] ? c : a));
	}

	/**
	 * Sorts the specified sub-array of shorts into ascending order.
	 */
	private static <T> void sort1(short x[], T[] a2, int off, int len) {
		// Insertion sort on smallest arrays
		if (len < 7) {
			for (int i=off; i<len+off; i++)
				for (int j=i; j>off && x[j-1]>x[j]; j--)
					swap(x, a2,j, j-1);
			return;
		}

		// Choose a partition element, v
		int m = off + (len >> 1);       // Small arrays, middle element
		if (len > 7) {
			int l = off;
			int n = off + len - 1;
			if (len > 40) {        // Big arrays, pseudomedian of 9
				int s = len/8;
				l = med3(x, l,     l+s, l+2*s);
				m = med3(x, m-s,   m,   m+s);
				n = med3(x, n-2*s, n-s, n);
			}
			m = med3(x, l, m, n); // Mid-size, med of 3
		}
		short v = x[m];

		// Establish Invariant: v* (<v)* (>v)* v*
		int a = off, b = a, c = off + len - 1, d = c;
		while(true) {
			while (b <= c && x[b] <= v) {
				if (x[b] == v)
					swap(x,a2, a++, b);
				b++;
			}
			while (c >= b && x[c] >= v) {
				if (x[c] == v)
					swap(x, a2,c, d--);
				c--;
			}
			if (b > c)
				break;
			swap(x,a2, b++, c--);
		}

		// Swap partition elements back to middle
		int s, n = off + len;
		s = Math.min(a-off, b-a  );  vecswap(x,a2, off, b-s, s);
		s = Math.min(d-c,   n-d-1);  vecswap(x,a2, b,   n-s, s);

		// Recursively sort non-partition-elements
		if ((s = b-a) > 1)
			sort1(x, a2,off, s);
		if ((s = d-c) > 1)
			sort1(x, a2,n-s, s);
	}

	/**
	 * Swaps x[a] with x[b].
	 */
	private static <T> void swap(short x[], T[] a2, int a, int b) {
		short t = x[a];
		x[a] = x[b];
		x[b] = t;
		T t2 = a2[a];
		a2[a] = a2[b];
		a2[b] = t2;
	}

	/**
	 * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
	 */
	private static <T> void vecswap(short x[], T[] a2, int a, int b, int n) {
		for (int i=0; i<n; i++, a++, b++)
			swap(x, a2, a, b);
	}

	/**
	 * Returns the index of the median of the three indexed shorts.
	 */
	private static int med3(short x[], int a, int b, int c) {
		return (x[a] < x[b] ?
				(x[b] < x[c] ? b : x[a] < x[c] ? c : a) :
					(x[b] > x[c] ? b : x[a] > x[c] ? c : a));
	}


	/**
	 * Sorts the specified sub-array of chars into ascending order.
	 */
	private static <T> void sort1(char x[], T[] a2, int off, int len) {
		// Insertion sort on smallest arrays
		if (len < 7) {
			for (int i=off; i<len+off; i++)
				for (int j=i; j>off && x[j-1]>x[j]; j--)
					swap(x,a2, j, j-1);
			return;
		}

		// Choose a partition element, v
		int m = off + (len >> 1);       // Small arrays, middle element
		if (len > 7) {
			int l = off;
			int n = off + len - 1;
			if (len > 40) {        // Big arrays, pseudomedian of 9
				int s = len/8;
				l = med3(x, l,     l+s, l+2*s);
				m = med3(x, m-s,   m,   m+s);
				n = med3(x, n-2*s, n-s, n);
			}
			m = med3(x, l, m, n); // Mid-size, med of 3
		}
		char v = x[m];

		// Establish Invariant: v* (<v)* (>v)* v*
		int a = off, b = a, c = off + len - 1, d = c;
		while(true) {
			while (b <= c && x[b] <= v) {
				if (x[b] == v)
					swap(x,a2, a++, b);
				b++;
			}
			while (c >= b && x[c] >= v) {
				if (x[c] == v)
					swap(x,a2, c, d--);
				c--;
			}
			if (b > c)
				break;
			swap(x,a2, b++, c--);
		}

		// Swap partition elements back to middle
		int s, n = off + len;
		s = Math.min(a-off, b-a  );  vecswap(x, a2,off, b-s, s);
		s = Math.min(d-c,   n-d-1);  vecswap(x, a2,b,   n-s, s);

		// Recursively sort non-partition-elements
		if ((s = b-a) > 1)
			sort1(x,a2, off, s);
		if ((s = d-c) > 1)
			sort1(x,a2, n-s, s);
	}


	/**
	 * Sorts the specified sub-array of chars into ascending order.
	 */
	private static <T> void sort1(char x[], char[] a2, int off, int len) {
		// Insertion sort on smallest arrays
		if (len < 7) {
			for (int i=off; i<len+off; i++)
				for (int j=i; j>off && x[j-1]>x[j]; j--)
					swap(x,a2, j, j-1);
			return;
		}

		// Choose a partition element, v
		int m = off + (len >> 1);       // Small arrays, middle element
		if (len > 7) {
			int l = off;
			int n = off + len - 1;
			if (len > 40) {        // Big arrays, pseudomedian of 9
				int s = len/8;
				l = med3(x, l,     l+s, l+2*s);
				m = med3(x, m-s,   m,   m+s);
				n = med3(x, n-2*s, n-s, n);
			}
			m = med3(x, l, m, n); // Mid-size, med of 3
		}
		char v = x[m];

		// Establish Invariant: v* (<v)* (>v)* v*
		int a = off, b = a, c = off + len - 1, d = c;
		while(true) {
			while (b <= c && x[b] <= v) {
				if (x[b] == v)
					swap(x,a2, a++, b);
				b++;
			}
			while (c >= b && x[c] >= v) {
				if (x[c] == v)
					swap(x,a2, c, d--);
				c--;
			}
			if (b > c)
				break;
			swap(x,a2, b++, c--);
		}

		// Swap partition elements back to middle
		int s, n = off + len;
		s = Math.min(a-off, b-a  );  vecswap(x, a2,off, b-s, s);
		s = Math.min(d-c,   n-d-1);  vecswap(x, a2,b,   n-s, s);

		// Recursively sort non-partition-elements
		if ((s = b-a) > 1)
			sort1(x,a2, off, s);
		if ((s = d-c) > 1)
			sort1(x,a2, n-s, s);
	}


	/**
	 * Swaps x[a] with x[b].
	 */
	private static <T> void swap(char x[], T[] a2, int a, int b) {
		char t = x[a];
		x[a] = x[b];
		x[b] = t;
		T t2 = a2[a];
		a2[a] = a2[b];
		a2[b] = t2;
	}


	/**
	 * Swaps x[a] with x[b].
	 */
	private static void swap(char x[], char[] a2, int a, int b) {
		char t = x[a];
		x[a] = x[b];
		x[b] = t;
		char t2 = a2[a];
		a2[a] = a2[b];
		a2[b] = t2;
	}

	/**
	 * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
	 */
	private static <T> void vecswap(char x[], T[] a2, int a, int b, int n) {
		for (int i=0; i<n; i++, a++, b++)
			swap(x, a2, a, b);
	}

	/**
	 * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
	 */
	private static void vecswap(char x[], char[] a2, int a, int b, int n) {
		for (int i=0; i<n; i++, a++, b++)
			swap(x, a2, a, b);
	}

	/**
	 * Returns the index of the median of the three indexed chars.
	 */
	private static int med3(char x[], int a, int b, int c) {
		return (x[a] < x[b] ?
				(x[b] < x[c] ? b : x[a] < x[c] ? c : a) :
					(x[b] > x[c] ? b : x[a] > x[c] ? c : a));
	}


	/**
	 * Sorts the specified sub-array of bytes into ascending order.
	 */
	private static <T> void sort1(byte x[], T[] a2, int off, int len) {
		// Insertion sort on smallest arrays
		if (len < 7) {
			for (int i=off; i<len+off; i++)
				for (int j=i; j>off && x[j-1]>x[j]; j--)
					swap(x, a2, j, j-1);
			return;
		}

		// Choose a partition element, v
		int m = off + (len >> 1);       // Small arrays, middle element
		if (len > 7) {
			int l = off;
			int n = off + len - 1;
			if (len > 40) {        // Big arrays, pseudomedian of 9
				int s = len/8;
				l = med3(x, l,     l+s, l+2*s);
				m = med3(x, m-s,   m,   m+s);
				n = med3(x, n-2*s, n-s, n);
			}
			m = med3(x, l, m, n); // Mid-size, med of 3
		}
		byte v = x[m];

		// Establish Invariant: v* (<v)* (>v)* v*
		int a = off, b = a, c = off + len - 1, d = c;
		while(true) {
			while (b <= c && x[b] <= v) {
				if (x[b] == v)
					swap(x,a2,  a++, b);
				b++;
			}
			while (c >= b && x[c] >= v) {
				if (x[c] == v)
					swap(x,a2,  c, d--);
				c--;
			}
			if (b > c)
				break;
			swap(x,a2, b++, c--);
		}

		// Swap partition elements back to middle
		int s, n = off + len;
		s = Math.min(a-off, b-a  );  vecswap(x,a2, off, b-s, s);
		s = Math.min(d-c,   n-d-1);  vecswap(x,a2, b,   n-s, s);

		// Recursively sort non-partition-elements
		if ((s = b-a) > 1)
			sort1(x,a2, off, s);
		if ((s = d-c) > 1)
			sort1(x,a2, n-s, s);
	}

	/**
	 * Swaps x[a] with x[b].
	 */
	private static <T> void swap(byte x[],T[] a2, int a, int b) {
		byte t = x[a];
		x[a] = x[b];
		x[b] = t;
		T t2 = a2[a];
		a2[a] = a2[b];
		a2[b] = t2;
	}

	/**
	 * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
	 */
	private static <T> void vecswap(byte x[], T[] a2, int a, int b, int n) {
		for (int i=0; i<n; i++, a++, b++)
			swap(x, a2, a, b);
	}

	/**
	 * Returns the index of the median of the three indexed bytes.
	 */
	private static int med3(byte x[], int a, int b, int c) {
		return (x[a] < x[b] ?
				(x[b] < x[c] ? b : x[a] < x[c] ? c : a) :
					(x[b] > x[c] ? b : x[a] > x[c] ? c : a));
	}


	/**
	 * Sorts the specified sub-array of doubles into ascending order.
	 */
	private static <T> void sort1(double x[], T[] a2, int off, int len) {
		// Insertion sort on smallest arrays
		if (len < 7) {
			for (int i=off; i<len+off; i++)
				for (int j=i; j>off && x[j-1]>x[j]; j--)
					swap(x,a2, j, j-1);
			return;
		}

		// Choose a partition element, v
		int m = off + (len >> 1);       // Small arrays, middle element
		if (len > 7) {
			int l = off;
			int n = off + len - 1;
			if (len > 40) {        // Big arrays, pseudomedian of 9
				int s = len/8;
				l = med3(x, l,     l+s, l+2*s);
				m = med3(x, m-s,   m,   m+s);
				n = med3(x, n-2*s, n-s, n);
			}
			m = med3(x, l, m, n); // Mid-size, med of 3
		}
		double v = x[m];

		// Establish Invariant: v* (<v)* (>v)* v*
		int a = off, b = a, c = off + len - 1, d = c;
		while(true) {
			while (b <= c && x[b] <= v) {
				if (x[b] == v)
					swap(x,a2, a++, b);
				b++;
			}
			while (c >= b && x[c] >= v) {
				if (x[c] == v)
					swap(x,a2, c, d--);
				c--;
			}
			if (b > c)
				break;
			swap(x,a2, b++, c--);
		}

		// Swap partition elements back to middle
		int s, n = off + len;
		s = Math.min(a-off, b-a  );  vecswap(x, a2,off, b-s, s);
		s = Math.min(d-c,   n-d-1);  vecswap(x, a2,b,   n-s, s);

		// Recursively sort non-partition-elements
		if ((s = b-a) > 1)
			sort1(x,a2, off, s);
		if ((s = d-c) > 1)
			sort1(x,a2, n-s, s);
	}


	/**
	 * Sorts the specified sub-array of doubles into ascending order.
	 */
	private static void sort1(double x[], double[] a2, int off, int len) {
		// Insertion sort on smallest arrays
		if (len < 7) {
			for (int i=off; i<len+off; i++)
				for (int j=i; j>off && x[j-1]>x[j]; j--)
					swap(x,a2, j, j-1);
			return;
		}

		// Choose a partition element, v
		int m = off + (len >> 1);       // Small arrays, middle element
		if (len > 7) {
			int l = off;
			int n = off + len - 1;
			if (len > 40) {        // Big arrays, pseudomedian of 9
				int s = len/8;
				l = med3(x, l,     l+s, l+2*s);
				m = med3(x, m-s,   m,   m+s);
				n = med3(x, n-2*s, n-s, n);
			}
			m = med3(x, l, m, n); // Mid-size, med of 3
		}
		double v = x[m];

		// Establish Invariant: v* (<v)* (>v)* v*
		int a = off, b = a, c = off + len - 1, d = c;
		while(true) {
			while (b <= c && x[b] <= v) {
				if (x[b] == v)
					swap(x,a2, a++, b);
				b++;
			}
			while (c >= b && x[c] >= v) {
				if (x[c] == v)
					swap(x,a2, c, d--);
				c--;
			}
			if (b > c)
				break;
			swap(x,a2, b++, c--);
		}

		// Swap partition elements back to middle
		int s, n = off + len;
		s = Math.min(a-off, b-a  );  vecswap(x, a2,off, b-s, s);
		s = Math.min(d-c,   n-d-1);  vecswap(x, a2,b,   n-s, s);

		// Recursively sort non-partition-elements
		if ((s = b-a) > 1)
			sort1(x,a2, off, s);
		if ((s = d-c) > 1)
			sort1(x,a2, n-s, s);
	}

	/**
	 * Sorts the specified sub-array of doubles into ascending order.
	 */
	private static void sort1(double x[], int[] a2, int off, int len) {
		// Insertion sort on smallest arrays
		if (len < 7) {
			for (int i=off; i<len+off; i++)
				for (int j=i; j>off && x[j-1]>x[j]; j--)
					swap(x,a2, j, j-1);
			return;
		}

		// Choose a partition element, v
		int m = off + (len >> 1);       // Small arrays, middle element
		if (len > 7) {
			int l = off;
			int n = off + len - 1;
			if (len > 40) {        // Big arrays, pseudomedian of 9
				int s = len/8;
				l = med3(x, l,     l+s, l+2*s);
				m = med3(x, m-s,   m,   m+s);
				n = med3(x, n-2*s, n-s, n);
			}
			m = med3(x, l, m, n); // Mid-size, med of 3
		}
		double v = x[m];

		// Establish Invariant: v* (<v)* (>v)* v*
		int a = off, b = a, c = off + len - 1, d = c;
		while(true) {
			while (b <= c && x[b] <= v) {
				if (x[b] == v)
					swap(x,a2, a++, b);
				b++;
			}
			while (c >= b && x[c] >= v) {
				if (x[c] == v)
					swap(x,a2, c, d--);
				c--;
			}
			if (b > c)
				break;
			swap(x,a2, b++, c--);
		}

		// Swap partition elements back to middle
		int s, n = off + len;
		s = Math.min(a-off, b-a  );  vecswap(x, a2,off, b-s, s);
		s = Math.min(d-c,   n-d-1);  vecswap(x, a2,b,   n-s, s);

		// Recursively sort non-partition-elements
		if ((s = b-a) > 1)
			sort1(x,a2, off, s);
		if ((s = d-c) > 1)
			sort1(x,a2, n-s, s);
	}

	/**
	 * Swaps x[a] with x[b].
	 */
	private static <T> void swap(double x[], T[] a2, int a, int b) {
		double t = x[a];
		x[a] = x[b];
		x[b] = t;
		T t2 = a2[a];
		a2[a] = a2[b];
		a2[b] = t2;
	}

	/**
	 * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
	 */
	private static <T> void vecswap(double x[], T[] a2, int a, int b, int n) {
		for (int i=0; i<n; i++, a++, b++)
			swap(x,a2, a, b);
	}


	/**
	 * Swaps x[a] with x[b].
	 */
	private static void swap(double x[], int[] a2, int a, int b) {
		double t = x[a];
		x[a] = x[b];
		x[b] = t;
		int t2 = a2[a];
		a2[a] = a2[b];
		a2[b] = t2;
	}

	/**
	 * Swaps x[a] with x[b].
	 */
	private static void swap(double x[], double[] a2, int a, int b) {
		double t = x[a];
		x[a] = x[b];
		x[b] = t;
		double t2 = a2[a];
		a2[a] = a2[b];
		a2[b] = t2;
	}

	/**
	 * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
	 */
	private static void vecswap(double x[], double[] a2, int a, int b, int n) {
		for (int i=0; i<n; i++, a++, b++)
			swap(x,a2, a, b);
	}

	/**
	 * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
	 */
	private static void vecswap(double x[], int[] a2, int a, int b, int n) {
		for (int i=0; i<n; i++, a++, b++)
			swap(x,a2, a, b);
	}

	/**
	 * Returns the index of the median of the three indexed doubles.
	 */
	private static int med3(double x[], int a, int b, int c) {
		return (x[a] < x[b] ?
				(x[b] < x[c] ? b : x[a] < x[c] ? c : a) :
					(x[b] > x[c] ? b : x[a] > x[c] ? c : a));
	}


	/**
	 * Sorts the specified sub-array of floats into ascending order.
	 */
	private static <T> void sort1(float x[], T[] a2, int off, int len) {
		// Insertion sort on smallest arrays
		if (len < 7) {
			for (int i=off; i<len+off; i++)
				for (int j=i; j>off && x[j-1]>x[j]; j--)
					swap(x, a2,j, j-1);
			return;
		}

		// Choose a partition element, v
		int m = off + (len >> 1);       // Small arrays, middle element
		if (len > 7) {
			int l = off;
			int n = off + len - 1;
			if (len > 40) {        // Big arrays, pseudomedian of 9
				int s = len/8;
				l = med3(x, l,     l+s, l+2*s);
				m = med3(x, m-s,   m,   m+s);
				n = med3(x, n-2*s, n-s, n);
			}
			m = med3(x, l, m, n); // Mid-size, med of 3
		}
		float v = x[m];

		// Establish Invariant: v* (<v)* (>v)* v*
		int a = off, b = a, c = off + len - 1, d = c;
		while(true) {
			while (b <= c && x[b] <= v) {
				if (x[b] == v)
					swap(x,a2, a++, b);
				b++;
			}
			while (c >= b && x[c] >= v) {
				if (x[c] == v)
					swap(x,a2, c, d--);
				c--;
			}
			if (b > c)
				break;
			swap(x, a2,b++, c--);
		}

		// Swap partition elements back to middle
		int s, n = off + len;
		s = Math.min(a-off, b-a  );  vecswap(x,a2, off, b-s, s);
		s = Math.min(d-c,   n-d-1);  vecswap(x,a2, b,   n-s, s);

		// Recursively sort non-partition-elements
		if ((s = b-a) > 1)
			sort1(x,a2, off, s);
		if ((s = d-c) > 1)
			sort1(x,a2, n-s, s);
	}

	/**
	 * Swaps x[a] with x[b].
	 */
	private static <T> void swap(float x[], T[] a2,int a, int b) {
		float t = x[a];
		x[a] = x[b];
		x[b] = t;
		T t2 = a2[a];
		a2[a] = a2[b];
		a2[b] = t2;
	}

	/**
	 * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
	 */
	private static <T> void vecswap(float x[],T[] a2, int a, int b, int n) {
		for (int i=0; i<n; i++, a++, b++)
			swap(x, a2, a, b);
	}

	/**
	 * Returns the index of the median of the three indexed floats.
	 */
	private static int med3(float x[], int a, int b, int c) {
		return (x[a] < x[b] ?
				(x[b] < x[c] ? b : x[a] < x[c] ? c : a) :
					(x[b] > x[c] ? b : x[a] > x[c] ? c : a));
	}


	// Like public version, but without range checks.
	private static int binarySearch0(float[] a, int fromIndex, int toIndex,
			float key) {
		int low = fromIndex;
		int high = toIndex - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
		float midVal = a[mid];

		int cmp;
		if (midVal < key) {
			cmp = -1;   // Neither val is NaN, thisVal is smaller
		} else if (midVal > key) {
			cmp = 1;    // Neither val is NaN, thisVal is larger
		} else {
			int midBits = Float.floatToIntBits(midVal);
			int keyBits = Float.floatToIntBits(key);
			cmp = (midBits == keyBits ?  0 : // Values are equal
				(midBits < keyBits ? -1 : // (-0.0, 0.0) or (!NaN, NaN)
					1));                     // (0.0, -0.0) or (NaN, !NaN)
		}

		if (cmp < 0)
			low = mid + 1;
		else if (cmp > 0)
			high = mid - 1;
		else
			return mid; // key found
		}
		return -(low + 1);  // key not found.
	}

	// Like public version, but without range checks.
	private static int binarySearch0(double[] a, int fromIndex, int toIndex,
			double key) {
		int low = fromIndex;
		int high = toIndex - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
				double midVal = a[mid];

				int cmp;
				if (midVal < key) {
					cmp = -1;   // Neither val is NaN, thisVal is smaller
				} else if (midVal > key) {
					cmp = 1;    // Neither val is NaN, thisVal is larger
				} else {
					long midBits = Double.doubleToLongBits(midVal);
					long keyBits = Double.doubleToLongBits(key);
					cmp = (midBits == keyBits ?  0 : // Values are equal
						(midBits < keyBits ? -1 : // (-0.0, 0.0) or (!NaN, NaN)
							1));                     // (0.0, -0.0) or (NaN, !NaN)
				}

				if (cmp < 0)
					low = mid + 1;
				else if (cmp > 0)
					high = mid - 1;
				else
					return mid; // key found
		}
		return -(low + 1);  // key not found.
	}

	// Like public version, but without range checks.
	public static int binarySearchReversed(int[] a, int fromIndex, int toIndex,
			int key) {
		int low = fromIndex;
		int high = toIndex - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
						int midVal = a[mid];

						if (midVal > key)
							low = mid + 1;
						else if (midVal < key)
							high = mid - 1;
						else
							return mid; // key found
		}
		return -(low + 1);  // key not found.
	}

	/**
	 * Sorts the specified array of objects into ascending order, according to
	 * the {@linkplain Comparable natural ordering}
	 * of its elements.  All elements in the array
	 * must implement the {@link Comparable} interface.  Furthermore, all
	 * elements in the array must be <i>mutually comparable</i> (that is,
	 * <tt>e1.compareTo(e2)</tt> must not throw a <tt>ClassCastException</tt>
	 * for any elements <tt>e1</tt> and <tt>e2</tt> in the array).<p>
	 *
	 * This sort is guaranteed to be <i>stable</i>:  equal elements will
	 * not be reordered as a result of the sort.<p>
	 *
	 * The sorting algorithm is a modified mergesort (in which the merge is
	 * omitted if the highest element in the low sublist is less than the
	 * lowest element in the high sublist).  This algorithm offers guaranteed
	 * n*log(n) performance.
	 *
	 * @param a the array to be sorted
	 * @throws  ClassCastException if the array contains elements that are not
	 *		<i>mutually comparable</i> (for example, strings and integers).
	 */
	public static <T extends Comparable,S> void sort(T[] a, S[] a2) {
		if (a.length!=a2.length)
			throw new IllegalArgumentException("Arrays don't have same length!");
		T[] aux = (T[])a.clone();
		S[] aux2 = (S[])a.clone();
		mergeSort(aux,aux2,a,a2, 0, a.length, 0);
	}

	/**
	 * Sorts the specified range of the specified array of objects into
	 * ascending order, according to the
	 * {@linkplain Comparable natural ordering} of its
	 * elements.  The range to be sorted extends from index
	 * <tt>fromIndex</tt>, inclusive, to index <tt>toIndex</tt>, exclusive.
	 * (If <tt>fromIndex==toIndex</tt>, the range to be sorted is empty.)  All
	 * elements in this range must implement the {@link Comparable}
	 * interface.  Furthermore, all elements in this range must be <i>mutually
	 * comparable</i> (that is, <tt>e1.compareTo(e2)</tt> must not throw a
	 * <tt>ClassCastException</tt> for any elements <tt>e1</tt> and
	 * <tt>e2</tt> in the array).<p>
	 *
	 * This sort is guaranteed to be <i>stable</i>:  equal elements will
	 * not be reordered as a result of the sort.<p>
	 *
	 * The sorting algorithm is a modified mergesort (in which the merge is
	 * omitted if the highest element in the low sublist is less than the
	 * lowest element in the high sublist).  This algorithm offers guaranteed
	 * n*log(n) performance.
	 *
	 * @param a the array to be sorted
	 * @param fromIndex the index of the first element (inclusive) to be
	 *        sorted
	 * @param toIndex the index of the last element (exclusive) to be sorted
	 * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	 * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	 *	       <tt>toIndex &gt; a.length</tt>
	 * @throws    ClassCastException if the array contains elements that are
	 *		  not <i>mutually comparable</i> (for example, strings and
	 *		  integers).
	 */
	public static <T extends Comparable,S> void sort(T[] a, S[] a2, int fromIndex, int toIndex) {
		rangeCheck(a.length, fromIndex, toIndex);
		rangeCheck(a2.length, fromIndex, toIndex);
		T[] aux = Arrays.copyOfRange(a, fromIndex, toIndex);
		S[] aux2 = Arrays.copyOfRange(a2, fromIndex, toIndex);
		mergeSort(aux,aux2, a, a2,fromIndex, toIndex, -fromIndex);
	}

	/**
	 * Tuning parameter: list size at or below which insertion sort will be
	 * used in preference to mergesort or quicksort.
	 */
	private static final int INSERTIONSORT_THRESHOLD = 7;

	/**
	 * Src is the source array that starts at index 0
	 * Dest is the (possibly larger) array destination with a possible offset
	 * low is the index in dest to start sorting
	 * high is the end index in dest to end sorting
	 * off is the offset to generate corresponding low, high in src
	 */
	private static  <T extends Comparable,S> void mergeSort(T[] src, S[] src2,
			T[] dest,
			S[] dest2,
			int low,
			int high,
			int off) {
		int length = high - low;

		// Insertion sort on smallest arrays
		if (length < INSERTIONSORT_THRESHOLD) {
			for (int i=low; i<high; i++)
				for (int j=i; j>low &&
						dest[j-1].compareTo(dest[j])>0; j--)
					swap(dest,dest2, j, j-1);
			return;
		}

		// Recursively sort halves of dest into src
		int destLow  = low;
		int destHigh = high;
		low  += off;
		high += off;
		int mid = (low + high) >>> 1;
		mergeSort(dest, dest2, src, src2,low, mid, -off);
		mergeSort(dest, dest2, src, src2, mid, high, -off);

		// If list is already sorted, just copy from src to dest.  This is an
		// optimization that results in faster sorts for nearly ordered lists.
		if (src[mid-1].compareTo(src[mid]) <= 0) {
			System.arraycopy(src, low, dest, destLow, length);
			System.arraycopy(src2, low, dest2, destLow, length);
			return;
		}

		// Merge sorted halves (now in src) into dest
		for(int i = destLow, p = low, q = mid; i < destHigh; i++) {
			if (q >= high || p < mid && src[p].compareTo(src[q])<=0) {
				dest2[i] = src2[p];
				dest[i] = src[p++];
			}
			else {
				dest2[i] = src2[q];
				dest[i] = src[q++];
			}
		}
	}

	/**
	 * Sorts the specified array of objects according to the order induced by
	 * the specified comparator.  All elements in the array must be
	 * <i>mutually comparable</i> by the specified comparator (that is,
	 * <tt>c.compare(e1, e2)</tt> must not throw a <tt>ClassCastException</tt>
	 * for any elements <tt>e1</tt> and <tt>e2</tt> in the array).<p>
	 *
	 * This sort is guaranteed to be <i>stable</i>:  equal elements will
	 * not be reordered as a result of the sort.<p>
	 *
	 * The sorting algorithm is a modified mergesort (in which the merge is
	 * omitted if the highest element in the low sublist is less than the
	 * lowest element in the high sublist).  This algorithm offers guaranteed
	 * n*log(n) performance.
	 *
	 * @param a the array to be sorted
	 * @param c the comparator to determine the order of the array.  A
	 *        <tt>null</tt> value indicates that the elements'
	 *        {@linkplain Comparable natural ordering} should be used.
	 * @throws  ClassCastException if the array contains elements that are
	 *		not <i>mutually comparable</i> using the specified comparator.
	 */
	public static <T,S> void sort(T[] a, S[] a2,Comparator<? super T> c) {
		T[] aux = (T[])a.clone();
		S[] aux2 = (S[])a2.clone();
		mergeSort(aux, aux2, a, a2, 0, a.length, 0, c);
	}

	/**
	 * Sorts the specified range of the specified array of objects according
	 * to the order induced by the specified comparator.  The range to be
	 * sorted extends from index <tt>fromIndex</tt>, inclusive, to index
	 * <tt>toIndex</tt>, exclusive.  (If <tt>fromIndex==toIndex</tt>, the
	 * range to be sorted is empty.)  All elements in the range must be
	 * <i>mutually comparable</i> by the specified comparator (that is,
	 * <tt>c.compare(e1, e2)</tt> must not throw a <tt>ClassCastException</tt>
	 * for any elements <tt>e1</tt> and <tt>e2</tt> in the range).<p>
	 *
	 * This sort is guaranteed to be <i>stable</i>:  equal elements will
	 * not be reordered as a result of the sort.<p>
	 *
	 * The sorting algorithm is a modified mergesort (in which the merge is
	 * omitted if the highest element in the low sublist is less than the
	 * lowest element in the high sublist).  This algorithm offers guaranteed
	 * n*log(n) performance.
	 *
	 * @param a the array to be sorted
	 * @param fromIndex the index of the first element (inclusive) to be
	 *        sorted
	 * @param toIndex the index of the last element (exclusive) to be sorted
	 * @param c the comparator to determine the order of the array.  A
	 *        <tt>null</tt> value indicates that the elements'
	 *        {@linkplain Comparable natural ordering} should be used.
	 * @throws ClassCastException if the array contains elements that are not
	 *	       <i>mutually comparable</i> using the specified comparator.
	 * @throws IllegalArgumentException if <tt>fromIndex &gt; toIndex</tt>
	 * @throws ArrayIndexOutOfBoundsException if <tt>fromIndex &lt; 0</tt> or
	 *	       <tt>toIndex &gt; a.length</tt>
	 */
	public static <T,S> void sort(T[] a, S[] a2, int fromIndex, int toIndex,
			Comparator<? super T> c) {
		rangeCheck(a.length, fromIndex, toIndex);
		rangeCheck(a2.length, fromIndex, toIndex);
		T[] aux = (T[])Arrays.copyOfRange(a, fromIndex, toIndex);
		S[] aux2= (S[])Arrays.copyOfRange(a2, fromIndex, toIndex);
		mergeSort(aux, aux2, a, a2, fromIndex, toIndex, -fromIndex, c);
	}

	/**
	 * Src is the source array that starts at index 0
	 * Dest is the (possibly larger) array destination with a possible offset
	 * low is the index in dest to start sorting
	 * high is the end index in dest to end sorting
	 * off is the offset into src corresponding to low in dest
	 */
	private static <T,S>void mergeSort(T[] src,S[] src2,
			T[] dest,
			S[] dest2,
			int low, int high, int off,
			Comparator c) {
		int length = high - low;

		// Insertion sort on smallest arrays
		if (length < INSERTIONSORT_THRESHOLD) {
			for (int i=low; i<high; i++)
				for (int j=i; j>low && c.compare(dest[j-1], dest[j])>0; j--) 
					swap(dest,dest2,j, j-1);
			return;
		}

		// Recursively sort halves of dest into src
		int destLow  = low;
		int destHigh = high;
		low  += off;
		high += off;
		int mid = (low + high) >>> 1;
			mergeSort(dest,dest2, src, src2,low, mid, -off, c);
			mergeSort(dest,dest2, src, src2, mid, high, -off, c);

			// If list is already sorted, just copy from src to dest.  This is an
			// optimization that results in faster sorts for nearly ordered lists.
			if (c.compare(src[mid-1], src[mid]) <= 0) {
				System.arraycopy(src, low, dest, destLow, length);
				System.arraycopy(src2, low, dest2, destLow, length);
				return;
			}

			// Merge sorted halves (now in src) into dest
			for(int i = destLow, p = low, q = mid; i < destHigh; i++) {
				if (q >= high || p < mid && c.compare(src[p], src[q]) <= 0) {
					dest2[i] = src2[p];
					dest[i] = src[p++];
				}
				else {
					dest2[i] = src2[q];
					dest[i] = src[q++];
				}
			}
	}

	/**
	 * Swaps x[a] with x[b].
	 */
	private static <T,S> void swap(T x[], S[] a2, int a, int b) {
		T t = x[a];
		x[a] = x[b];
		x[b] = t;
		S t2 = a2[a];
		a2[a] = a2[b];
		a2[b] = t2;
	}
	/**
	 * Swaps x[a] with x[b].
	 */
	private static <T,S> void swap(T x[], int[] a2, int a, int b) {
		T t = x[a];
		x[a] = x[b];
		x[b] = t;
		int t2 = a2[a];
		a2[a] = a2[b];
		a2[b] = t2;
	}

	/**
	 * Check that fromIndex and toIndex are in range, and throw an
	 * appropriate exception if they aren't.
	 */
	private static void rangeCheck(int arrayLen, int fromIndex, int toIndex) {
		if (fromIndex > toIndex)
			throw new IllegalArgumentException("fromIndex(" + fromIndex +
					") > toIndex(" + toIndex+")");
		if (fromIndex < 0)
			throw new ArrayIndexOutOfBoundsException(fromIndex);
		if (toIndex > arrayLen)
			throw new ArrayIndexOutOfBoundsException(toIndex);
	}

	/**
	 * Stores each element of list in a {@link Map}, the key is selected using the given transformer.
	 * 
	 * 
	 * @see #listToMultiMap(List, Transformer)
	 * @param <TKey>
	 * @param <TVal>
	 * @param list
	 * @param keySelector
	 * @return
	 */
	public static <TKey,TVal> TreeMap<TKey, TVal> toMap(
			TVal[] array,
			Function<TVal, TKey> keySelector) {
		TreeMap<TKey, TVal> re = new TreeMap<TKey, TVal>();
		for (TVal v : array) 
			re.put(keySelector.apply(v), v);
		return re;
	}

	public static int[][] removeColumnsAndRows(int[][] matrix,boolean[] rows, boolean[] cols) {
		int[] rowCum = new int[rows.length];
		rowCum[0] = (rows[0]?0:1);
		for (int i=1; i<rows.length; i++)
			rowCum[i] = rowCum[i-1]+(rows[i]?0:1);

		int[] colCum = new int[cols.length];
		colCum[0] = (cols[0]?0:1);
		for (int i=1; i<cols.length; i++)
			colCum[i] = colCum[i-1]+(cols[i]?0:1);

		int[][] re = new int[rowCum[rowCum.length-1]][colCum[colCum.length-1]];
		for (int i=0; i<matrix.length; i++)
			if (!rows[i])
				for (int j=0; j<matrix[i].length; j++) {
					if (!cols[j])
						re[rowCum[i]-1][colCum[j]-1] = matrix[i][j];
				}
		return re;
	}

	public static float[][] transpose(float[][] m) {
		float[][] re = new float[m[0].length][m.length];
		for (int i=0; i<m.length; i++)
			for (int j=0; j<m[i].length; j++)
				re[j][i] = m[i][j];
		return re;
	}

	public static double[][] transpose(double[][] m) {
		double[][] re = new double[m[0].length][m.length];
		for (int i=0; i<m.length; i++)
			for (int j=0; j<m[i].length; j++)
				re[j][i] = m[i][j];
		return re;
	}

	public static float[] getRowSums(float[][] matrix) {
		float[] re = new float[matrix.length];
		for (int i=0; i<re.length; i++)
			re[i] = sum(matrix[i]);
		return re;
	}

	public static float[] getRowMaxs(float[][] matrix) {
		float[] re = new float[matrix.length];
		for (int i=0; i<re.length; i++)
			re[i] = max(matrix[i]);
		return re;
	}

	public static int compare(int[] a1, int[] a2) {
		int n = Math.min(a1.length,a2.length);
		for (int i=0; i<n; i++) {
			int r = a1[i]-a2[i];
			if (r!=0)
				return r;
		}
		return a1.length-a2.length;
	}


	public static int compare(char[] a1, char[] a2) {
		int n = Math.min(a1.length,a2.length);
		for (int i=0; i<n; i++) {
			int r = a1[i]-a2[i];
			if (r!=0)
				return r;
		}
		return a1.length-a2.length;
	}


	public static int compare(double[] a1, double[] a2, double eps) {
		int n = Math.min(a1.length,a2.length);
		for (int i=0; i<n; i++) {
			double r = a1[i]-a2[i];
			if (Math.abs(r)>eps)
				return (int)Math.signum(r);
		}
		return a1.length-a2.length;
	}

	public static <T> Collection<T> asList(T[] objects, int start, int end) {
		return new ArraySegmentList<T>(objects,start,end);
	}

	private static class ArraySegmentList<E> extends AbstractList<E> implements RandomAccess, java.io.Serializable
	{
		private static final long serialVersionUID = -2764017381108945198L;
		private final E[] a;
		private final int start;
		private final int end;

		ArraySegmentList(E[] array, int start, int end) {
			if (array==null)
				throw new NullPointerException();
			this.start = start;
			this.end = end;
			a = array;
		}

		public int size() {
			return end-start;
		}

		public Object[] toArray() {
			return Arrays.copyOfRange(a, start, end);
		}

		public <T> T[] toArray(T[] a) {
			int size = size();
			if (a.length < size)
				return Arrays.copyOfRange(this.a, start, end,
						(Class<? extends T[]>) a.getClass());
			System.arraycopy(this.a, start, a, 0, size);
			if (a.length > size)
				a[size] = null;
			return a;
		}

		public E get(int index) {
			return a[start+index];
		}

		public E set(int index, E element) {
			E oldValue = a[start+index];
			a[start+index] = element;
			return oldValue;
		}

		public int indexOf(Object o) {
			if (o==null) {
				for (int i=start; i<end; i++)
					if (a[i]==null)
						return i-start;
			} else {
				for (int i=start; i<end; i++)
					if (o.equals(a[i]))
						return i-start;
			}
			return -1;
		}

		public boolean contains(Object o) {
			return indexOf(o) != -1;
		}
	}

	public static double[] mergeSorted(double[] a, double[] b) {
		double[] re = new double[a.length+b.length];

		int i1=0, i2=0;
		while (i1<a.length && i2<b.length) 
			re[i1+i2] = a[i1]<b[i2] ? a[i1++] : b[i2++];

			for (; i1<a.length; i1++)
				re[i1+i2] = a[i1];

			for (; i2<b.length; i2++)
				re[i1+i2] = b[i2];

			return re;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Comparable<T>> T[] mergeSorted(T[] a, T[] b) {
		T[] re = (T[]) Array.newInstance(a.getClass().getComponentType(), a.length+b.length);

		int i1=0, i2=0;
		while (i1<a.length && i2<b.length) 
			re[i1+i2] = a[i1].compareTo(b[i2])<0 ? a[i1++] : b[i2++];

			for (; i1<a.length; i1++)
				re[i1+i2] = a[i1];

			for (; i2<b.length; i2++)
				re[i1+i2] = b[i2];

			return re;
	}

	/**
	 * Projects only true entries from a. entries and a must have same size!
	 * @param a
	 * @param entries
	 * @return
	 */
	public static double[] restrict(double[] a, BitVector entries) {
		double[] re = new double[entries.cardinality()];
		int index = 0;
		for (int i=0; i<a.length; i++)
			if (entries.getQuick(i))
				re[index++] = a[i];
		return re;
	}

	/**
	 * Projects only true entries from a. entries and a must have same size!
	 * @param a
	 * @param entries
	 * @return
	 */
	public static int[] restrict(int[] a, BitVector entries) {
		int[] re = new int[entries.cardinality()];
		int index = 0;
		for (int i=0; i<a.length; i++)
			if (entries.getQuick(i))
				re[index++] = a[i];
		return re;
	}
	
	public static int[] restrict(int[] a, int[] indices) {
		int[] re = new int[indices.length];
		for (int i=0; i<indices.length; i++)
			re[i] = a[indices[i]];
		return re;
	}
	
	public static double[] restrict(double[] a, int[] indices) {
		double[] re = new double[indices.length];
		for (int i=0; i<indices.length; i++)
			re[i] = a[indices[i]];
		return re;
	}
	
	public static <T> T[] restrict(T[] a, int[] indices) {
		T[] re = (T[]) Array.newInstance(a.getClass().getComponentType(), indices.length);
		for (int i=0; i<indices.length; i++)
			re[i] = a[indices[i]];
		return re;
	}
	
	

	/**
	 * Projects only true entries from a. entries and a must have same size!
	 * @param a
	 * @param entries
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] restrict(T[] a, BitVector entries) {
		T[] re = (T[]) Array.newInstance(a.getClass().getComponentType(), entries.cardinality());
		int index = 0;
		for (int i=0; i<a.length; i++)
			if (entries.getQuick(i))
				re[index++] = a[i];
		return re;
	}

	/**
	 * Projects only true entries from a. entries and a must have same size!
	 * @param a
	 * @param entries
	 * @return
	 */
	public static BitVector restrict(BitVector a, BitVector entries) {
		BitVector re = new BitVector(entries.cardinality());
		int index = 0;
		for (int i=0; i<a.size(); i++)
			if (entries.getQuick(i))
				re.putQuick(index++, a.getQuick(i));
		return re;
	}


	/**
	 * Projects only true entries from a. entries and a must have same size!
	 * @param a
	 * @param entries
	 * @return
	 */
	public static double[] restrict(double[] a, IntPredicate use) {
		int s = 0;
		for (int i=0; i<a.length; i++)
			if (use.test(i))
				s++;

		double[] re = new double[s];
		int index = 0;
		for (int i=0; i<a.length; i++)
			if (use.test(i))
				re[index++] = a[i];
		return re;
	}

	/**
	 * Projects only true entries from a. entries and a must have same size!
	 * @param a
	 * @param entries
	 * @return
	 */
	public static int[] restrict(int[] a, IntPredicate use) {
		int s = 0;
		for (int i=0; i<a.length; i++)
			if (use.test(i))
				s++;

		int[] re = new int[s];
		int index = 0;
		for (int i=0; i<a.length; i++)
			if (use.test(i))
				re[index++] = a[i];
		return re;
	}

	/**
	 * Projects only true entries from a. entries and a must have same size!
	 * @param a
	 * @param entries
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] restrict(T[] a, IntPredicate use) {
		int s = 0;
		for (int i=0; i<a.length; i++)
			if (use.test(i))
				s++;

		T[] re = (T[]) Array.newInstance(a.getClass().getComponentType(), s);
		int index = 0;
		for (int i=0; i<a.length; i++)
			if (use.test(i))
				re[index++] = a[i];
		return re;
	}

	/**
	 * Mapping new->old indices
	 * @param entries
	 * @return
	 */
	public static int[] restrictMap(BitVector entries) {
		int[] re = new int[entries.cardinality()];
		int index = 0;
		for (int i=0; i<entries.size(); i++)
			if (entries.getQuick(i))
				re[index++] = i;
		return re;
	}
	/**
	 * Mapping old->new indices (-1 if missing)
	 * @param entries
	 * @return
	 */
	public static int[] restrictInverseMap(BitVector entries) {
		int[] re = new int[entries.size()];
		int index = 0;
		for (int i=0; i<entries.size(); i++)
			if (entries.getQuick(i))
				re[i] = index++;
			else
				re[i] = -1;
		return re;
	}

	/**
	 * Projects only true rows and/or columns from m.
	 * @param m
	 * @param rowCol
	 * @param rows
	 * @param columns
	 * @return
	 */
	public static double[][] matrixRestrict(double[][] m, BitVector rowCol,
			boolean rows, boolean columns) {

		double[][] re = new double[rows?rowCol.cardinality():m.length][];
		int ri = 0;

		for (int i=0; i<m.length; i++)
			if (!rows || rowCol.getQuick(i))
				re[ri++] = columns?restrict(m[i],rowCol):m[i].clone();

				return re;

	}

	/**
	 * Reverses {@link #restrict(double[], BitVector)}: returns an array of size entries.size and puts 
	 * the entries of a at true positions (a.length==entries.cardinality)
	 * @param a
	 * @param entries
	 * @return
	 */
	public static double[] expand(double[] a,
			BitVector entries) {
		double[] re = new double[entries.size()];
		int index = 0;
		for (int i=0; i<re.length; i++) 
			if (entries.getQuick(i))
				re[i] = a[index++];
		return re;
	}

	public static double[][] matrixExpand(double[][] m,
			BitVector rowCol, boolean rows, boolean columns) {

		double[][] re = new double[rows?rowCol.size():m.length][];
		int ri = 0;

		for (int i=0; i<m.length; i++)
			if (!rows || rowCol.getQuick(i))
				re[i] = columns?expand(m[ri++],rowCol):m[ri++].clone();

				return re;
	}

	/**
	 * Multiplies inplace
	 * @param dp
	 * @param d
	 */
	public static void mult(double[] a, double f) {
		for (int i=0; i<a.length; i++)
			a[i]*=f;
	}

	public static void mult(double[] a, int start, int end, double f) {
		for (int i=start; i<end; i++)
			a[i]*=f;
	}

	public static float[] toFloat(double[] a) {
		float[] re = new float[a.length];
		for (int i=0; i<re.length; i++)
			re[i] = (float)a[i];
		return re;
	}

	public static float[] toFloat(int[] a) {
		float[] re = new float[a.length];
		for (int i=0; i<re.length; i++)
			re[i] = (float)a[i];
		return re;
	}

	public static double[] toDouble(float[] a) {
		double[] re = new double[a.length];
		for (int i=0; i<re.length; i++)
			re[i] = (double)a[i];
		return re;
	}

	public static long[] toLong(int[] a) {
		long[] re = new long[a.length];
		for (int i=0; i<re.length; i++)
			re[i] = a[i];
		return re;
	}

	public static int[] toInteger(String[] a) {
		int[] re = new int[a.length];
		for (int i=0; i<re.length; i++)
			re[i] = Integer.parseInt(a[i]);
		return re;
	}

	public static double[] toDouble(String[] a) {
		double[] re = new double[a.length];
		for (int i=0; i<re.length; i++)
			re[i] = Double.parseDouble(a[i]);
		return re;
	}
	public static double[] toDouble(int[] a) {
		double[] re = new double[a.length];
		for (int i=0; i<re.length; i++)
			re[i] = (double)a[i];
		return re;
	}

	public static float[][] toFloatMatrix(int[][] m) {
		float[][] re = new float[m.length][];
		for (int i=0; i<re.length;i++)
			re[i] = toFloat(m[i]);
		return re;
	}

	public static boolean equals(int[] a1, int start1, int end1, int[] a2, int start2, int end2) {
		if (end1-start1!=end2-start2)
			return false;
		int n = end1-start1;
		for (int i=0; i<n; i++)
			if (a1[i+start1]!=a2[i+start2])
				return false;
		return true;
	}
	
	public static boolean equals(double[] a, double[] b, double eps) {
		if (a.length!=b.length) return false;
		for (int i=0; i<a.length; i++)
			if (Math.abs(a[i]-b[i])>eps)
				return false;
		return true;
	}

	public static boolean equals(long[] a1, int start1, int end1, long[] a2, int start2, int end2) {
		if (end1-start1!=end2-start2)
			return false;
		int n = end1-start1;
		for (int i=0; i<n; i++)
			if (a1[i+start1]!=a2[i+start2])
				return false;
		return true;
	}

	/**
	 * Checks double identity!!
	 * @param a1
	 * @param start1
	 * @param end1
	 * @param a2
	 * @param start2
	 * @param end2
	 * @return
	 */
	public static boolean equals(double[] a1, int start1, int end1, double[] a2, int start2, int end2) {
		if (end1-start1!=end2-start2)
			return false;
		int n = end1-start1;
		for (int i=0; i<n; i++)
			if (a1[i+start1]!=a2[i+start2])
				return false;
		return true;
	}

	public static int[][] cloneMatrix(int[][] m) {
		int[][] re = new int[m.length][];
		for (int i=0; i<re.length; i++)
			re[i] = m[i].clone();
		return re;
	}

	/**
	 * L1 normalization
	 * @param a
	 */
	public static void normalize(double[] a) {
		double sum = 0;
		for (double x : a) sum+=x;
		if (sum==0)
			for (int i=0; i<a.length; i++) a[i]=1/a.length;
		else
			for (int i=0; i<a.length; i++) a[i]/=sum;
	}


	/**
	 * L2 normalization
	 * @param a
	 */
	public static void normalizeL2(double[] a) {
		double sum = 0;
		for (double x : a) sum+=x*x;
		sum = Math.sqrt(sum);
		if (sum==0)
			for (int i=0; i<a.length; i++) a[i]=1/a.length;
		else
			for (int i=0; i<a.length; i++) a[i]/=sum;
	}

	/**
	 * L1 normalization
	 * @param a
	 */
	public static void normalize(float[] a) {
		float sum = 0;
		for (float x : a) sum+=x;
		for (int i=0; i<a.length; i++) a[i]/=sum;
	}


	/**
	 * L1 normalization
	 * @param a
	 */
	public static void normalize(float[][] a) {
		double sum = 0;
		for (float[] a2 : a) 
			for (float f : a2)
				sum+=f;
		for (float[] a2 : a) 
			for (int i=0; i<a2.length; i++)
				a2[i]/=sum;
	}

	/**
	 * L1 normalization
	 * @param a
	 */
	public static void normalize(double[][] a) {
		double sum = 0;
		for (double[] a2 : a) 
			for (double f : a2)
				sum+=f;
		for (double[] a2 : a) 
			for (int i=0; i<a2.length; i++)
				a2[i]/=sum;
	}

	/**
	 * L2 normalization
	 * @param first
	 */
	public static void euclidNormalize(double[] a) {
		double ss = 0;
		for (double d : a)
			ss+=d*d;
		ss = Math.sqrt(ss);
		for (int i=0; i<a.length; i++)
			a[i] /= ss;
	}

	/**
	 * Removes duplicate successive entries.
	 * Returns the index after all the unique entries.
	 * @param a
	 * @param fromIndex
	 * @param toIndex
	 * @return
	 */
	public static int unique(int[] a) {
		return unique(a,0,a.length);
	}
	public static int unique(int[] a, int fromIndex, int toIndex) {
		if (toIndex<=fromIndex) return 0;
		int index = fromIndex;
		for (int i=index+1; i<toIndex; i++) {
			if (a[i]!=a[index])
				index++;
			a[index]=a[i];
		}
		return index+1;
	}

	/**
	 * Removes duplicate successive entries.
	 * Returns the index after all the unique entries.
	 * @param a
	 * @param fromIndex
	 * @param toIndex
	 * @return
	 */
	public static int unique(String[] a) {
		return unique(a,0,a.length);
	}
	public static int unique(String[] a, int fromIndex, int toIndex) {
		int index = fromIndex;
		for (int i=index+1; i<toIndex; i++) {
			if (!a[i].equals(a[index]))
				index++;
			a[index]=a[i];
		}
		return index+1;
	}

	/**
	 * Removes duplicate successive entries by using equals
	 * Returns the index after all the unique entries.
	 * @param a
	 * @param fromIndex
	 * @param toIndex
	 * @return
	 */
	public static <T> int unique(T[] a, int fromIndex, int toIndex) {
		int index = fromIndex;
		for (int i=index+1; i<toIndex; i++) {
			if (!a[i].equals(a[index]))
				index++;
			a[index]=a[i];
		}
		return index+1;
	}
	public static <T> int unique(T[] a, int fromIndex, int toIndex, BiPredicate<T, T> equals) {
		int index = fromIndex;
		for (int i=index+1; i<toIndex; i++) {
			if (!equals.test(a[i],a[index]))
				index++;
			a[index]=a[i];
		}
		return index+1;
	}

	public static <T> int unique(T[] a, Comparator<T> comp) {
		int index = 0;
		for (int i=1; i<a.length; i++) {
			if (comp.compare(a[i],a[index])!=0)
				index++;
			a[index]=a[i];
		}
		return index+1;
	}

	public static <T> int remove(T[] a, Predicate<T> retain) {
		int index = 0;
		for (int i=0; i<a.length; i++) {
			if (retain.test(a[i]))
				a[index++]=a[i];
		}
		return index;
	}

	public static int unique(long[] a, int fromIndex, int toIndex) {
		int index = fromIndex;
		for (int i=index+1; i<toIndex; i++) {
			if (a[i]!=a[index])
				index++;
			a[index]=a[i];
		}
		return index+1;
	}


	public static int unique(double[] a) {
		return unique(a,0,a.length);
	}
	public static int unique(double[] a, int fromIndex, int toIndex) {
		int index = fromIndex;
		for (int i=index+1; i<toIndex; i++) {
			if (a[i]!=a[index])
				index++;
			a[index]=a[i];
		}
		return index+1;
	}

	public static void fillMatrix(float[][] a, float val) {
		for (int i=0; i<a.length; i++)
			for (int j=0; j<a[i].length; j++)
				a[i][j] = val;
	}

	public static void fillMatrix(double[][] a, double val) {
		for (int i=0; i<a.length; i++)
			for (int j=0; j<a[i].length; j++)
				a[i][j] = val;
	}

	public static void fillMatrix(int[][] a, int val) {
		for (int i=0; i<a.length; i++)
			for (int j=0; j<a[i].length; j++)
				a[i][j] = val;
	}

	public static double[] repeat(double value, int length) {
		double[] re = new double[length];
		Arrays.fill(re, value);
		return re;
	}

	public static int[] parseIntArray(String s, char separator) {
		return parseIntArray(s, separator, 0);
	}

	public static int[] parseIntArray(String s, char separator, int defaultForEmpty) {
		if (s==null) return new int[0];
		String[] fields = StringUtils.split(s, separator);
		int[] re = new int[fields.length];
		for (int i=0;i<re.length; i++)
			re[i] = fields[i].length()==0?defaultForEmpty:Integer.parseInt(fields[i]);
		return re;
	}

	public static double[] parseDoubleArray(String s, char separator) {
		if (s==null) return new double[0];
		String[] fields = StringUtils.split(s, separator);
		double[] re = new double[fields.length];
		for (int i=0;i<re.length; i++)
			re[i] = fields[i].length()==0?0:Double.parseDouble(fields[i]);
		return re;
	}

	public static double[] parseDoubleArray(String[] fields) {
		if (fields==null) return new double[0];
		double[] re = new double[fields.length];
		for (int i=0;i<re.length; i++)
			re[i] = fields[i].length()==0?0:Double.parseDouble(fields[i]);
		return re;
	}

	public static int[] subtract(int a, int[] b) {
		int[] re = new int[b.length];
		for (int i=0; i<b.length; i++)
			re[i] = a-b[i];
		return re;
	}

	public static double[] divide(double[] a, double[] b) {
		if (a.length!=b.length)
			throw new IllegalArgumentException("Lengths do not match: "+a.length+"!="+b.length);
		double[] re = new double[b.length];
		for (int i=0; i<b.length; i++)
			re[i] = a[i]/b[i];
		return re;
	}

	public static <T> T first(T[] a) {
		for (T t : a)
			if (t!=null) return t;
		return null;
	}

	/**
	 * At re[0] is the index of the smallest element of a and so on.
	 * @param a
	 * @return
	 */
	public static int[] getRanks(double[] a) {
		int[] re = seq(0, a.length-1, 1);
		parallelSort(a.clone(), re,0,a.length);
		return re;
	}

	public static void toZscores(double[] a) {
		double mean = 0;
		double sd = 0;
		for (double d : a) 
			mean+=d;
		mean/=a.length;
		for (double d : a) {
			double d2 = d-mean;
			sd+=d2*d2;
		}
		sd = Math.sqrt(sd/(a.length-1));
		for (int i=0; i<a.length; i++)
			a[i] = (a[i]-mean)/sd;
	}

	public static boolean noNaN(double[] a) {
		for (double d : a)
			if (Double.isNaN(d))
				return false;
		return true;
	}

	public static double[] createProbabilities(
			RealDistribution distr, int from, int to, int bins) {
		double[] re = new double[bins];
		double binSize = (to-from)/(double)bins;
		for (int i=0; i<bins; i++)
			re[i] = distr.cumulativeProbability(from+binSize*i, from+binSize*(i+1));
		return re;
	}

	public static double[] decumSumInPlace(double[] a, int dir) {
		if (dir>0) {
			for (int i=a.length-1; i>0; i--)
				a[i]-=a[i-1];
		}
		else {
			for (int i=0; i<a.length-1; i++)
				a[i]-=a[i+1];
		}
		return a;
	}

	public static double[] decumSumInPlace(double[] a, int start, int end, int dir) {
		if (dir>0) {
			for (int i=end-1; i>start; i--)
				a[i]-=a[i-1];
		}
		else {
			for (int i=start; i<end-1; i++)
				a[i]-=a[i+1];
		}
		return a;
	}

	/**
	 * The secondary max is the is the second highest value except all values right next to the maximum.
	 * Right next to here means all decreasing values.
	 * @param scores
	 * @return
	 */
	public static int secondaryArgmax(double[] a) {
		int mi = argmax(a);
		// scan left until increasing
		int i;
		for (i=mi-1; i>=0 && a[i]<=a[i+1]; i--);
		int l = argmax(a,0,i+1);
		for (i=mi+1; i<a.length && a[i]<=a[i-1]; i++);
		int r = argmax(a,i,a.length);
		if (l==-1) return r;
		if (r==-1) return l;
		return a[l]>=a[r]?l:r;
	}

	public static double[][] diagonalMatrix(double[] d) {
		double[][] re = new double[d.length][d.length];
		for (int i=0; i<d.length; i++)
			re[i][i] = d[i];
		return re;
	}

	public static double[] getColumnWiseMean(double[][] matrix) {
		return getColumnWiseMean(matrix, null);
	}

	public static double[] getColumnWiseMean(double[][] matrix, IntArrayList rows) {
		double[] re = new double[matrix[0].length];

		if (rows==null)
			for (int i=0; i<matrix.length; i++)
				for (int j=0; j<matrix[i].length; j++)
					re[j]+=matrix[i][j];
		else
			for (int in=0; in<rows.size(); in++) {
				int i=rows.getInt(in);
				for (int j=0; j<matrix[i].length; j++)
					re[j]+=matrix[i][j];
			}


		for (int j=0; j<re.length; j++)
			re[j]/=matrix.length;
		return re;
	}

	public static <T> T[] append(T[] a, T e) {
		a = redimPreserve(a, a.length+1);
		a[a.length-1] = e;
		return a;
	}

	public static <T> T[] getSingletonArray(T e, Class<T> cls) {
		T[] re = (T[])Array.newInstance(cls, 1);
		re[0] = e;
		return re;
	}

	public static int[] add(int[] total, int[] add) {
		if (total==null||total.length!=add.length) total = new int[add.length];
		for (int i=0; i<total.length; i++)
			total[i]+=add[i];
		return total;
	}
	public static long[] add(long[] total, long[] add) {
		if (total==null||total.length!=add.length) total = new long[add.length];
		for (int i=0; i<total.length; i++)
			total[i]+=add[i];
		return total;
	}
	public static double[] add(double[] total, double[] add) {
		if (total==null||total.length!=add.length) total = new double[add.length];
		for (int i=0; i<total.length; i++)
			total[i]+=add[i];
		return total;
	}
	public static double[] add(double[] total, double add) {
		for (int i=0; i<total.length; i++)
			total[i]+=add;
		return total;
	}

	public static <IN,OUT> OUT[] map(IN[] input, OUT[] out, Function<IN,OUT> fun) {
		if (out.length < input.length)
			out = (OUT[]) Array.newInstance(out.getClass().getComponentType(), input.length);

		for (int i=0; i<input.length; i++)
			out[i] = fun.apply(input[i]);
		return out;
	}

	public static int[] toInt(short[] a) {
		int[] re = new int[a.length];
		for (int i=0; i<a.length; i++)
			re[i] = a[i];
		return re;
	}

	public static int[] toPrimitive(Integer[] a) {
		int[] re = new int[a.length];
		for (int i=0; i<a.length; i++)
			re[i] = a[i];
		return re;
	}

	public static double[] componentWise(double[] a,
			double[] b, DoubleBinaryOperator op) {
		double[] re = new double[a.length];
		for (int i=0; i<re.length; i++)
			re[i] = op.applyAsDouble(a[i],b[i]);
		return re;
	}

	public static <A,K,V> HashMap<K, V> createMapping(A[] a, Function<A,K> key, Function<A,V> val) {
		HashMap<K,V> re = new HashMap<K, V>();
		for (int i=0; i<a.length; i++)
			re.put(key.apply(a[i]), val.apply(a[i]));
		return re;
	}

	/**
	 * each entry is moved from index to index+shift
	 * @param a
	 * @param shift
	 * @param insert
	 */
	public static double shift(double[] a, int shift, double insert) {
		if (shift>0) {
			double re = sum(a,a.length-shift,a.length);
			for (int i=a.length-1; i>=shift; i--)
				a[i] = a[i-shift];
			Arrays.fill(a,0,shift, insert);
			return re;
		} else {
			double re = sum(a,0,-shift);
			for (int i=0; i<a.length+shift; i++)
				a[i] = a[i-shift];
			Arrays.fill(a,a.length+shift,a.length, insert);
			return re;
		}
	}


	private static LZ4Compressor lz4Compressor = LZ4Factory.fastestInstance().fastCompressor();

	/**
	 * Uses the LZ4 algorithm to compress the contents of b into o
	 * 
	 * @param b
	 * @param o
	 * @return number of bytes written to o or -save if o was too small
	 */
	public static int compress(byte[] b, int boffset, int len, byte[] o, int ooffset) throws IOException {
		try{
			return lz4Compressor.compress(b, boffset, len, o, ooffset);
		} catch (LZ4Exception e) {
			return -lz4Compressor.maxCompressedLength(b.length);
		}
	}

	public static int getSaveCompressedSize(int len) throws IOException {
		return lz4Compressor.maxCompressedLength(len);
	}

	private static LZ4FastDecompressor lz4Decompressor = LZ4Factory.fastestInstance().fastDecompressor();
	/**
	 * Uses the LZ4 algorithm to decompress the contents of b into o; the decompressed length must be known
	 * 
	 * @param b
	 * @param o
	 * 
	 * @return number of bytes read from b
	 */
	public static int decompress(byte[] b, int boffset, byte[] o, int ooffset, int decompressedLength) throws IOException {
		return lz4Decompressor.decompress(b, boffset,o, ooffset, decompressedLength);
	}

	/**
	 * Computes the rank-order statistic of a (that is, the rank smallest element starting from 0)
	 * This partially sorts the array!
	 * 
	 * @param a
	 * @param rank
	 * @return
	 */
	public static double orderStatistic(double[] a, int rank) {
		if (a == null || a.length <= rank)
			throw new IndexOutOfBoundsException();

		int from = 0, to = a.length - 1;

		// if from == to we reached the kth element
		while (from < to) {
			int r = from, w = to;
			double mid = a[(r + w) / 2];

			// stop if the reader and writer meets
			while (r < w) {

				if (a[r] >= mid) { // put the large values at the end
					double tmp = a[w];
					a[w] = a[r];
					a[r] = tmp;
					w--;
				} else { // the value is smaller than the pivot, skip
					r++;
				}
			}

			// if we stepped up (r++) we need to step one down
			if (a[r] > mid)
				r--;

			// the r pointer is on the end of the first k elements
			if (rank <= r) {
				to = r;
			} else {
				from = r + 1;
			}
		}

		return a[rank];
	}
	
	/**
	 * Computes the rank-order statistic of a (that is, the rank smallest element starting from 0)
	 * This partially sorts the array!
	 * 
	 * @param a
	 * @param rank
	 * @return
	 */
	public static double orderStatistic(int[] a, int rank) {
		if (a == null || a.length <= rank)
			throw new IndexOutOfBoundsException();

		int from = 0, to = a.length - 1;

		// if from == to we reached the kth element
		while (from < to) {
			int r = from, w = to;
			int mid = a[(r + w) / 2];

			// stop if the reader and writer meets
			while (r < w) {

				if (a[r] >= mid) { // put the large values at the end
					int tmp = a[w];
					a[w] = a[r];
					a[r] = tmp;
					w--;
				} else { // the value is smaller than the pivot, skip
					r++;
				}
			}

			// if we stepped up (r++) we need to step one down
			if (a[r] > mid)
				r--;

			// the r pointer is on the end of the first k elements
			if (rank <= r) {
				to = r;
			} else {
				from = r + 1;
			}
		}

		return a[rank];
	}

	public static int hashCode(int[] a) {
        if (a == null)
            return 0;

        int result = 1;
        for (int element : a)
            result = (31 * result + element) ^ result;

        return result;
	}


}
