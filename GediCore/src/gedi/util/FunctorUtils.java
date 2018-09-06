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
package gedi.util;


import gedi.util.datastructure.collections.FastSortingCollection;
import gedi.util.datastructure.collections.SerializerSortingCollection;
import gedi.util.datastructure.collections.SortingCollection;
import gedi.util.datastructure.collections.doublecollections.DoubleIterator;
import gedi.util.datastructure.collections.intcollections.IntIterator;
import gedi.util.functions.BooleanUnaryOperator;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.randomaccess.serialization.BinarySerializer;
import gedi.util.mutable.MutablePair;
import gedi.util.orm.Orm;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoublePredicate;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

import cern.colt.bitvector.BitVector;

/**
 * Generally, iterators skip null values!
 * @author erhard
 *
 */
public class FunctorUtils {


	/// Iterators

	public static class DemultiplexIterator<I,O> implements ExtendedIterator<O> {

		private Iterator<I> it;
		private Function<I, Iterator<O>> applyer;
		private Iterator<O> current;
		private O next;

		public DemultiplexIterator(Iterator<I> it,
				Function<I, Iterator<O>> applyer) {
			this.it = it;
			this.applyer = applyer;
		}

		@Override
		public boolean hasNext() {
			lookAhead();
			return next!=null;
		}

		@Override
		public O next() {
			lookAhead();
			O re = next;
			next = null;
			return re;
		}

		private void lookAhead() {
			if (next==null) {
				while (next==null) {
					if (current==null || !current.hasNext())
						current = it.hasNext() ? applyer.apply(it.next()) : null;

						if (current==null)
							return;
						if (current.hasNext())
							next = current.next();
				}
			}
		}

		@Override
		public void remove() {}


	}

	public static class SingletonIterator<T> implements ExtendedIterator<T> {

		private T element;
		private boolean returned = false;

		public SingletonIterator(T e) {
			this.element = e;
			if (e==null) returned = true;//skip null
		}

		@Override
		public boolean hasNext() {
			return !returned;
		}

		@Override
		public T next() {
			returned = true;
			return element;
		}

		@Override
		public void remove() {}

	}

	public static class SubstringIterator implements ExtendedIterator<String> {

		private String s;
		private int l;
		private int o = 0;
		private boolean overlapping;
		
		public SubstringIterator(String s, int l, boolean overlapping) {
			this.s = s;
			this.l = l;
			this.overlapping = overlapping;
		}

		@Override
		public boolean hasNext() {
			return o+l<=s.length();
		}

		@Override
		public String next() {
			String re = s.substring(o,o+l);
			if (overlapping)
					o++;
			else
				o+=l;
			return re;
		}

		@Override
		public void remove() {}
		
	}
	
	
	/**
	 * Each iterator must yield a strictly increasing list of item (i.e. two succinct elements must compare to a number >1)
	 * This is for performance reasons not checked!
	 * @author erhard
	 *
	 * @param <T>
	 */
	public static class ParallellIterator<T> implements ExtendedIterator<T[]> {

		private Iterator<T>[] iterators;
		private Comparator<? super T> order;
		private boolean ensured = false;
		private BitVector smallest;
		private T[] next;
		private T[] cache;
		private int N;

		@SuppressWarnings("unchecked")
		public ParallellIterator(Iterator<T>[] iterators, Comparator<? super T> order, Class<T> cls) {
			this.iterators = iterators;
			this.order = order;
			this.N = iterators.length;

			next = (T[]) Array.newInstance(cls, N);
			cache = (T[]) Array.newInstance(cls, N);
			smallest = new BitVector(N);
		}
		
		public Iterator<T>[] getIterators() {
			return iterators;
		}

		@Override
		public boolean hasNext() {
			lookAhead();
			return next!=null;
		}

		public BitVector getNextMask() {
			lookAhead();
			return smallest;
		}

		@Override
		public T[] next() {
			lookAhead();
			ensured = false;
			return next.clone();
		}

		private void lookAhead() {
			if (!ensured) {
				updateCache();
				if (determineSmallest()) {
					// move all smallest from cache to next
					for (int i=0; i<N; i++)
						if(smallest.getQuick(i)) {
							next[i] = cache[i];
							cache[i] = null;
						} else
							next[i] = null;
				} else
					next = null;
				ensured = true;
			}
		}

		private boolean determineSmallest() {
			T smallest = null;
			int i;
			for (i=0; i<N && cache[i]==null; i++);
			
			if (i==N)
				return false;
			
			smallest = cache[i];

			this.smallest.clear();
			this.smallest.putQuick(i, true);

			for (i++; i<N; i++) {
				if (cache[i]!=null) {
					int c = order.compare(smallest, cache[i]);
					if (c>0) 
						this.smallest.clear();
					if (c>=0) {
						this.smallest.putQuick(i, true);
						smallest = cache[i];
					}
				}
			}
			return true;
		}

		/**
		 * After calling this, cache contains the next elements or null, if an iterator is depleted
		 */
		private void updateCache() {
			for (int i=0; i<N; i++)
				if (cache[i]==null && iterators[i].hasNext())
					cache[i] = iterators[i].next();
//			System.out.println(Arrays.toString(cache));
		}

		@Override
		public void remove() {}

	}


	/**
	 * Iterators must yield sorted order; the resulting iteration yields all elements from all iterators in sorted order
	 * @author erhard
	 *
	 * @param <T>
	 */
	public static class MergeIterator<T> implements ExtendedIterator<T> {

		private Iterator<T>[] iterators;
		private Comparator<? super T> order;
		private TreeSet<Entry<T>> heap;
		private int itIndex;

		public MergeIterator(Iterator<T>[] iterators, Comparator<? super T> order) {
			this.iterators = iterators;
			this.order = order;

			heap = new TreeSet<Entry<T>>(new EntryComparator());
			for (int i=0; i<iterators.length; i++)
				insert(i);
		}

		@Override
		public boolean hasNext() {
			return heap.size()>0;
		}

		@Override
		public T next() {
			Entry<T> f = heap.pollFirst();
			itIndex = f.iteratorIndex;
			Entry<T> n = insert(f.iteratorIndex);
			if (n!=null && f.item==n.item && !f.item.getClass().isEnum()) throw new RuntimeException("Do not reuse objects for merging!");
			if (n!=null && order.compare(f.item, n.item)>0) throw new RuntimeException("Iterator not ordered: (Iterator: "+itIndex+") "+f.item+" > "+n.item);
			return f.item;
		}
		
		public Entry<T> nextEntry() {
			Entry<T> f = heap.pollFirst();
			itIndex = f.iteratorIndex;
			Entry<T> n = insert(f.iteratorIndex);
			if (n!=null && f.item==n.item) throw new RuntimeException("Do not reuse objects for merging!");
			if (order.compare(f.item, n.item)>0) throw new RuntimeException("Iterator not ordered: (Iterator: "+itIndex+") "+f.item+" > "+n.item);
			return f;
		}

