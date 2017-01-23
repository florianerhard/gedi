package gedi.virology.hsv1annotation.kinetics.provider;

import gedi.core.data.annotation.NameAnnotation;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.functions.ExtendedIterator;

public class FromStorageProvider implements KineticRegionProvider {

	private String type;
	private MemoryIntervalTreeStorage<NameAnnotation> storage;
	
	
	public FromStorageProvider(String type, MemoryIntervalTreeStorage<NameAnnotation> storage) {
		this.type = type;
		this.storage = storage;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public ExtendedIterator<ImmutableReferenceGenomicRegion<NameAnnotation>> ei(
			ReferenceGenomicRegion<?> region) {
		return storage.ei(region).filter(r->region.contains(r));
	}

}
