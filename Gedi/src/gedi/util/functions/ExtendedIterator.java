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

package gedi.util.functions;

import gedi.core.data.table.Table;
import gedi.core.data.table.TableType;
import gedi.core.data.table.Tables;
import gedi.util.ArrayUtils;
import gedi.util.FunctorUtils;
import gedi.util.FunctorUtils.AlternatingIterator;
import gedi.util.FunctorUtils.BlockIterator;
import gedi.util.FunctorUtils.BufferingIterator;
import gedi.util.FunctorUtils.ChainedIterator;
import gedi.util.FunctorUtils.DemultiplexIterator;
import gedi.util.FunctorUtils.FilteredIterator;
import gedi.util.FunctorUtils.FuseIterator;
import gedi.util.FunctorUtils.MergeIterator;
import gedi.util.FunctorUtils.MultiplexIterator;
import gedi.util.FunctorUtils.ParallelizedSideEffectIterator;
import gedi.util.FunctorUtils.ParallellIterator;
import gedi.util.FunctorUtils.PeekIterator;
import gedi.util.FunctorUtils.ResortIterator;
import gedi.util.FunctorUtils.SideEffectIterator;
import gedi.util.FunctorUtils.ToDoubleMappedIterator;
import gedi.util.FunctorUtils.ToIntMappedIterator;
import gedi.util.FunctorUtils.UnifySequentialIterator;
import gedi.util.ReflectionUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.collections.bitcollections.BitList;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.datastructure.collections.doublecollections.DoubleIterator;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.datastructure.collections.intcollections.IntIterator;
import gedi.util.io.randomaccess.serialization.BinarySerializer;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.mutable.Mutable;
import gedi.util.mutable.MutableInteger;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.Progress;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;


public interface ExtendedIterator<T> extends Iterator<T> {

	default ChainedIterator<T> chain(Iterator<T> other) {
		return FunctorUtils.chainedIterator(this,other);
	}
	
	default <A> ExtendedIterator<A> map(Function<T,A> mapper) {
    	return FunctorUtils.mappedIterator(this, mapper);
    }
	
	default ToIntMappedIterator<T> mapToInt(ToIntFunction<T> mapper) {
    	return FunctorUtils.mappedIntIterator(this, mapper);
    }
	
	default ToDoubleMappedIterator<T> mapToDouble(ToDoubleFunction<T> mapper) {
    	return FunctorUtils.mappedDoubleIterator(this, mapper);
    }

	default ExtendedIterator<T> removeNulls() {
		return FunctorUtils.filteredIterator(this, e->e!=null);
	}
	
	default T first() {
		if (hasNext()) return next();
		return null;
	}
	
	default T getUniqueResult(boolean throwOnMore, boolean throwOnNone) {
		return getUniqueResult(throwOnMore?"More than one result available!":null, throwOnNone?"No result available!":null);
	}
	
	default T getUniqueResult(String moreMessage, String noneMessage) {
		if (hasNext()) {
			T re = next();
			if (hasNext()) {
				if (moreMessage!=null)
					throw new RuntimeException(moreMessage);
				return null;
			}
			return re;
		}
		if (noneMessage!=null)
			throw new RuntimeException(noneMessage);
		return null;
	}
	
	default ExtendedIterator<T> getUnique(boolean throwOnMore, boolean throwOnNone) {
		return getUnique(throwOnMore?"More than one result available!":null, throwOnNone?"No result available!":null);
	}
	
	default ExtendedIterator<T> getUnique(String moreMessage, String noneMessage) {
		if (hasNext()) {
			T re = next();
			if (hasNext()) {
				if (moreMessage!=null)
					throw new RuntimeException(moreMessage);
				return EI.empty();
			}
			return EI.singleton(re);
		}
		if (noneMessage!=null)
			throw new RuntimeException(noneMessage);
		return EI.empty();
	}
	
	
	default T last() {
		T re = null;
		while (hasNext()) re = next();
		return re;
	}
	
	
	default void drain() {
		while (hasNext()) 
			next();
	}
	
	
	/**
	 * Only the ones with pred.test(?)=true remain!
	 * @param mapper
	 * @return
	 */
	default FilteredIterator<T> filter(Predicate<? super T> pred) {
    	return FunctorUtils.filteredIterator(this, pred);
    }
	
