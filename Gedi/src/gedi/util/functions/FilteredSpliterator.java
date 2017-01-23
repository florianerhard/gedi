package gedi.util.functions;

import gedi.util.mutable.MutableMonad;

import java.util.Comparator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class FilteredSpliterator<T> implements Spliterator<T> {

	private Spliterator<T> ssplit;
	private Predicate<T> filter;
	private MutableMonad<T> box = new MutableMonad<T>();
	
	
	public FilteredSpliterator(Spliterator<T> ssplit, Predicate<T> filter) {
		this.ssplit = ssplit;
		this.filter = filter;
	}

	@Override
	public boolean tryAdvance(Consumer<? super T> action) {
		while (ssplit.tryAdvance(o->{if (filter.test(o)) box.Item=o;}) && box.Item==null);
		if (box.Item==null) return false;
		action.accept(box.Item);
		box.Item=null;
		return true;
	}

	@Override
	public Spliterator<T> trySplit() {
		Spliterator<T> s = ssplit.trySplit();
		if (s==null) return null;
		return new FilteredSpliterator<T>(s,filter);
	}

	@Override
	public long estimateSize() {
		return ssplit.estimateSize();
	}

	@Override
	public int characteristics() {
		return ssplit.characteristics() & ~SIZED;
	}
	
	public Comparator<? super T> getComparator() {
        return ssplit.getComparator();
    }
	
}
