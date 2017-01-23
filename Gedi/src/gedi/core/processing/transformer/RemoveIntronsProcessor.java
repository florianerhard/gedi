package gedi.core.processing.transformer;

import gedi.core.processing.GenomicRegionProcessor;
import gedi.core.processing.ProcessorContext;
import gedi.core.region.MutableReferenceGenomicRegion;

public class RemoveIntronsProcessor implements GenomicRegionProcessor {


	
	@Override
	public void beginRegion(MutableReferenceGenomicRegion<?> region, ProcessorContext context)
			throws Exception {
		region.setRegion(region.getRegion().removeIntrons());
	}




}