	default <O> ExtendedIterator<O> instanceOf(Class<O> cls) {
    	return filter(x->cls.isInstance(x)).cast(cls);
    }
	
	default <O> ExtendedIterator<O> cast(Class<O> cls) {
    	return map(x->(O)cls.cast(x));
    }
	
	default <O> ExtendedIterator<O> castFiltered(Class<O> cls) {
    	return filter(x->cls.isInstance(x)).map(x->(O)cls.cast(x));
    }

	default DoubleIterator castDouble() {
    	return mapToDouble(x->(Double)x);
    }

	default IntIterator castInt() {
    	return mapToInt(x->(Integer)x);
    }
	
	
	default <O> ExtendedIterator<O> parallelized(Function<ExtendedIterator<T>,ExtendedIterator<O>> sub) {
		return new ParallelizedIterator<T, O,Void>(this, sub);
	}
	
	default <O> ExtendedIterator<O> parallelized(int threads, int blocksize, Function<ExtendedIterator<T>,ExtendedIterator<O>> sub) {
		if (threads<=0) return sub.apply(this);
		return new ParallelizedIterator<T, O,Void>(this, threads, blocksize, null, sub);
	}

	default <O,S> ParallelizedIterator<T,O,S> parallelized(int threads, int blocksize, Supplier<S> stateMaker, BiFunction<ExtendedIterator<T>,S,ExtendedIterator<O>> sub) {
		return new ParallelizedIterator<T, O,S>(this, threads, blocksize, null, stateMaker, sub);
	}

	default <O> ExtendedIterator<O> checkParallelized(int threads, int blocksize, BiFunction<O,O,String> checker, Function<ExtendedIterator<T>,ExtendedIterator<O>> sub) {
		if (threads==0) return sub.apply(this);
		return new ParallelizedIterator<T, O,Void>(this, threads, blocksize, checker, sub);
	}
	
	default ExtendedIterator<T> initAction(Consumer<T> initial) {
		return FunctorUtils.initActionIterator(this,initial);
	}
	
	default ExtendedIterator<T> progress() {
		return progress(new ConsoleProgress(),-1,(t)->t.toString());
	}

	default ExtendedIterator<T> progress(Function<T,String> description) {
		return progress(new ConsoleProgress(),-1,description);
	}

	
	default ExtendedIterator<T> progress(int count) {
		return progress(new ConsoleProgress(),count,(t)->t.toString());
	}
	
	default ExtendedIterator<T> progress(Progress progress, int count, Function<T,String> description) {
		return initAction(r->{
			progress.init();
			if (count>=0) progress.setCount(count);
		}).sideEffect(t->progress.setDescription(()->description.apply(t)).incrementProgress()).endAction(()->progress.finish());
	}
	
	default ExtendedIterator<T> progress(Progress progress, MutableInteger count, Function<T,String> description) {
		return initAction(r->{
			progress.init();
			if (count.N>=0) progress.setCount(count.N);
		}).sideEffect(t->progress.setDescription(()->description.apply(t)).incrementProgress()).endAction(()->progress.finish());
	}
	
	default BlockIterator<T,ArrayList<T>> block(Predicate<T> first) {
    	return FunctorUtils.blockIterator(this, first);
    }
	default <C> BlockIterator<T,C> block(Predicate<T> first, Supplier<C> blockSupplier, BiConsumer<C, T> adder) {
    	return FunctorUtils.blockIterator(this, first, blockSupplier, adder);
    }
	
	
	default AlternatingIterator<T> alternating(ExtendedIterator<? extends T>... others) {
		return FunctorUtils.alternating(this,others);
	}
	
	default BufferingIterator<T> buffer(int n) {
		return FunctorUtils.bufferingIterator(this, n);
	}
	
	default <O> DemultiplexIterator<T,O> demultiplex(Function<T,Iterator<O>> demulti) {
		return FunctorUtils.demultiplexIterator(this, demulti);
	}
	
	default <O> DemultiplexIterator<T,O> unfold(Function<T,Iterator<O>> demulti) {
		return FunctorUtils.demultiplexIterator(this, demulti);
	}
	
	default MergeIterator<T> merge(Comparator<T> comp, Iterator<T>...iterators) {
		Iterator<T>[] it = new Iterator[iterators.length+1];
		it[0] = this;
		System.arraycopy(iterators, 0, it, 1, iterators.length);
    	return FunctorUtils.mergeIterator(it,comp);
    }
	
