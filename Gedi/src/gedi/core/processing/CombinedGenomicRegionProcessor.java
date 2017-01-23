package gedi.core.processing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.region.MutableReferenceGenomicRegion;

public class CombinedGenomicRegionProcessor extends ArrayList<GenomicRegionProcessor> implements GenomicRegionProcessor {

	public CombinedGenomicRegionProcessor() {
	}

	
	public CombinedGenomicRegionProcessor(Collection<? extends GenomicRegionProcessor> fill) {
		super(fill);
	}

	
	public CombinedGenomicRegionProcessor(GenomicRegionProcessor... fill) {
		super(Arrays.asList(fill));
	}

	
	@Override
	public void begin(ProcessorContext context) throws Exception {
		for (GenomicRegionProcessor p : this)
			p.begin(context);
	}

	@Override
	public void beginRegion(MutableReferenceGenomicRegion<?> region,
			ProcessorContext context) throws Exception {
		for (GenomicRegionProcessor p : this)
			p.beginRegion(region, context);
	}
	
	@Override
	public void read(MutableReferenceGenomicRegion<?> region,
			MutableReferenceGenomicRegion<AlignedReadsData> read,
			ProcessorContext context) throws Exception {
		for (GenomicRegionProcessor p : this)
			p.read(region, read, context);
	}
	
	@Override
	public void value(MutableReferenceGenomicRegion<?> region, int position,
			double[] values, ProcessorContext context) throws Exception {
		for (GenomicRegionProcessor p : this)
			p.value(region, position, values, context);
	}
	
	
	@Override
	public void endRegion(MutableReferenceGenomicRegion<?> region,
			ProcessorContext context) throws Exception {
		for (GenomicRegionProcessor p : this)
			p.endRegion(region, context);
	}


	@Override
	public void end(ProcessorContext context) throws Exception {
		for (GenomicRegionProcessor p : this)
			p.end(context);
	}

	
	

}
