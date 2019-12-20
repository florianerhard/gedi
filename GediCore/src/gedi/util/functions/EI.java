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

import gedi.util.FunctorUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.collections.doublecollections.DoubleIterator;
import gedi.util.datastructure.collections.intcollections.IntIterator;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializer;
import gedi.util.io.text.LineIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.mutable.MutableTuple;
import jdk.nashorn.internal.objects.NativeArray;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.regex.Matcher;


public interface EI {
	
	
	public static StringIterator split(String s, char separator) {
		if (s.length()==0)
			return StringIterator.empty();
		
		return new StringIterator() {
			int p = 0;
			int sepIndex=s.indexOf(separator);
			@Override
			public String next() {
				String re;
				if (sepIndex>=0) {
					re = s.substring(p,p+sepIndex);
					p += sepIndex+1;
					sepIndex = s.substring(p).indexOf(separator);
				} else {
					re = s.substring(p);
					p = -1;
				}
				return re;
			}
			
			@Override
			public boolean hasNext() {
				return sepIndex>=0 || p>=0;
			}
		};
	}
	
	
	public static IntIterator seq(int start, int end) {
		return seq(start,end,end>=start?1:-1);
	}
	public static <T> IntIterator along(T[] a) {
		return seq(0,a.length);
	}
	
	public static IntIterator seq(int start, int end, int step) {
		if (step==0) step = end>=start?1:-1;
		int ustep = step;
		return new IntIterator() {
			int c = start;
			@Override
			public int nextInt() {
				int re = c;
				c+=ustep;
				return re;
			}
			
			@Override
			public boolean hasNext() {
				return ustep>0?c<end:c>end;
			}
		};
	}
	
	
	public static DoubleIterator seq(double start, double end, double step) {
		if (step==0) step = end>=start?1:-1;
		double ustep = step;
		return new DoubleIterator() {
			double c = start;
			@Override
			public double nextDouble() {
				double re = c;
				c+=ustep;
				return re;
			}
			
			@Override
			public boolean hasNext() {
				return ustep>0?c<end:c>end;
			}
		};
	}
	
	
	public static <T> ExtendedIterator<T> wrap(Spliterator<T> spl) {
		return wrap(Spliterators.iterator(spl));
	}
	
	public static <T> ExtendedIterator<T> wrap(Iterable<T> itr) {
		if (itr==null) return empty();
		return wrap(itr.iterator());
	}
	
	public static <T> ExtendedIterator<T> wrap(Iterator<T> it) {
		if (it instanceof ExtendedIterator)
			return (ExtendedIterator<T>) it;
		return new ExtendedIterator<T>() {

			@Override
			public boolean hasNext() {
				return it.hasNext();
			}

			@Override
			public T next() {
				return it.next();
			}
			public void remove() {
				it.remove();
			}
		};
	}

	public static <T> ExtendedIterator<T> singleton(T a) {
		return FunctorUtils.singletonIterator(a);
	}
	
	/**
	 * Returns as long as the supplier does not supply null!
	 * @param a
	 * @return
	 */
	public static <T> ExtendedIterator<T> wrap(Supplier<T> a) {
		return new FunctorUtils.TryNextIterator<T>() {
			@Override
			protected T tryNext() {
				return a.get();
			}
			
		};
	}
	
	@SafeVarargs
	public static <T> ExtendedIterator<T> wrap(T... a) {
		if (a==null || a.length==0) return empty();
		if (a.length==1) return singleton(a[0]);
		return FunctorUtils.arrayIterator(a);
	}
	
	
	public static ExtendedIterator<Byte> wrap(byte[] a) {
		return (ExtendedIterator)NumericArray.wrap(a).iterator();
	}
	
	public static ExtendedIterator<Short> wrap(short[] a) {
		return (ExtendedIterator)NumericArray.wrap(a).iterator();
	}
	
	public static ExtendedIterator<Integer> wrap(int[] a) {
		return NumericArray.wrap(a).intIterator();
	}
	
	public static ExtendedIterator<Long> wrap(long[] a) {
		return (ExtendedIterator)NumericArray.wrap(a).iterator();
	}
	
	public static ExtendedIterator<Float> wrap(float[] a) {
		return (ExtendedIterator)NumericArray.wrap(a).iterator();
	}
	
	public static DoubleIterator wrap(double[] a) {
		return NumericArray.wrap(a).doubleIterator();
	}
	
	public static ExtendedIterator<Object> wrap(NativeArray na) {
		return wrap(na.valueIterator());
	}
	
	
	public static StringIterator wrap(Matcher regexMatcher) {
		return wrap(regexMatcher,0);
	}
	
	public static StringIterator wrap(Matcher regexMatcher, int group) {
		return new StringIterator() {
			boolean isFound = false;
			@Override
			public boolean hasNext() {
				tryFind();
				return isFound;
			}

			private void tryFind() {
				if (!isFound) isFound = regexMatcher.find();
			}

			@Override
			public String next() {
				tryFind();
				if (!isFound) throw new RuntimeException("No more element!");
				String re = group>0?regexMatcher.group(group):regexMatcher.group();
				isFound = false;
				return re;
			}
			
		};
	}
	