	default FuseIterator<T> fuse(Class<T> cls, Iterator<T>...iterators) {
		Iterator<T>[] it = new Iterator[iterators.length+1];
		it[0] = this;
		System.arraycopy(iterators, 0, it, 1, iterators.length);
    	return FunctorUtils.fuseIterator(cls,it);
    }
	
	
	default ExtendedIterator<T[]> parallel(Comparator<T> comp, Class<T> cls, Iterator<T>...iterators) {
		Iterator<T>[] it = new Iterator[iterators.length+1];
		it[0] = this;
		System.arraycopy(iterators, 0, it, 1, iterators.length);
    	return FunctorUtils.parallellIterator(it, comp, cls);
    }
	
	default ExtendedIterator<T[]> parallel(Comparator<T> comp, Class<T> cls, Iterator<T> iterator) {
    	return FunctorUtils.parallellIterator(this, iterator, comp, cls);
    }
	
	default MergeIterator<T> merge(Comparator<T> comp, Iterator<T> iterator) {
		Iterator<T>[] it = new Iterator[]{this,iterator};
    	return FunctorUtils.mergeIterator(it,comp);
    }
	
	default MultiplexIterator<T,T[]> multiplex(Comparator<T> comp, Class<? super T> cls) {
    	return FunctorUtils.multiplexIterator(this, comp, cls);
    }
	
	default MultiplexIterator<T,T[]> multiplexUnsorted(BiPredicate<T,T> comp, Class<? super T> cls) {
    	return FunctorUtils.multiplexIterator(this, comp, cls);
    }
	
	/**
	 * Dont use the list itself (applyer == t->t), as it is reused and cleared in next()
	 * Ordering cannot be and is not checked
	 * @param iterator
	 * @param applyer
	 * @param comp 
	 * @return
	 */
	default <O> MultiplexIterator<T,O> multiplexUnsorted(BiPredicate<T,T> comp, Function<List<T>,O> applyer) {
		return FunctorUtils.multiplexIterator(this,applyer,comp);
	}
	
	/**
	 * Dont use the list itself (applyer == t->t), as it is reused and cleared in next()
	 * @param iterator
	 * @param applyer
	 * @param comp 
	 * @return
	 */
	default <O> MultiplexIterator<T,O> multiplex(Comparator<T> comp, Function<List<T>,O> applyer) {
		return FunctorUtils.multiplexIterator(this,applyer,comp);
	}
	
	
	default PeekIterator<T> peeking() {
    	return FunctorUtils.peekIterator(this);
    }
	
	default <R> ExtendedIterator<R> fold(Comparator<? super T> comp,Supplier<R> supplier, BiFunction<T, R, R> reducer) {
		return FunctorUtils.reduceIterator(this, comp, supplier, reducer);
	}
	
	default ArrayList<T> list() {
		return toCollection(new ArrayList<T>());
	}
	default HashSet<T> set() {
		return toCollection(new HashSet<T>());
	}
	
	default <R> R add(R o)  {
		for (Method m : o.getClass().getMethods()) {
			if (m.getName().equals("add") && m.getParameterTypes().length==1) {
				while (hasNext())
					try {
						m.invoke(o, next());
					} catch (IllegalAccessException | IllegalArgumentException
							| InvocationTargetException e) {
						throw new RuntimeException("Could not invoke add method!",e);
					}
				return o;	
			}
		}
		throw new RuntimeException(o.getClass()+" does not have an add method!");
	}
	
	default <R extends Collection<? super T>> R toCollection(R collection) {
		while (hasNext()) 
			collection.add(next());
		return collection;
	}
	
	default <K,V,R extends Map<K,V>> R  toMap(R map, Function<T,K> key, Function<T,V> value) {
		while (hasNext()) {
			T t = next();
			map.put(key.apply(t),value.apply(t));
		}
		return map;
	}
	
	default <R extends Collection<? super T>> ExtendedIterator<? super T> wrap(R collection) {
		while (hasNext()) 
			collection.add(next());
		return EI.wrap(collection.iterator());
	}
	
	default void append(String path) throws IOException {
		LineWriter lw = new LineOrientedFile(path).append();
		print(lw);
		lw.close();
	}
	
	default void print(String header, String path) throws IOException {
		LineWriter lw = new LineOrientedFile(path).write();
		lw.writeLine(header);
		print(lw);
		lw.close();
	}
	