		/**
		 * Gets the index of the iterator, that returned the item from the most recent call to next
		 * @return
		 */
		public int getIteratorIndex() {
			return itIndex;
		}
		
		private Entry<T> insert(int index) {
			T n = null;
			while (n==null && iterators[index].hasNext())
				n = iterators[index].next();
			Entry<T> re = null;
			if (n!=null)
				heap.add(re = new Entry<T>(index,n));
			return re;
		}


		@Override
		public void remove() {}

		public static class Entry<T>  {
			private int iteratorIndex;
			private T item;
			public Entry(int iteratorIndex, T item) {
				this.iteratorIndex = iteratorIndex;
				this.item = item;
			}
			public int getIteratorIndex() {
				return iteratorIndex;
			}
			public T getItem() {
				return item;
			}
			@Override
			public String toString() {
				return item.toString()+" ("+iteratorIndex+")";
			}
		}
		private class EntryComparator implements Comparator<Entry<T>> {

			@Override
			public int compare(Entry<T> o1, Entry<T> o2) {
				int re = order.compare(o1.item, o2.item);
				if (re==0)
					return o1.iteratorIndex-o2.iteratorIndex;
				else
					return re;
			}

		}

	}

	public static class FuseIterator<T,R> implements ExtendedIterator<R> {

		private Iterator<T>[] iterators;
		private Function<List<T>,R> map;
		private ArrayList<T> collector;

		public FuseIterator(Iterator<T>[] iterators, Function<List<T>,R> map) {
			this.iterators = iterators;
			this.map = map;
			collector = new ArrayList<>(iterators.length);
			for (int i=0; i<iterators.length; i++)
				collector.add(null);
		}

		@Override
		public boolean hasNext() {
			boolean re = iterators[0].hasNext();
			if (!re)
				for (int i=1; i<iterators.length; i++)
					if (iterators[i].hasNext())
						throw new RuntimeException("Iterator have different amounts of elements!");
			return re;
		}

		@Override
		public R next() {
			for (int i=0; i<iterators.length; i++)
				collector.set(i, iterators[i].next());
			return map.apply(collector);
		}
		
	}
	
	
	private static class PatternIterator<T> implements ExtendedIterator<T> {

		private Iterator<T> parent;
		private boolean[] pattern;
		private int index;

		public PatternIterator(Iterator<T> parent, boolean[] pattern) {
			this.parent = parent;
			this.pattern = pattern;
			
			// advance to first true
			for (; index<pattern.length && !pattern[index] && parent.hasNext(); index++)
				parent.next();
			if (index==pattern.length)
				throw new RuntimeException("Pattern empty!");
		}

		@Override
		public boolean hasNext() {
			return parent.hasNext();
		}

		@Override
		public T next() {
			T re = parent.next();
			for (index++; index<pattern.length && !pattern[index] && parent.hasNext(); index = (index+1)%pattern.length)
				parent.next();
			return re;
		}
		
		
		
	}

	
	private static class UnorderedPairIterator<T> implements ExtendedIterator<MutablePair<T, T>> {


		private MutablePair<T, T> next;

		private long to;
		private long index;

		private T[] a;
		private int N;

		private int d;
		private int l;

		public UnorderedPairIterator(T[] a, long from, long to, boolean includeId) {
			if (!includeId) {
				from+=a.length;
				to+=a.length;
			}
			this.to = to;

			this.a = a;
			N = a.length;

			skip(from);
		}
		private void skip(long i) {
			for (d=0; d<N; d++) 
				for (l=0; l+d<N; l++) 
					if (index==i)
						return;
					else index++;
		}
		@Override
		public boolean hasNext() {
			lookAhead();
			return next!=null;
		}
		@Override
		public MutablePair<T, T> next() {
			lookAhead();
			MutablePair<T, T> re = next;
			next = null;
			return re;
		}
		private void lookAhead() {
			if (next==null && index<to) {
				if(l+d>=N) {
					d++;
					l=0;
				}
				if (l+d>=N)
					return;
				next = new MutablePair<T,T>(a[l],a[l+d]);
				index++;
				l++;
			}
		}
		@Override
		public void remove() {}

	}

	public static class PeekIntIterator implements IntIterator {

		private IntIterator parent;
		private int n;
		private boolean isnull = true;
		
		public PeekIntIterator(IntIterator parent) {
			this.parent = parent;
		}
		
		@Override
		public boolean hasNext() {
			return !isnull || parent.hasNext();
		}

		@Override
		public int nextInt() {
			if (isnull)
				return parent.nextInt();
			
			isnull = true;
			return n;
		}
		
		public int peek() {
			if (isnull) {
				isnull = false;
				n = parent.nextInt();
				return n;
			}
			return n;
		}
		
		public IntIterator getParent() {
			return parent;
		}

	}
	public static class FilteredIntIterator implements IntIterator {
		private IntIterator it;
		private IntPredicate predicate;
		private int next;
		private boolean isnull = true;
		
		public FilteredIntIterator(IntIterator it, IntPredicate predicate) {
			this.it = it;
			this.predicate = predicate;
		}
		@Override
		public boolean hasNext() {
			lookAhead();
			return !isnull;
		}
		@Override
		public int nextInt() {
			lookAhead();
			isnull = true;
			return next;
		}
		private void lookAhead() {
			if (isnull && it.hasNext()) { 
				boolean valid = false;
				isnull = false;
				for (next = it.nextInt(); !(valid = predicate.test(next))&&it.hasNext(); next = it.nextInt());
				if (!valid) isnull = true;
			}
		}
		
	}
	
	public static class FilteredDoubleIterator implements DoubleIterator {
		private DoubleIterator it;
		private DoublePredicate predicate;
		private double next;
		private boolean isnull = true;
		
		public FilteredDoubleIterator(DoubleIterator it, DoublePredicate predicate) {
			this.it = it;
			this.predicate = predicate;
		}
		@Override
		public boolean hasNext() {
			lookAhead();
			return !isnull;
		}
		@Override
		public double nextDouble() {
			lookAhead();
			isnull = true;
			return next;
		}
		private void lookAhead() {
			if (isnull && it.hasNext()) { 
				boolean valid = false;
				isnull = false;
				for (next = it.nextDouble(); !(valid = predicate.test(next))&&it.hasNext(); next = it.nextDouble());
				if (!valid) isnull = true;
			}
		}
		
	}
	
	public static class PeekIterator<T> implements ExtendedIterator<T> {

		private Iterator<T> parent;
		private T n;
		
		public PeekIterator(Iterator<T> parent) {
			this.parent = parent;
		}
		
		@Override
		public boolean hasNext() {
			while (n==null && parent.hasNext())
				n = parent.next();
			return n!=null;
		}

		@Override
		public T next() {
			while (n==null && parent.hasNext())
				n = parent.next();
			T re = n;
			n = null;
			return re;
		}
		public T peek() {
			while (n==null && parent.hasNext()) 
				n = parent.next();
			return n;
		}
		
