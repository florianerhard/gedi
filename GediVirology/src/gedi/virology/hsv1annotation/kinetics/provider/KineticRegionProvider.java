package gedi.virology.hsv1annotation.kinetics.provider;

import gedi.core.data.annotation.NameAnnotation;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.functions.ExtendedIterator;


public interface KineticRegionProvider {

	
	String getType();	
	ExtendedIterator<ImmutableReferenceGenomicRegion<NameAnnotation>> ei(ReferenceGenomicRegion<?> region);

	default ImmutableReferenceGenomicRegion<NameAnnotation>[] arr(ReferenceGenomicRegion<?> region) {
		return ei(region).toArray(new ImmutableReferenceGenomicRegion[0]);
	}
}