	default void print(String path) throws IOException {
		LineWriter lw = new LineOrientedFile(path).write();
		print(lw);
		lw.close();
	}
	
	default void print(LineWriter writer) throws IOException {
		while (hasNext())
			writer.writeLine(StringUtils.toString(next()));
		writer.flush();
	}
	
	default void print(OutputStream stream) {
		PrintWriter p = new PrintWriter(stream);
		while (hasNext())
			p.println(StringUtils.toString(next()));
		p.flush();
	}
	
	default void print(Writer wr) {
		PrintWriter p = new PrintWriter(wr);
		while (hasNext())
			p.println(StringUtils.toString(next()));
		p.flush();
	}
	
	default void print() {
		print(System.out);
	}
	
	default void log(Logger log, Level level) {
		while (hasNext())
			log.log(level,StringUtils.toString(next()));
	}
	
//	default Table<T> toRTable() {
//		return toTable(TableType.R,StringUtils.createRandomIdentifier(10));
//	}
	
	default Table<T> toTable() {
		return toTable(TableType.Temporary,StringUtils.createRandomIdentifier(10));
	}
	
	default Table<T> toTable(TableType type, String name) {
		PeekIterator<T> it = peeking();
		Class<T> cls = (Class<T>) it.peek().getClass();
		Function mapper = t->t;
		Class<T> mcls = Mutable.getMutable(cls);
		if (mcls!=null) {
			mapper = Mutable.getConverter(cls);
			cls = mcls;
		}
		
		Table tab = Tables.getInstance().createOrOpen(type, Tables.getInstance().buildMeta(name, "EI", cls));
		tab.beginAddBatch();
		while (it.hasNext())
			tab.add(mapper.apply(it.next()));
		
		tab.endAddBatch();
		return tab;
	}
	
	default T reduce(BinaryOperator<T> reducer) {
		T re = null;
		while (hasNext()) 
			re = re==null?next():reducer.apply(next(), re);
		return re;
	}

	
	default <R> R reduce(BiFunction<T, R, R> reducer) {
		R re = null;
		while (hasNext()) 
			re = reducer.apply(next(), re);
		return re;
	}
	
	default <R> R reduce(R start, BiFunction<T, R, R> reducer) {
		R re = start;
		while (hasNext()) 
			re = reducer.apply(next(), re);
		return re;
	}
	
	default String concat(String sep) {
		return concat(sep,s->s.toString());
	}
	