		public Iterator<T> getParent() {
			return parent;
		}

		@Override
		public void remove() {
			parent.remove();
		}
		
	}
	
	
	public static class InitActionIterator<T> implements ExtendedIterator<T> {

		private Iterator<T> parent;
		private Consumer<T> effect;
		
		public InitActionIterator(Iterator<T> parent, Consumer<T> effect) {
			this.parent = parent;
			this.effect = effect;
		}
		
		@Override
		public boolean hasNext() {
			return parent.hasNext();
		}

		@Override
		public T next() {
			T re = parent.next();
			if (effect!=null) {
				effect.accept(re);
				effect = null;
			}
			return re;
		}

		@Override
		public void remove() {
			parent.remove();
		}
		
	}
	public static class SideEffectIterator<T> implements ExtendedIterator<T> {

		private Iterator<T> parent;
		private Predicate<T> pred;
		private Consumer<T> effect;
		
		public SideEffectIterator(Iterator<T> parent, Consumer<T> effect) {
			this.parent = new PeekIterator<>(parent);
			pred=t->true;
			this.effect = effect;
		}
		public SideEffectIterator(Iterator<T> parent, Predicate<T> pred, Consumer<T> effect) {
			this.parent = new PeekIterator<>(parent);
			this.effect = effect;
			this.pred = pred;
		}
		
		@Override
		public boolean hasNext() {
			return parent.hasNext();
		}

		@Override
		public T next() {
			T re = parent.next();
			if (pred.test(re))
				effect.accept(re);
			return re;
		}

		@Override
		public void remove() {
			parent.remove();
		}
		
	}
	
	public static class ParallelizedSideEffectIterator<T> implements ExtendedIterator<T> {

		private Iterator<T> parent;
		private Consumer<T> effect;
		
		private LinkedBlockingQueue<T> queue;
		private Thread worker;
		
		public ParallelizedSideEffectIterator(Iterator<T> parent, int capacity, Consumer<T> effect) {
			this.parent = parent;
			if (capacity<0)
				queue = new LinkedBlockingQueue<T>();
			else
				queue = new LinkedBlockingQueue<T>(capacity);
			this.effect = effect;
			
			worker = new Thread() {
				public void run() {
					for (;;) {
						try {
							effect.accept(queue.take());
						} catch (InterruptedException e) {
							break;
						}
					}
					T t;
					while ((t=queue.poll())!=null)
						effect.accept(t);
				}
			};
			worker.setName("SideEffectIterator");
			worker.setDaemon(true);
			worker.start();
		}
		
		@Override
		public boolean hasNext() {
			boolean re = parent.hasNext();
			if (!re)
				worker.interrupt();
			return re;
		}

		@Override
		public T next() {
			T re = parent.next();
			try {
				queue.put(re);
			} catch (InterruptedException e) {
			}
			return re;
		}

		@Override
		public void remove() {
			parent.remove();
		}
		
	}
	
	public static class HasNextActionIterator<T> implements ExtendedIterator<T> {

		private Iterator<T> parent;
		private BooleanUnaryOperator op;
		
		public HasNextActionIterator(Iterator<T> parent, BooleanUnaryOperator op) {
			this.parent = parent;
			this.op = op;
		}
		
		@Override
		public boolean hasNext() {
			return op.applyAsBoolean(parent.hasNext());
		}

		@Override
		public T next() {
			return parent.next();
		}

		@Override
		public void remove() {
			parent.remove();
		}
		
	}
	
	@SuppressWarnings("unchecked")
	public static class BufferingIterator<T> implements ExtendedIterator<T> {

		private Iterator<T> it;
		private Object[] buffer;
		private int zero;

		public BufferingIterator(Iterator<T> it, int bufferSize) {
			this.it = new PeekIterator<T>(it);
			buffer = new Object[bufferSize+1];
		}
		@Override
		public boolean hasNext() {
			return it.hasNext();
		}
		@Override
		public T next() {
			zero=(zero+1)%buffer.length;
			buffer[zero] = it.next();
			return (T) buffer[zero];
		}
		/**
		 * num==0 returns the element previously returned by next
		 * @param num
		 * @return
		 */
		public T previous(int num) {
			num = (zero-num+buffer.length)%buffer.length;
			return (T) buffer[num];
		}
		public void remove() {
			it.remove();
		}

	}
	
	
	public static abstract class TryNextIterator<T> implements ExtendedIterator<T> {

		private T next;
		

		@Override
		public boolean hasNext() {
			if (next==null)
				next=tryNext();
			return next!=null;
		}

		@Override
		public T next() {
			if (next==null)
				next=tryNext();
			T re = next;
			next = null;
			return re;
		}
		
		/**
		 * Must return null only if the iterator has no next element.
		 * @param next
		 * @return
		 */
		protected abstract T tryNext();


	}
	

	/**
	 * May not be sorted; every element by the parent iterator is checked whether it is a first element. For each first element,
	 * a block is supplied and all subsequent, not first elements are added to the block
	 * @author erhard
	 *
	 * @param <T>
	 * @param <C>
	 */
	public static class BlockIterator<T,C> extends TryNextIterator<C> {

		private Iterator<T> it;
		private Predicate<T> first;
		private Supplier<C> blockSupplier;
		private BiConsumer<C, T> adder;
		
		private T header;
		
		
		public BlockIterator(Iterator<T> it, Predicate<T> first,
				Supplier<C> blockSupplier, BiConsumer<C, T> adder) {
			this.it = it;
			this.first = first;
			this.blockSupplier = blockSupplier;
			this.adder = adder;
			if (it.hasNext())
				header=it.next();
		}

		@Override
		protected C tryNext() {
			if (header==null) 
				return null;
			
			C re = blockSupplier.get();
			adder.accept(re, header);
			if (!it.hasNext()) {
				header=null;
				return re;
			}
			while (it.hasNext()) {
				header = it.next();
				if (first.test(header))
					break;
				adder.accept(re, header);
				header=null;
			}
			
			return re;
		}
	}
	
	/**
	 * Iterator must be sorted; the resulting iterator yields all equal (comparison is 0) objects combined
	 * @author erhard
	 *
	 * @param <I>
	 * @param <O>
	 */
	public static class MultiplexIterator<I,O> implements ExtendedIterator<O> {

		private PeekIterator<I> iterator;
		private Function<List<I>, O> applyer;
		private Comparator<I> comp;
		private BiPredicate<I,I> looseComp;
		private ArrayList<I> aggregator = new ArrayList<I>();

		public MultiplexIterator(Iterator<I> iterator,
				Function<List<I>, O> applyer, Comparator<I> comp) {
			this.iterator = new PeekIterator<I>(iterator);
			this.applyer = applyer;
			this.comp = comp;
		}
		
