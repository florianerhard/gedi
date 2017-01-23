package gedi.core.processing.transformer;

import gedi.core.processing.GenomicRegionProcessor;
import gedi.core.processing.ProcessorContext;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegionPosition;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.sequence.SequenceProvider;
import gedi.util.io.text.LineOrientedFile;

public class TransformToPositionProcessor implements GenomicRegionProcessor {

	private GenomicRegionPosition position;
	private int offset;
	

	public TransformToPositionProcessor(GenomicRegionPosition position, int offset) {
		this.position = position;
		this.offset = offset;
	}

	
	public TransformToPositionProcessor(GenomicRegionPosition position) {
		this.position = position;
		this.offset = 0;
	}


	
	
	@Override
	public void beginRegion(MutableReferenceGenomicRegion<?> region, ProcessorContext context)
			throws Exception {
		int pos = position.position(region.getReference(), region.getRegion(), offset);
		region.setRegion(new ArrayGenomicRegion(pos, pos+1));
	}




}