	default String concat(String sep, Function<T,String> stringer) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		while (hasNext()) {
			if (!first) sb.append(sep);
			sb.append(stringer.apply(next()));
			first = false;
		}
		return sb.toString();
	}
	
	
	/**
	 * it must yield elements sorted according to weak. Strong must be consistent with weak w.r.t. non 0 comparisons, but 
	 * should yield non 0 comparisons for 0 comparisons of weak. The returned iterator yields elements sorted according to strong.
	 * @param it
	 * @param weak
	 * @param strong
	 * @return
	 */
	default  ResortIterator<T> resort(Class<T> cls,
			Comparator<T> weak,
			Comparator<? super T> strong, boolean nestStrong) {
		return FunctorUtils.resortIterator(cls,this,weak,strong,nestStrong);
	}
	
	default  ResortIterator<T> resort(
			Comparator<T> weak,
			Comparator<? super T> strong, boolean nestStrong) {
		PeekIterator<T> it = peeking();
		Class<T> cls = (Class<T>) it.peek().getClass();
		return FunctorUtils.resortIterator(cls,it,weak,strong,nestStrong);
	}
	
	default  ResortIterator<T> sort(Class<T> cls, 
			Comparator<? super T> comp) {
		return FunctorUtils.resortIterator(cls, this,(a,b)->0,comp, false);
	}
	
	default  ResortIterator<T> sort(BinarySerializer<T> serializer, 
			Comparator<? super T> comp) {
		return FunctorUtils.resortIterator(serializer, this,(a,b)->0,comp, false);
	}
	
	
	default ExtendedIterator<T> sort(Comparator<? super T> comp) {
		if (!hasNext()) return this;
		
		PeekIterator<T> it = peeking();
		Class<T> cls = (Class<T>) it.peek().getClass();
		return it.sort(cls,comp);
	}
	
	
	default  ExtendedIterator<T> sort(Class<T> cls) {
		return sort(cls,(Comparator)FunctorUtils.naturalComparator());
	}
	
	default  ExtendedIterator<T> sort(BinarySerializer<T> serializer) {
		return sort(serializer,(Comparator)FunctorUtils.naturalComparator());
	}
	
	default ExtendedIterator<T> sort() {
		if (!hasNext()) return this;
		PeekIterator<T> it = peeking();
		Class<T> cls = (Class<T>) it.peek().getClass();
		return it.sort(cls,(Comparator)FunctorUtils.naturalComparator());
	}
	
	default ExtendedIterator<T[]> group(Comparator<T> comp, Class<T> cls) {
		return sort(cls,comp).multiplex(comp, cls);
	}
	
	default <C extends Comparable<C>> ExtendedIterator<T[]> group(Function<T,C> field, Class<T> cls) {
		Comparator<T> comp = (a,b)->field.apply(a).compareTo(field.apply(b));
		return sort(cls,comp).multiplex(comp, cls);
	}
	
	default SideEffectIterator<T> sideEffect(Consumer<T> effect) {
		return FunctorUtils.sideEffectIterator(this,effect);
	}
	
	
	default ParallelizedSideEffectIterator<T> parallelizedSideEffect(Consumer<T> effect, int capacity) {
		return FunctorUtils.parallelizedSideEffectIterator(this,capacity,effect);
	}
	
	default ExtendedIterator<T> skip(int n, Consumer<T> action) {
		for (int i=0; i<n && hasNext(); i++) action.accept(next());
		return this;
	}
	
	default ExtendedIterator<T> pattern(boolean...pattern) {
		return FunctorUtils.patternIterator(this,pattern);
	}

	
	default ExtendedIterator<T> head(int n) {
		return FunctorUtils.headIterator(this,n);
	}
	
	
	/** 
	 * Skips all entries where the predicate says true (i.e. the next entry is the first one that says false!)
	 * @param skip
	 * @return
	 */
	default ExtendedIterator<T> skip(Predicate<T> skip, Consumer<T> action) {
		PeekIterator<T> it = peeking();
		while (it.hasNext() && skip.test(it.peek()))
			action.accept(it.next());
		return it;
	}
	
	default ExtendedIterator<T> skip(int n) {
		return skip(n,l->{});
	}
	
	/** 
	 * Skips all entries where the predicate says true (i.e. the next entry is the first one that says false!)
	 * @param skip
	 * @return
	 */
	default ExtendedIterator<T> skip(Predicate<T> skip) {
		return skip(skip,l->{});
	}
	
	
	
	default ExtendedIterator<T> iff(boolean condition, UnaryOperator<ExtendedIterator<T>> op) {
		if (condition) return op.apply(this);
		return this;
	}
	
	default <O> ExtendedIterator<T> endAction(Runnable action){
		
		return hasNextAction(new BooleanUnaryOperator() {
			private boolean run = false;
			@Override
			public boolean applyAsBoolean(boolean hasnext) {
				if (!hasnext && !run) {
					action.run();
					run = true;
				}
				return hasnext;
			}
		});
	}
	
	default <O> ExtendedIterator<T> hasNextAction(BooleanUnaryOperator op){
		return FunctorUtils.hasNextActionIterator(this, op);
	}
	
	default Object[] toArray() {
		return toCollection(new LinkedList<T>()).toArray();
	}
	
	/**
	 * if a matches exactly the number of elements, a is returned; otherwise a new array is returned!
	 * @param a
	 * @return
	 */
	@SuppressWarnings("unchecked")
	default T[] toArray(T[] a) {
		LinkedList<T> re = new LinkedList<T>();
		int index = 0;
		while (hasNext()){
			if (index<a.length)
				a[index++] = next();
			else
				re.add(next());
		}
		if (re.size()==0 && index==a.length)return a;
		T[] r = (T[]) Array.newInstance(a.getClass().getComponentType(), index+re.size());
		System.arraycopy(a, 0, r, 0, index);
		if (re.size()>0) {
			for (T t : re)
				r[index++] = t;
		}
		return r;
	}
	
	default NumericArray toNumericArray() {
		return NumericArray.wrap(toDoubleArray());
	}
	
	default double[] toDoubleArray() {
		DoubleArrayList re = new DoubleArrayList();
		while (hasNext())
			re.add(((Number)next()).doubleValue());
		return re.toDoubleArray();
	}
	
	default int[] toIntArray() {
		IntArrayList re = new IntArrayList();
		while (hasNext())
			re.add(((Number)next()).intValue());
		return re.toIntArray();
	}
	
	default boolean[] toBooleanArray() {
		BitList re = new BitList();
		while (hasNext())
			re.add((Boolean)next());
		return re.toBooleanArray();
	}
	
	default T[] toArray(Class<T> cls) {
		cls = ReflectionUtils.toBoxClass(cls);
		LinkedList<T> re = new LinkedList<T>();
		while (hasNext())
			re.add(next());
		return re.toArray((T[]) Array.newInstance(cls, re.size()));
	}
	
	/**
	 * if a matches exactly the number of elements, a is returned; otherwise a new array is returned!
	 * @param a
	 * @return
	 */
	default LinkedList<T> toList() {
		LinkedList<T> re = new LinkedList<T>();
		while (hasNext()){
			re.add(next());
		}
		return re;
	}
	
	/**
	 * Input must be sorted, sorting order is checked if comp is given!
	 * @param comp
	 * @return
	 */
	default UnifySequentialIterator<T> unique(Comparator<? super T> comp) {
		return FunctorUtils.unifySequentialIterator(this,comp);
	}
	
	/**
	 * if sequential, only subsequent equal objects are skipped. Otherwise all objects that already occurred are skipped (may be memory consuming!) 
	 * @param sequential
	 * @return
	 */
	default ExtendedIterator<T> unique(boolean sequential) {
		if (sequential)
			return FunctorUtils.unifySequentialIterator(this,null);
		else
			return FunctorUtils.unifyNonSequentialIterator(this);
	}
	