		public MultiplexIterator(Iterator<I> iterator,
				Function<List<I>, O> applyer, BiPredicate<I,I> looseComp) {
			this.iterator = new PeekIterator<I>(iterator);
			this.applyer = applyer;
			this.looseComp = looseComp;
		}

		@Override
		public boolean hasNext() {
			lookAhead();
			return !aggregator.isEmpty();
		}

		@Override
		public O next() {
			lookAhead();
			O re = applyer.apply(aggregator);
			aggregator.clear();
			return re;
		}
		
		private void lookAhead() {
			if (aggregator.size()==0 && iterator.hasNext()) {
				aggregator.add(iterator.next());
				if (comp!=null) {
					int cmp = -1;
					while (iterator.hasNext() && (cmp=comp.compare(aggregator.get(0), iterator.peek()))==0)
						aggregator.add(iterator.next());
					if (cmp>0) throw new RuntimeException("Iterator must be sorted for multiplexing: "+StringUtils.toString(aggregator.get(0)) + " > " + StringUtils.toString(iterator.peek())+" Comparator: "+comp);
				}
				else {
					while (iterator.hasNext() && (looseComp.test(aggregator.get(0), iterator.peek())))
						aggregator.add(iterator.next());
				}
			}
			
		}

		@Override
		public void remove() {}
		
	}
	
	public static class ResortIterator<T> implements ExtendedIterator<T> {

		private PeekIterator<T> it;
		private Comparator<? super T> weak;
		private Comparator<? super T> strong;
		private Class<T> cls;
		private T proto;
		
		private BinarySerializer<T> serializer;
		
		private ExtendedIterator<T> cit;

		public ResortIterator(Iterator<T> it, Class<T> cls, Comparator<? super T> weak,
				Comparator<? super T> strong) {
			this.cls = cls;
			this.it = peekIterator(it);
			this.proto = this.it.peek();
			this.weak = weak;
			this.strong = strong;
		}
		
		public ResortIterator(Iterator<T> it, BinarySerializer<T> serializer, Comparator<? super T> weak,
				Comparator<? super T> strong) {
			this.serializer = serializer;
			this.it = peekIterator(it);
			this.weak = weak;
			this.strong = strong;
		}

		@Override
		public boolean hasNext() {
			tryFillCollector();
			return cit!=null && cit.hasNext();
		}

		@Override
		public T next() {
			tryFillCollector();
			return cit.next();
		}

		@SuppressWarnings("resource")
		private void tryFillCollector() {
			if ((cit==null || !cit.hasNext()) && it.hasNext()) {
				Collection<T> collector = null;
				try {
					if (serializer!=null)
						collector = new SerializerSortingCollection<T>(serializer, strong, 64*1024);
					else
						collector = new FastSortingCollection<T>(proto, strong, 64*1024);
				} catch (Throwable e) {
					collector = new SortingCollection<T>(cls, strong, 64*1024);
				}
				
				T first = it.next();
				collector.add(first);
				while (it.hasNext() && weak.compare(first, it.peek())==0)
					collector.add(it.next());
				// check comparators
				if (it.hasNext()) {
					T next = it.peek();
					if (weak.compare(first, next)>0 || strong.compare(first, next)>0)
						throw new RuntimeException("Comparators inconsistent:\n"+first.toString()+" > "+next.toString()+"; weak="+weak.compare(first, next)+"; strong="+strong.compare(first, next));
				}
				
				cit = (ExtendedIterator<T>) collector.iterator();
			}
		}
	}
	
	
	public static class FilteredIterator<T> implements ExtendedIterator<T> {
		private Iterator<T> it;
		private Predicate<? super T> predicate;
		private T next;
		public FilteredIterator(Iterator<T> it, Predicate<? super T> predicate) {
			this.it = peekIterator(it);
			this.predicate = predicate;
		}
		@Override
		public boolean hasNext() {
			lookAhead();
			return next!=null;
		}
		@Override
		public T next() {
			lookAhead();
			T r = next;
			next = null;
			return r;
		}
		private void lookAhead() {
			if (next==null && it.hasNext()) { 
				boolean valid = false;
				for (next = it.next(); !(valid = predicate.test(next))&&it.hasNext(); next = it.next());
				if (!valid) next = null;
			}
		}
		@Override
		public void remove() {
			throw new RuntimeException();
		}
		
	}
	
	public static class ExtendedIteratorAdapter<T> implements ExtendedIterator<T> {
		private Iterator<T> it;

		
		public ExtendedIteratorAdapter(Iterator<T> it) {
			this.it = it;
		}

		@Override
		public boolean hasNext() {
			return it.hasNext();
		}

		@Override
		public T next() {
			return it.next();
		}
		
		@Override
		public void remove() {
			it.remove();
		}
		
	}
	
	public static class MappedIterator<S,T> implements ExtendedIterator<T> {
		private Iterator<S> it;
		private Function<S,T> mapper;
		public MappedIterator(Iterator<S> it, Function<S,T> mapper) {
			this.it = peekIterator(it);
			this.mapper = mapper;
		}
		@Override
		public boolean hasNext() {
			return it.hasNext();
		}
		@Override
		public T next() {
			return mapper.apply(it.next());
		}
		@Override
		public void remove() {
			it.remove();
		}
		
		public Iterator<S> getParent() {
			return it;
		}
		
	}
	
	public static class ToIntMappedIterator<S> implements IntIterator {
		private Iterator<S> it;
		private ToIntFunction<S> mapper;
		public ToIntMappedIterator(Iterator<S> it, ToIntFunction<S> mapper) {
			this.it = it;
			this.mapper = mapper;
		}
		@Override
		public boolean hasNext() {
			return it.hasNext();
		}
		@Override
		public int nextInt() {
			return mapper.applyAsInt(it.next());
		}
		@Override
		public void remove() {
			it.remove();
		}
		
		public Iterator<S> getParent() {
			return it;
		}
		
	}
	
	public static class ToDoubleMappedIterator<S> implements DoubleIterator {
		private Iterator<S> it;
		private ToDoubleFunction<S> mapper;
		public ToDoubleMappedIterator(Iterator<S> it, ToDoubleFunction<S> mapper) {
			this.it = it;
			this.mapper = mapper;
		}
		@Override
		public boolean hasNext() {
			return it.hasNext();
		}
		@Override
		public double nextDouble() {
			return mapper.applyAsDouble(it.next());
		}
		@Override
		public void remove() {
			it.remove();
		}
		
		public Iterator<S> getParent() {
			return it;
		}
		
	}

	public static class UnifySequentialIterator<T> implements ExtendedIterator<T> {
		private PeekIterator<T> iterator;
		private Comparator<? super T> comp;
		private T next;
		public UnifySequentialIterator(Iterator<T> it, Comparator<? super T> comp) {
			this.iterator = peekIterator(it);
			this.comp = comp!=null?comp:(a,b)->a.equals(b)?0:-1;
		}
		@Override
		public boolean hasNext() {
			lookAhead();
			return next!=null;
		}

