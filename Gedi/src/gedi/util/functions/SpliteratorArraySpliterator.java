package gedi.util.functions;

import java.util.Comparator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SpliteratorArraySpliterator<D> implements Spliterator<D> {

	private Spliterator<D>[] a;
	private Supplier<Spliterator<D>>[] s;
	private int start;
	private int end;
	
	public SpliteratorArraySpliterator(Spliterator<D>[] a) {
		this(a,0,a.length);
	}
	
	public SpliteratorArraySpliterator(Spliterator<D>[] a, int start, int end) {
		this.a = a;
		this.start = start;
		this.end = end;
	}
	
	public SpliteratorArraySpliterator(Supplier<Spliterator<D>>[] a) {
		this(a,0,a.length);
	}
	
	public SpliteratorArraySpliterator(Supplier<Spliterator<D>>[] a, int start, int end) {
		this.s = a;
		this.a = new Spliterator[s.length];
		this.start = start;
		this.end = end;
	}
	
	public SpliteratorArraySpliterator(Supplier<Spliterator<D>>[] s, Spliterator<D>[] a, int start, int end) {
		this.a = a;
		this.s = s;
		this.start = start;
		this.end = end;
	}

	@Override
	public boolean tryAdvance(Consumer<? super D> action) {
		while (start<end && !getSpliterator(start).tryAdvance(action))
			start++;
		return start<end;
	}

	@Override
	public Spliterator<D> trySplit() {
		if (start+1==end) return null;
		int p = (start+end)/2;
		end = p;
		return new SpliteratorArraySpliterator<D>(s,a, p, end);
	}

	@Override
	public long estimateSize() {
		long re = 0;
		for (int i=start;i<end; i++)
			re+=getSpliterator(i).estimateSize();
		return re;
	}

	@Override
	public int characteristics() {
		if (a.length==0) return 0;
		return getSpliterator(0).characteristics();
	}
	
	public Comparator<? super D> getComparator() {
        return getSpliterator(0).getComparator();
    }
	
	private Spliterator<D> getSpliterator(int index) {
		if (a[index]==null) a[index] = s[index].get();
		return a[index];
	}
	
	
}