	public static <T> ExtendedIterator<T> wrap(Enumeration<T> a) {
		return new ExtendedIterator<T>() {

			@Override
			public boolean hasNext() {
				return a.hasMoreElements();
			}

			@Override
			public T next() {
				return a.nextElement();
			}
			
		};
	}
	
	public static <T> ExtendedIterator<T> wrap(T[] a, int start, int end) {
		return FunctorUtils.arrayIterator(a,start,end);
	}
	
	
	public static <T> ExtendedIterator<T> wrap(int size, IntFunction<T> factory) {
		return FunctorUtils.factoryIterator(size, factory);
	}
	
	public static <T>ExtendedIterator<T> repeatIndex(int n, IntFunction<T> fun) {
		return new ExtendedIterator<T>() {
			int c = 0;
			@Override
			public boolean hasNext() {
				return c<n;
			}

			@Override
			public T next() {
				return fun.apply(c++);
			}
			
		};
	}
	
	public static <T>ExtendedIterator<T> repeat(int n, T o) {
		return new ExtendedIterator<T>() {
			int c = 0;
			@Override
			public boolean hasNext() {
				return c<n;
			}

			@Override
			public T next() {
				c++;
				return o;
			}
			
		};
	}
	
	public static <T>ExtendedIterator<T> repeat(int n, Supplier<T> fac) {
		return new ExtendedIterator<T>() {
			int c = 0;
			@Override
			public boolean hasNext() {
				return c<n;
			}

			@Override
			public T next() {
				c++;
				return fac.get();
			}
			
		};
	}
	
	public static <T> ExtendedIterator<T[]> parallel(Class<T> cls, Comparator<? super T> comp, Iterator<T>... it) {
		return FunctorUtils.parallellIterator(it, comp, cls);
	}
	
	public static <T> ExtendedIterator<T> merge(Comparator<? super T> comp, Iterator<T>... it) {
		return FunctorUtils.mergeIterator(it, comp);
	}
	public static StringIterator substrings(String str, int s) {
		return FunctorUtils.substringIterator(str, s);
	}
	public static StringIterator substrings(String str, int s, boolean overlapping) {
		return FunctorUtils.substringIterator(str, s,overlapping);
	}
	public static LineIterator lines(String path, String commentPrefixes) throws IOException {
		return new LineOrientedFile(path).lineIterator(commentPrefixes);
	}
	
	public static LineIterator lines(InputStream stream) throws IOException {
		return new LineIterator(stream);
	}
	
	public static LineIterator lines(File file) throws IOException {
		return new LineOrientedFile(file.getPath()).lineIterator();
	}
	
	
	public static LineIterator lines(String path) throws IOException {
		return new LineOrientedFile(path).lineIterator();
	}
	public static LineIterator lines2(String path) {
		try {
			return new LineOrientedFile(path).lineIterator();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	public static StringIterator fileNames(String path) throws IOException {
		return EI.wrap(new File(path).list()).str();
	}
	public static ExtendedIterator<File> files(String path) throws IOException {
		return EI.wrap(new File(path).listFiles());
	}
	
	static ExtendedIterator em = new ExtendedIterator() {
		@Override
		public Object next() {
			return null;
		}
		@Override
		public boolean hasNext() {
			return false;
		}
	};
	
	public static <T> ExtendedIterator<T> empty() {
		return em;
	}


	public static <E,R> ExtendedIterator<R> combine(Collection<Iterable<E>> list, Function<E[], R> combiner, Class<E> cls) {
		E[] r = (E[]) Array.newInstance(cls, list.size());
		
		ExtendedIterator<E[]> pre = EI.singleton(r);
		int index = 0;
		for (Iterable<E> itr : list) {
			int uindex = index++;
			pre = EI.wrap(pre).unfold(a->EI.wrap(itr).map(e->{
				a[uindex] = e;
				return a;
			}));
		}
		
		return pre.map(combiner);
	}
	
	
	public static <T,R extends BinaryReader> ExtendedIterator<T> deserialize(BinarySerializer<T> serializer, R reader, Consumer2<R> endReader) throws IOException {
		serializer.beginDeserialize(reader);
		
		return new ExtendedIterator<T>() {

			@Override
			public boolean hasNext() {
				return !reader.eof();
			}

			@Override
			public T next() {
				try {
					return serializer.deserialize(reader);
				} catch (IOException e) {
					throw new RuntimeException("Could not deserialize!",e);
				}
			}
			
		}.endAction(()->{
			try {
				serializer.endDeserialize(reader);
				endReader.accept2(reader);
			} catch (Exception e) {
				throw new RuntimeException("Could not deserialize!",e);
			}
		});
		
	}
	
}