		@Override
		public T next() {
			lookAhead();
			T re = next;
			next = null;
			return re;
		}
		
		private void lookAhead() {
			if (next==null && iterator.hasNext()) {
				next = iterator.next();
				int cmp = -1;
				while (iterator.hasNext() && (cmp=comp.compare(next, iterator.peek()))==0)
					iterator.next();
				if (cmp>0) throw new RuntimeException("Iterator must be sorted for unifying!");
			}
			
		}

		@Override
		public void remove() {}
	}
	
	public static class UnifyNonSequentialIterator<T> implements ExtendedIterator<T> {
		
		private PeekIterator<T> parent;
		private HashSet<T> encountered = new HashSet<T>();
		
		public UnifyNonSequentialIterator(Iterator<T> it) {
			this.parent = peekIterator(it);
		}
		@Override
		public boolean hasNext() {
			lookAhead();
			return parent.hasNext();
		}

		@Override
		public T next() {
			lookAhead();
			T re = parent.next();
			encountered.add(re);
			return re;
		}
		
		private void lookAhead() {
			while (parent.hasNext() && encountered.contains(parent.peek()))
					parent.next();
		}

		@Override
		public void remove() {}
	}
	

	public static class ChainedIterator<T> implements ExtendedIterator<T> {
		
		private Iterator<T>[] a;
		private int index = 0;
		
		public ChainedIterator(Iterator<T>[] its) {
			this.a = its;
		}
		@Override
		public boolean hasNext() {
			while (index<a.length && !a[index].hasNext()) 
				index++;
			return index<a.length;
		}

		@Override
		public T next() {
			while (index<a.length && !a[index].hasNext()) 
				index++;
			return a[index].next();
		}
		
	}
	
	
	public static class AlternatingIterator<T> implements ExtendedIterator<T> {
		
		private class CircularList {
			CircularList next;
			Iterator<? extends T> it;
			public CircularList(CircularList next, Iterator<? extends T> it) {
				this.next = next;
				this.it = it;
			}
		}
		
		// Invariant: each it in l hasNext!
		private CircularList l;
		
		public AlternatingIterator(Iterator<T> fi, Iterator<? extends T>... its) {
			CircularList f = null;
			for (int i=its.length-1; i>=0; i--)
				if (its[i].hasNext()) {
					l = new CircularList(l, its[i]);
					if (f==null) f = l;
				}
			if (fi.hasNext()) {
				l = new CircularList(l, fi);
				if (f==null) f = l;
			}
			f.next = l;
		}
		@Override
		public boolean hasNext() {
			return l!=null;
		}

		@Override
		public T next() {
			T re = l.it.next();
			if (!l.it.hasNext()) {
				if (l.next==l) l = null;
				else {
					l.it = l.next.it;
					l.next = l.next.next;
				}
			}
			if (l!=null) l = l.next;
			return re;
		}
	}

	public static class HeadIterator<T> implements ExtendedIterator<T> {
		
		private Iterator<T> a;
		private int n;

		
		public HeadIterator(Iterator<T> a, int n) {
			this.a = a;
			this.n = n;
		}

		@Override
		public boolean hasNext() {
			return n>0 && a.hasNext();
		}

		@Override
		public T next() {
			n--;
			return a.next();
		}
		
	}
	
	public static class ReduceIterator<T,R> implements ExtendedIterator<R> {
		private PeekIterator<T> iterator;
		private Comparator<? super T> comp;
		private Supplier<R> supplier;
		private BiFunction<T,R,R> reducer;
		private R next;
		public ReduceIterator(Iterator<T> it, Comparator<? super T> comp, Supplier<R> supplier, BiFunction<T,R,R> reducer) {
			this.iterator = peekIterator(it);
			this.comp = comp;
			this.supplier = supplier;
			this.reducer = reducer;
		}
		@Override
		public boolean hasNext() {
			lookAhead();
			return next!=null;
		}

		@Override
		public R next() {
			lookAhead();
			R re = next;
			next = null;
			return re;
		}
		
		private void lookAhead() {
			if (next==null && iterator.hasNext()) {
				int cmp = -1;
				next = supplier.get();
				T last;
				next = reducer.apply(last = iterator.next(), next);
				while (iterator.hasNext() && (cmp=comp.compare(last, iterator.peek()))==0)
//					next = reducer.apply(next, iterator.next());
					next = reducer.apply(last = iterator.next(), next);
				if (cmp>0) throw new RuntimeException("Iterator must be sorted for unifying!");
			}
			
		}

		@Override
		public void remove() {}
	}
	

	/**
	 * May return null!
	 * @author erhard
	 *
	 * @param <T>
	 */
	public static class FactoryIterator<T> implements ExtendedIterator<T> {
		private int index;
		private int size;
		private IntFunction<T> factory;
		
		public FactoryIterator(int size, IntFunction<T> factory) {
			this.size = size;
			this.factory = factory;
		}
		@Override
		public boolean hasNext() {
			return index<size;
		}
		@Override
		public T next() {
			return factory.apply(index++);
		}
		@Override
		public void remove() {
			throw new RuntimeException();
		}
		
	}
	
	/**
	 * May return null!
	 * @author erhard
	 *
	 * @param <T>
	 */
	public static class ArrayIterator<T> implements ExtendedIterator<T> {
		private int index;
		private int end;
		private T[] a;
		public ArrayIterator(T[] a) {
			this.a = a;
			end = a.length;
		}
		
		public ArrayIterator(T[] a, int start, int end) {
			this.a = a;
			index = start;
			this.end = end;
		}
		
		@Override
		public boolean hasNext() {
			return index<end;
		}
		@Override
		public T next() {
			return a[index++];
		}
		@Override
		public void remove() {
			throw new RuntimeException();
		}
		
	}

	/// Functions
	
	public static class WritingFunction<K> implements Function<K,K> {

		private Writer writer;
		private boolean flush;
		
		public WritingFunction(Writer writer, boolean flush) {
			this.writer = writer;
			this.flush = flush;
		}

		@Override
		public K apply(K arg0) {
			try {
				writer.write(arg0.toString()+"\n");
				if (flush)
					writer.flush();
			} catch (IOException e) {
				throw new RuntimeException("Could write!",e);
			}
			return arg0;
		}

	}
	
	
	public static class OrmExtractFunction<T,R> implements Function<T,R> {

		private int index;
		
		public OrmExtractFunction(int index) {
			this.index = index;
		}

		@Override
		public R apply(T t) {
			return Orm.getField(t, index);
		}
		
	}
	
	/// Comporators
	
	public static class CharSequenceComparator<T extends CharSequence> implements Comparator<T> {

		@Override
		public int compare(T o1, T o2) {
			int len1 = o1.length();
			int len2 = o2.length();
			int n = Math.min(len1, len2);

			for (int k=0; k<n; k++) {
				char c1 = o1.charAt(k);
				char c2 = o2.charAt(k);
				if (c1 != c2) {
					return c1 - c2;
				}
			}
			return len1 - len2;
		}

	}
	
