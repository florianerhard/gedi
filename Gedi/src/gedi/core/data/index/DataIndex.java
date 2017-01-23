package gedi.core.data.index;

import java.util.function.Function;

import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;

public interface DataIndex<D,R> extends Function<D,ReferenceGenomicRegion<R>>{
	
	ReferenceGenomicRegion<R> get(D data);
	
	default ReferenceGenomicRegion<R> apply(D data) {
		return get(data);
	}
	
}
