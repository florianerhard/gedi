package gedi.core.data.index;

import java.util.HashMap;
import java.util.function.Function;
import java.util.logging.Logger;

import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;

public class MemoryArrayDataIndex<D,R> extends HashMap<D,ImmutableReferenceGenomicRegion<R[]>> implements DataIndex<D,R[]> {

	private static final Logger log = Logger.getLogger( MemoryArrayDataIndex.class.getName() );
	
	
	public MemoryArrayDataIndex(GenomicRegionStorage<R[]> storage, Function<R,D> mapper) {
		storage.iterateReferenceGenomicRegions().forEachRemaining(rgr->{
			for (R d : rgr.getData())
				putWarn(mapper.apply(d),rgr);
		});
		
	}
	
	
	private void putWarn(D data, ImmutableReferenceGenomicRegion<R[]> rgr) {
		if (containsKey(data))
			log.warning("Element "+data+" already present in index!");
		put(data,rgr);
	}
}