	public static class NoZeroComparator<T> implements Comparator<T> {

		private Comparator<T> original;
		
		public NoZeroComparator(Comparator<T> original) {
			this.original = original;
		}

		@Override
		public int compare(T o1, T o2) {
			int re = original.compare(o1, o2);
			if (re==0 && o1.hashCode()!=o2.hashCode())
				re = o1.hashCode()<o2.hashCode()?1:-1;
			if (re==0 && System.identityHashCode(o1)!=System.identityHashCode(o2))
				re = System.identityHashCode(o1)<System.identityHashCode(o2)?1:-1;
			if (re==0)
				throw new RuntimeException("Collision occured, objects are equal?");
			return re;
		}

	}
	
	public static class ChainedComparator<T> implements Comparator<T> {
		
		private Comparator<T>[] comparators;
		

		public ChainedComparator(Comparator<T>[] comparators) {
			this.comparators = comparators;
		}


		@Override
		public int compare(T o1, T o2) {
			int re = 0;
			for (int i=0; re==0 && i<comparators.length; i++)
				re = comparators[i].compare(o1, o2);
			return re;
		}
	}
	
	
	public static class NaturalComparator<T extends Comparable<? super T>> implements Comparator<T> {

		@Override
		public int compare(T o1, T o2) {
			return o1.compareTo(o2);
		}
		
	}
	
	public static class MappedComparator<F,T> implements Comparator<F> {

		private Function<F,T> mapper;
		private Comparator<T> comp;
		
		
		public MappedComparator(Function<F, T> mapper, Comparator<T> comp) {
			this.mapper = mapper;
			this.comp = comp;
		}


		@Override
		public int compare(F o1, F o2) {
			return comp.compare(mapper.apply(o1),mapper.apply(o2));
		}
		
	}
	
	public static class ArrayComparator<T> implements Comparator<T[]> {
		
		private int[] indices;
		private Comparator[] comps;
		
		public ArrayComparator() {}
		
		public ArrayComparator(int[] indices) {
			this.indices = indices;
		}
		
		public ArrayComparator(int[] indices, Comparator[] comps) {
			this.indices = indices;
			this.comps = comps;
		}

		@Override
		public int compare(T[] a1, T[] a2) {
			if (indices==null) {
				int n = Math.min(a1.length,a2.length);
				for (int i=0; i<n; i++) {
					int r = cmp(i,a1[i],a2[i]);
					if (r!=0)
						return r;
				}
				return a1.length-a2.length;
			} else {
				for (int i : indices) {
					int r = cmp(i,a1[i],a2[i]);
					if (r!=0)
						return r;
				}
				return 0;
			}
		}

		private int cmp(int index,T a, T b) {
			if (comps!=null && index<comps.length && comps[index]!=null)
				return comps[index].compare(a, b);
			return ((Comparable)a).compareTo(b);
		}
	}

	public static class IntArrayComparator implements Comparator<int[]> {
		
		private int[] indices;
		
		public IntArrayComparator() {}
		
		public IntArrayComparator(int[] indices) {
			this.indices = indices;
		}
		
	
		@Override
		public int compare(int[] a1, int[] a2) {
			if (indices==null) {
				int n = Math.min(a1.length,a2.length);
				for (int i=0; i<n; i++) {
					int r = Integer.compare(a1[i],a2[i]);
					if (r!=0)
						return r;
				}
				return a1.length-a2.length;
			} else {
				for (int i : indices) {
					int r = Integer.compare(a1[i],a2[i]);
					if (r!=0)
						return r;
				}
				return 0;
			}
		}
	
	}
	
	public static class BitVectorComparator implements Comparator<BitVector> {

		@Override
		public int compare(BitVector o1, BitVector o2) {
			if (o1.size()!=o2.size())
				return o1.size()-o2.size();
			int r;
			for (int i=o1.size()-1; i>=0; i--) {
				r = (o1.getQuick(i)?1:0) - (o2.getQuick(i)?1:0);
				if (r!=0) return r;
			}
			return 0;
		}
		
	}
	
	/// Suppliers
	
	public static class NewInstanceSupplier<T> implements Supplier<T> {
		private Class<T> cls;
		private Object[] para;
		private Constructor<T> ctor;
		
		public NewInstanceSupplier(Class<T> cls) {
			this.cls = cls;
			try {
				ctor = cls.getConstructor();
			} catch (NoSuchMethodException | SecurityException e) {
			}
		}

		public NewInstanceSupplier(Class<T> cls, Object[] para) {
			this.cls = cls;
			this.para = para;
			m:for (Constructor c : cls.getConstructors()) {
				Class[] p = c.getParameterTypes();
				if (p.length==para.length) {
					for (int i=0; i<p.length; i++)
						if (!p[i].isInstance(para[i]))
							continue m;
					ctor = c;
					return;
				}
			}
		}

		@Override
		public T get() {
			try {
				if (ctor==null) return Orm.create(cls);
				return para==null?ctor.newInstance():ctor.newInstance(para);
			} catch (InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException("Cannot create instance of class "+cls.getName(),e);
			}
		}
		
		
	}
	
	

	/**
	 * Gets the number of iterated items. Attention! This method consumes the iterator,
	 * i.e. after calling this method, the iterator is useless!
	 * @param it the iterator
	 * @return the number of iterated items
	 */
	public static long iteratorSize(Iterator<?> it) {
		int re = 0;
		for (;it.hasNext(); it.next())
			re++;
		return re-1;
	}


	public static <T> Iterator<T> advanceIterator(Iterator<T> it,Predicate<T> until) {
		while (it.hasNext() && !until.test(it.next()));
		return it;
	}
	
	public static <T> Iterator<T> advanceIterator(Iterator<T> it,int n) {
		for (int i=0; i<n && it.hasNext(); i++)
			it.next();
		return it;
	}

	@SuppressWarnings("unchecked")
	public static <T> T[] toArray(Iterator<T> it, int offset, int length, Class<T> cls) {
		T[] re = (T[]) Array.newInstance(cls, length);
		for (int i=0; it.hasNext() && i<offset; i++)
			it.next();
		if (!it.hasNext())
			return re;

		int index = 0;
		while (it.hasNext() && index<length)
			re[index++] = it.next();
		return re;

	}

	@SuppressWarnings("unchecked")
	public static <T> T[] toArray(Iterator<T> it, Class<T> cls) {
		LinkedList<T> re = new LinkedList<T>();
		while (it.hasNext())
			re.add(it.next());
		return re.toArray((T[]) Array.newInstance(cls, re.size()));

	}

	public static <K,V> Map<K,V> intersectMaps(Map<K,V> m1,	Map<K,V> m2,Function<MutablePair<V,V>,V> valueFunction) {
		Map<K,V> re = new HashMap<K, V>();
		for (K k : m1.keySet())
			if (m2.containsKey(k))
				re.put(k, valueFunction.apply(new MutablePair<V,V>(m1.get(k),m2.get(k))));
		return re;
	}

