package gedi.core.processing;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.region.MutableReferenceGenomicRegion;

public interface GenomicRegionProcessor {

	default void begin(ProcessorContext context) throws Exception {}
	default void beginRegion(MutableReferenceGenomicRegion<?> region, ProcessorContext context) throws Exception {}
	default void read(MutableReferenceGenomicRegion<?> region, MutableReferenceGenomicRegion<AlignedReadsData> read, ProcessorContext context) throws Exception {}
	default void value(MutableReferenceGenomicRegion<?> region, int position,double[] values, ProcessorContext context) throws Exception {}
	default void endRegion(MutableReferenceGenomicRegion<?> region, ProcessorContext context) throws Exception {}
	default void end(ProcessorContext context) throws Exception {}

}
