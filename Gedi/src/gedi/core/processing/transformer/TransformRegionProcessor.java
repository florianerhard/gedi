package gedi.core.processing.transformer;

import gedi.core.processing.GenomicRegionProcessor;
import gedi.core.processing.ProcessorContext;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegionPosition;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.sequence.SequenceProvider;
import gedi.util.io.text.LineOrientedFile;


/**
 * Keeps introns!
 * @author erhard
 *
 */
public class TransformRegionProcessor implements GenomicRegionProcessor {

	private GenomicRegionPosition startPosition;
	private int startOffset;
	
	private GenomicRegionPosition endPosition;
	private int endOffset;

	public TransformRegionProcessor(GenomicRegionPosition startPosition,
			int startOffset, GenomicRegionPosition endPosition, int endOffset) {
		this.startPosition = startPosition;
		this.startOffset = startOffset;
		this.endPosition = endPosition;
		this.endOffset = endOffset;
	}
	
	public TransformRegionProcessor(GenomicRegionPosition startPosition,
			GenomicRegionPosition endPosition) {
		this.startPosition = startPosition;
		this.endPosition = endPosition;
	}


	
	@Override
	public void beginRegion(MutableReferenceGenomicRegion<?> region, ProcessorContext context)
			throws Exception {
		int start = startPosition.position(region.getReference(), region.getRegion(), startOffset);
		int end = endPosition.position(region.getReference(), region.getRegion(), endOffset);
		
		if (start>end) {
			int t = start;
			start = end;
			end = t;
		}
		
		ArrayGenomicRegion re = new ArrayGenomicRegion(start,end);
		re = re.intersect(region.getRegion()).union(re.subtract(region.getRegion().removeIntrons()));
		
		region.setRegion(re);
	}




}