	public static <K,V> Map<K,V> subtractMap(Map<K,V> map, Map<K,V> toSubtract) {
		Map<K,V> re = new HashMap<K, V>();
		for (K k : map.keySet())
			if (!toSubtract.containsKey(k))
				re.put(k, map.get(k));
		return re;
	}

	public static <K,V> Map<K,V> filterMap(Map<K,V> map, Predicate<V> accept) {
		Map<K,V> re = new HashMap<K, V>();
		for (K k : map.keySet())
			if (accept.test(map.get(k)))
				re.put(k, map.get(k));
		return re;
	}



	public static <I,O> DemultiplexIterator<I,O> demultiplexIterator(Iterator<I> it,
			Function<I, Iterator<O>> applyer) {
		return new DemultiplexIterator<I,O>(it,applyer);
	}




	public static <T> long countIterator(Iterator<T> iterator) {
		long re = 0;
		while (iterator.hasNext()) {
			iterator.next();
			re++;
		}
		return re;
	}




	/**
	 * Each iterator must yield a strictly increasing list of item (i.e. two succinct elements must compare to a number >1)
	 * This is for performance reasons not checked!
	 * @param <T>
	 * @param iterators
	 * @param order
	 * @return
	 */
	public static <T> ExtendedIterator<T[]> parallellIterator(Iterator<T>[] iterators, Comparator<? super T> order, Class<T> cls) {
		return new ParallellIterator<T>(iterators,order,cls);
	}

	public static <T> ExtendedIterator<T[]> parallellIterator(Iterator<T> iterator1, Iterator<T> iterator2, Comparator<? super T> order, Class<T> cls) {
		return new ParallellIterator<T>(new Iterator[]{iterator1,iterator2},order,cls);
	}
	
	public static <T> MergeIterator<T> mergeIterator(Iterator<T>[] iterators, Comparator<? super T> comp) {
		return new MergeIterator<T>(iterators,comp);
	}
	
	public static <T> FuseIterator<T,T[]> fuseIterator(Class<T> cls, Iterator<T>... iterators) {
		return new FuseIterator<>(iterators,l->{
			T[] re = (T[]) Array.newInstance(cls, iterators.length);
			for (int i=0; i<re.length; i++)
				re[i] = iterators[i].next();
			return re;
		});
	}
	
	public static <T,R> FuseIterator<T,R> fuseIterator(Function<List<T>,R> map, Iterator<T>... iterators) {
		return new FuseIterator<T,R>(iterators,map);
	}

	public static <T>  Iterator<MutablePair<T, T>> unorderedPairIterator(T[] a, long from, long to, boolean includeId) {
		return new UnorderedPairIterator<T>(a, from, to, includeId);
	}

	public static <T> BufferingIterator<T> bufferingIterator(Iterator<T> it, int s) {
		return new BufferingIterator<T>(it,s);
	}

	public static <T extends CharSequence> CharSequenceComparator<T> charSequenceComparator() {
		return new CharSequenceComparator<T>();
	}

	public static <T> Comparator<T> noZeroComparator(Comparator<T> comparator) {
		return new NoZeroComparator<T>(comparator);
	}

	public static <T> PeekIterator<T> peekIterator(Iterator<T> iterator) {
		return iterator instanceof PeekIterator?(PeekIterator<T>)iterator:new PeekIterator<T>(iterator);
	}

	public static <T> MultiplexIterator<T,T[]> multiplexIterator(Iterator<T> iterator, Comparator<T> comp, Class<? super T> cls) {
		return new MultiplexIterator<T,T[]>(iterator,l->l.toArray((T[]) Array.newInstance(cls, l.size())),comp);
	}

	public static <T> HeadIterator<T> headIterator(Iterator<T> iterator, int n) {
		return new HeadIterator<T>(iterator, n);
	}
	
	/**
	 * Dont use the list itself (applyer == t->t), as it is reused and cleared in next()
	 * Ordering is checked
	 * @param iterator
	 * @param applyer
	 * @param comp
	 * @return
	 */
	public static <T,O> MultiplexIterator<T,O> multiplexIterator(Iterator<T> iterator, Function<List<T>,O> applyer, Comparator<T> comp) {
		return new MultiplexIterator<T,O>(iterator,applyer,comp);
	}
	
	
	public static <T> MultiplexIterator<T,T[]> multiplexIterator(Iterator<T> iterator, BiPredicate<T,T> comp, Class<? super T> cls) {
		return new MultiplexIterator<T,T[]>(iterator,l->l.toArray((T[]) Array.newInstance(cls, l.size())),comp);
	}

	
	/**
	 * Dont use the list itself (applyer == t->t), as it is reused and cleared in next()
	 * Ordering cannot be and is not checked
	 * @param iterator
	 * @param applyer
	 * @param comp 
	 * @return
	 */
	public static <T,O> MultiplexIterator<T,O> multiplexIterator(Iterator<T> iterator, Function<List<T>,O> applyer, BiPredicate<T,T> comp) {
		return new MultiplexIterator<T,O>(iterator,applyer,comp);
	}

	public static <T extends Comparable<? super T>> Comparator<T[]> arrayComparator() {
		return new ArrayComparator<T>();
	}
	
	public static <T> Comparator<T[]> arrayComparator(int...indices) {
		return new ArrayComparator<T>(indices);
	}
	
	public static Comparator<int[]> intArrayComparator() {
		return new IntArrayComparator();
	}
	
	public static Comparator<int[]> intArrayComparator(int...indices) {
		return new IntArrayComparator(indices);
	}
	
	public static <T> Comparator<T[]> arrayComparator(int[] indices, Comparator<? extends T>[] comps) {
		return new ArrayComparator<T>(indices,comps);
	}

	public static <I> ChainedComparator<I> chainedComparator(Comparator<I>... cmp) {
		return new ChainedComparator<I>(cmp);
	}

	public static ExtendedIterator<String> substringIterator(String str, int length) {
		return new SubstringIterator(str,length, true);
	}
	public static ExtendedIterator<String> substringIterator(String str, int length, boolean overlapping) {
		return new SubstringIterator(str,length, overlapping);
	}
	
	public static <T> WritingFunction<T> writingFunction(Writer writer, boolean flush) {
		return new WritingFunction<T>(writer,flush);
	}

	public static BitVectorComparator bitVectorComparator() {
		return new BitVectorComparator();
	}

	public static <T> ExtendedIterator<T> singletonIterator(T element) {
		return new SingletonIterator(element);
	}

	public static <T> ExtendedIterator<T> arrayIterator(T[] elements) {
		return new ArrayIterator<T>(elements);
	}
	
	public static <T> ExtendedIterator<T> arrayIterator(T[] elements, int start, int end) {
		return new ArrayIterator<T>(elements,start, end);
	}
	
