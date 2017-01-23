package gedi.core.processing;

import gedi.core.reference.ReferenceSequence;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.sequence.SequenceProvider;
import gedi.util.io.text.LineOrientedFile;

public class SequenceExtractingProcessor implements GenomicRegionProcessor {

	// TODO: once table framework is there, do not produce an output file but write into a table of the context! (a table output processor will then do the trick)

	private SequenceProvider sequence;
	private LineOrientedFile out;
	private boolean strandspecific;
	
	
	public SequenceExtractingProcessor(LineOrientedFile out, SequenceProvider sequence, boolean strandspecific) {
		this.sequence = sequence;
		this.out = out;
		this.strandspecific = strandspecific;
	}


	@Override
	public void begin(ProcessorContext context) throws Exception {
		out.startWriting();
	}
	
	
	@Override
	public void beginRegion(MutableReferenceGenomicRegion<?> region, ProcessorContext context)
			throws Exception {
		CharSequence seq = strandspecific?sequence.getSequence(region.getReference(), region.getRegion()):sequence.getPlusSequence(region.getReference().getName(), region.getRegion());
		ReferenceSequence ref = region.getReference();
		if (!strandspecific)
			ref = ref.toStrandIndependent();
		out.writef(">%s\n%s\n", ref+":"+region.getRegion(),seq);
	}

	@Override
	public void end(ProcessorContext context) throws Exception {
		out.finishWriting();
	}




}
