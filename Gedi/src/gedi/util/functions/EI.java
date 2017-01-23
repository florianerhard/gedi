package gedi.util.functions;

import gedi.util.FunctorUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.collections.doublecollections.DoubleIterator;
import gedi.util.datastructure.collections.intcollections.IntIterator;
import gedi.util.io.text.LineIterator;
import gedi.util.io.text.LineOrientedFile;

import java.io.IOException;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.regex.Matcher;


public interface EI {
	
	
	public static ExtendedIterator<String> split(String s, char separator) {
		if (s.length()==0)
			return empty();
		
		return new ExtendedIterator<String>() {
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
		return new IntIterator() {
			int c = start;
			@Override
			public int nextInt() {
				int re = c;
				c+=step;
				return re;
			}
			
			@Override
			public boolean hasNext() {
				return step>0?c<end:c>end;
			}
		};
	}
	
	
	public static <T> ExtendedIterator<T> wrap(Spliterator<T> spl) {
		return wrap(Spliterators.iterator(spl));
	}
	
	public static <T> ExtendedIterator<T> wrap(Iterable<T> itr) {
		return wrap(itr.iterator());
	}
	
	public static <T> ExtendedIterator<T> wrap(Iterator<T> it) {
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
	
	@SafeVarargs
	public static <T> ExtendedIterator<T> wrap(T... a) {
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
	
	
	public static ExtendedIterator<String> wrap(Matcher regexMatcher) {
		return wrap(regexMatcher,0);
	}
	
	public static ExtendedIterator<String> wrap(Matcher regexMatcher, int group) {
		return new ExtendedIterator<String>() {
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
	
	public static <T> ExtendedIterator<T> merge(Comparator<? super T> comp, Iterator<T>... it) {
		return FunctorUtils.mergeIterator(it, comp);
	}
	public static ExtendedIterator<String> substrings(String str, int s) {
		return FunctorUtils.substringIterator(str, s);
	}
	public static LineIterator lines(String path, String commentPrefixes) throws IOException {
		return new LineOrientedFile().lineIterator(commentPrefixes);
	}
	
	public static LineIterator lines(String path) throws IOException {
		return new LineOrientedFile(path).lineIterator();
	}
	public static <T> ExtendedIterator<T> empty() {
		return new ExtendedIterator<T>() {
			@Override
			public T next() {
				return null;
			}
			@Override
			public boolean hasNext() {
				return false;
			}
		};
	}
	
}