//	public static <T> ExtendedIterator<T> wrap(Spliterator<T> spl) {
//		return wrap(Spliterators.iterator(spl));
//	}
//	
//	public static <T> ExtendedIterator<T> wrap(Iterable<T> itr) {
//		return wrap(itr.iterator());
//	}
//	
//	public static <T> ExtendedIterator<T> wrap(Iterator<T> it) {
//		return new ExtendedIterator<T>() {
//
//			@Override
//			public boolean hasNext() {
//				return it.hasNext();
//			}
//
//			@Override
//			public T next() {
//				return it.next();
//			}
//			public void remove() {
//				it.remove();
//			}
//		};
//	}
//	public static <T> ExtendedIterator<T> wrap(T[] a) {
//		return FunctorUtils.arrayIterator(a);
//	}
//	
//	public static <T> ExtendedIterator<T> wrap(T[] a, int start, int end) {
//		return FunctorUtils.arrayIterator(a,start,end);
//	}
	
	/**
	 * Returns an {@link Iterable} that can only be used once (and returns this).
	 * If you want to use an iterator chain in a for loop, use this.
	 * @return
	 */
	default Iterable<T> loop() {
		return new Iterable<T>() {

			@Override
			public Iterator<T> iterator() {
				return ExtendedIterator.this;
			}
			
		};
	}
	
	
	/**
	 * Checks whether there is a next and throws the given Exception if there is nothing
	 * @param e
	 * @return
	 * @throws E
	 */
	default <E extends Exception> T next(E e) throws E {
		if (!hasNext())
			throw e;
		return next();
	}
	
	default long count() {
		return FunctorUtils.countIterator(this);
	}

	default void retainAll(Predicate<T> predicate) {
		while (hasNext())
			if (!predicate.test(next()))
				remove();
	}
	
	default HashMap<T,Integer> indexPosition() {
		return indexPosition(t->t);
	}

	default <K> HashMap<K,Integer> indexPosition(Function<? super T,? extends K> key) {
		HashMap<K,Integer> re = new HashMap<K, Integer>();
		int p = 0;
		while (hasNext()) {
			re.put(key.apply(next()), p++);
		}
		return re;
	}


	default <K> HashMap<K,T> index(Function<? super T,? extends K> key) {
		return ArrayUtils.index(this, key, v->v);
	}
	
	default <K> HashMap<K,T> indexOverwrite(Function<? super T,? extends K> key) {
		return ArrayUtils.index(this, key, v->v,true);
	}
	
	default <K> HashMap<K,ArrayList<T>> indexMulti(Function<? super T,? extends K> key) {
		return ArrayUtils.indexMulti(this, key, v->v);
	}
	
	
	default <K> HashMap<K,T> indexMultiCombine(Function<? super T,? extends K> key, TriFunction<? super K,? super T, ? super T, ? extends T> combiner) {
		return ArrayUtils.indexCombine((Iterator<T>)this, key, v->v, combiner);
	}
	
	default <K> HashMap<K,T> indexSmallest(Function<? super T,? extends K> key, Comparator<T> takeSmallest) {
		return ArrayUtils.indexSmallest((Iterator<T>)this, key, v->v, takeSmallest);
	}
	
	default <K> HashMap<K,T> indexAdapt(Function<? super T,? extends K> key, TriFunction<Integer,? super T,? super K,? extends K> newKey) {
		return ArrayUtils.indexAdapt((Iterator<T>)this, key, v->v, newKey);
	}
	
	default <K,V> HashMap<K,V> index(Function<? super T,? extends K> key,Function<? super T,? extends V>  value) {
		return ArrayUtils.index(this, key, value);
	}
	
	default <K,V> HashMap<K,V> indexOverwrite(Function<? super T,? extends K> key,Function<? super T,? extends V>  value) {
		return ArrayUtils.index(this, key, value,true);
	}
	
	default <K,V> HashMap<K,ArrayList<V>> indexMulti(Function<? super T,? extends K> key,Function<? super T,? extends V>  value) {
		return ArrayUtils.indexMulti(this, key, value);
	}
	
	default <K,V,C extends Collection<? super V>> HashMap<K,C> indexMulti(Function<? super T,? extends K> key,Function<? super T,? extends V>  value, Function<? super K,? extends C> multi) {
		return ArrayUtils.indexMulti((Iterator<T>)this, key, value, multi);
	}
	
	default <K,V> HashMap<K,V> indexMultiCombine(Function<? super T,? extends K> key,Function<? super T,? extends V>  value, TriFunction<? super K,? super V, ? super V, ? extends V> combiner) {
		return ArrayUtils.indexCombine((Iterator<T>)this, key, value, combiner);
	}
	
	default <K,V> HashMap<K,V> indexSmallest(Function<? super T,? extends K> key,Function<? super T,? extends V>  value, Comparator<V> takeSmallest) {
		return ArrayUtils.indexSmallest((Iterator<T>)this, key, value, takeSmallest);
	}
	
	default <K,V> HashMap<K,V> indexAdapt(Function<? super T,? extends K> key,Function<? super T,? extends V>  value, TriFunction<Integer,? super T,? super K,? extends K> newKey) {
		return ArrayUtils.indexAdapt((Iterator<T>)this, key, value, newKey);
	}
	
	
	

	default void removeAll(Predicate<T> predicate) {
		while (hasNext()) {
			if (predicate.test(next()))
				remove();
		}
	}

	
	default void throwArg(Predicate<? super T> test, String format) {
		while (hasNext()) {
			T o = next();
			if (!test.test(o))
				throw new IllegalArgumentException(String.format(format,o));
		}
	}
	
	default void throwRuntime(Predicate<? super T> test, String format) {
		while (hasNext()) {
			T o = next();
			if (!test.test(o))
				throw new RuntimeException(String.format(format,o));
		}
	}
	
	default void throwIO(Predicate<? super T> test, String format) throws IOException {
		while (hasNext()) {
			T o = next();
			if (!test.test(o))
				throw new IOException(String.format(format,o));
		}
	}
	
	default <E extends Throwable> void throwException(Predicate<? super T> test, String format, Class<E> ex) throws E {
		while (hasNext()) {
			T o = next();
			if (!test.test(o))
				try {
					throw ex.getConstructor(String.class).newInstance(String.format(format,o));
				} catch (InstantiationException | IllegalAccessException
						| IllegalArgumentException | InvocationTargetException
						| NoSuchMethodException | SecurityException e) {
					throw new RuntimeException("Cannot create exception!",new RuntimeException(String.format(format,o)));
				}
		}
	}

	default String toString(int maxLength) {
		maxLength = Math.max(maxLength, 3);
		StringBuilder sb = new StringBuilder();
		while (hasNext()) {
			sb.append(StringUtils.toString(next()));
			if (sb.length()>maxLength) 
				return sb.delete(maxLength-3, sb.length()).append("...").toString();
		}
		return sb.toString();
			
	}

	
	
}
