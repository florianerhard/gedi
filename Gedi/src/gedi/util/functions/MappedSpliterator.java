package gedi.util.functions;

import java.util.Comparator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The supplier ctor can be used if this spliterator is supposed to use a single mutable object for its iteration, i.e. the map function will
 * always return the same object that is changed accordingly after every call to apply.
 * @author erhard
 *
 * @param <S>
 * @param <T>
 */
public class MappedSpliterator<S,T> implements Spliterator<T> {

	private Spliterator<S> ssplit;
	private Supplier<Function<S, T>> mapSupplier;
	private Function<S, T> mapper;
	
	public MappedSpliterator(Spliterator<S> ssplit, Supplier<Function<S,T>> mapSupplier) {
		this.ssplit = ssplit;
		this.mapSupplier = mapSupplier;
		this.mapper = mapSupplier.get();
	}

	
	public MappedSpliterator(Spliterator<S> ssplit, Function<S,T> mapper) {
		this.ssplit = ssplit;
		this.mapper = mapper;
	}

	@Override
	public boolean tryAdvance(Consumer<? super T> action) {
		return ssplit.tryAdvance(o->action.accept(mapper.apply(o)));
	}

	@Override
	public Spliterator<T> trySplit() {
		Spliterator<S> s = ssplit.trySplit();
		if (s==null) return null;
		
		if (mapSupplier!=null)
			return new MappedSpliterator<S,T>(s,mapSupplier);
		
		return new MappedSpliterator<S,T>(s,mapper);
	}

	@Override
	public long estimateSize() {
		return ssplit.estimateSize();
	}

	@Override
	public int characteristics() {
		return ssplit.characteristics() & ~SORTED;
	}
	
}