	public static <T> ExtendedIterator<T> factoryIterator(int size, IntFunction<T> factory) {
		return new FactoryIterator<T>(size,factory);
	}
	
	public static <T> BlockIterator<T,ArrayList<T>> blockIterator(Iterator<T> it, Predicate<T> first) {
		return new BlockIterator<T,ArrayList<T>>(it,first,ArrayList::new,ArrayList<T>::add);
	}
	
	public static <T,C> BlockIterator<T,C> blockIterator(Iterator<T> it, Predicate<T> first, Supplier<C> blockSupplier, BiConsumer<C, T> adder) {
		return new BlockIterator<>(it,first,blockSupplier,adder);
	}

	/**
	 * Keep everything where the predicate says true!
	 * @param it
	 * @param predicate
	 * @return
	 */
	public static <T> FilteredIterator<T> filteredIterator(Iterator<T> it,Predicate<? super T> predicate) {
		return new FilteredIterator<>(it,predicate);
	}
	
	public static <S,T> MappedIterator<S,T> mappedIterator(Iterator<S> it,Function<S,T> mapper) {
		return new MappedIterator<>(it,mapper);
	}
	public static <S> ToIntMappedIterator<S> mappedIntIterator(Iterator<S> it,ToIntFunction<S> mapper) {
		return new ToIntMappedIterator<>(it,mapper);
	}
	public static <S> ToDoubleMappedIterator<S> mappedDoubleIterator(Iterator<S> it,ToDoubleFunction<S> mapper) {
		return new ToDoubleMappedIterator<>(it,mapper);
	}
	
	public static <T> ExtendedIterator<T> extend(Iterator<T> it) {
		return new ExtendedIteratorAdapter<>(it);
	}
	public static <T> UnifySequentialIterator<T> unifySequentialIterator(Iterator<T> it,Comparator<? super T> comp) {
		return new UnifySequentialIterator<>(it,comp);
	}
	
	public static <T> UnifyNonSequentialIterator<T> unifyNonSequentialIterator(Iterator<T> it) {
		return new UnifyNonSequentialIterator<>(it);
	}
	public static <T> ChainedIterator<T> chainedIterator(Iterator<T>... it) {
		return new ChainedIterator<>(it);
	}
	public static <T,R> ExtendedIterator<R> reduceIterator(Iterator<T> it,Comparator<? super T> comp,Supplier<R> supplier, BiFunction<T, R, R> reducer) {
		return new ReduceIterator<>(it,comp,supplier,reducer);
	}

	/**
	 * it must yield elements sorted according to weak. Strong must be consistent with weak w.r.t. non 0 comparisons, but 
	 * should yield non 0 comparisons for 0 comparisons of weak. The returned iterator yields elements sorted according to strong.
	 * @param it
	 * @param weak
	 * @param strong
	 * @return
	 */
	public static <T> ResortIterator<T> resortIterator(Class<T> cls,
			Iterator<T> it,
			Comparator<T> weak,
			Comparator<? super T> strong, boolean nestStrong) {
		return new ResortIterator<T>(it,cls,weak,nestStrong?weak.thenComparing(strong):strong);
	}
	
	public static <T> ResortIterator<T> resortIterator(BinarySerializer<T> serializer,
			Iterator<T> it,
			Comparator<T> weak,
			Comparator<? super T> strong, boolean nestStrong) {
		return new ResortIterator<T>(it,serializer,weak,nestStrong?weak.thenComparing(strong):strong);
	}


	public static <T> SideEffectIterator<T> sideEffectIterator(Iterator<T> it, Consumer<T> effect) {
		return new SideEffectIterator<>(it,effect);
	}
	
	public static <T> SideEffectIterator<T> sideEffectIterator(Iterator<T> it, Predicate<T> pred, Consumer<T> effect) {
		return new SideEffectIterator<>(it,pred,effect);
	}
	
	public static <T> InitActionIterator<T> initActionIterator(Iterator<T> it, Consumer<T> effect) {
		return new InitActionIterator<>(it,effect);
	}
	
	public static <T> ParallelizedSideEffectIterator<T> parallelizedSideEffectIterator(Iterator<T> it, int capacity, Consumer<T> effect) {
		return new ParallelizedSideEffectIterator<>(it,capacity, effect);
	}
	
	public static <T> HasNextActionIterator<T> hasNextActionIterator(Iterator<T> it, BooleanUnaryOperator op) {
		return new HasNextActionIterator<>(it,op);
	}


	public static <T> NewInstanceSupplier<T> newInstanceSupplier(Class<T> cls) {
		return new NewInstanceSupplier<T>(cls);
	}
	
	public static <T> NewInstanceSupplier<T> newInstanceSupplier(Class<T> cls, Object...para) {
		return new NewInstanceSupplier<T>(cls,para);
	}


	public static <T extends Comparable<? super T>> Comparator<T> naturalComparator() {
		return new NaturalComparator<T>();
	}

	public static <F,T extends Comparable<? super T>> Comparator<F> mappedComparator(Function<F,T> mapper) {
		return new MappedComparator<F,T>(mapper,naturalComparator());
	}
	
	public static <F,T> Comparator<F> mappedComparator(Function<F,T> mapper, Comparator<T> comparator) {
		return new MappedComparator<F,T>(mapper,comparator);
	}

	public static Comparator<String> caseInsensitiveComparator() {
		return (a,b)->a.compareToIgnoreCase(b);
	}


	public static <T,R> Function<T,R> ormExtractFunction(int index) {
		return new OrmExtractFunction<T,R>(index);
	}


	public static <F,T> Function<F, T> asFunction(Map<F, T> map) {
		return k->map.get(k);
	}



	/**
	 * Items are compared according to iteration order (when comparable).
	 * @param a
	 * @param b
	 * @return
	 */
	public static int compareCollections(Collection a, Collection b) {
		Iterator it1 = a.iterator();
		Iterator it2 = b.iterator();

		while (it1.hasNext() && it2.hasNext()) {
			Object o1 = it1.next();
			Object o2 = it2.next();
			int r = tryCompare(o1,o2);
			if (r!=0)
				return r;
		}
		if (it1.hasNext()) return 1;
		if (it2.hasNext()) return -1;
		return 0;
	}


	public static int tryCompare(Object o1, Object o2) {
		if (o1 instanceof Comparable && o2 instanceof Comparable)
			return ((Comparable)o1).compareTo(o2);
		return 0;
	}


	public static <F,T> Function<F, T> constantFunction(
			T c) {
		return (o)->c;
	}


	public static <T> AlternatingIterator<T> alternating(ExtendedIterator<T> a, ExtendedIterator<? extends T>... r) {
		return new AlternatingIterator<T>(a,r);
	}


	public static <T> ExtendedIterator<T> patternIterator(Iterator<T> parent, boolean[] pattern) {
		return new PatternIterator<T>(parent, pattern);
	}
	
}
