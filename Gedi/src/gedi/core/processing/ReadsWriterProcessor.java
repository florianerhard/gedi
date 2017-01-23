package gedi.core.processing;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.io.text.LineOrientedFile;

public class ReadsWriterProcessor implements GenomicRegionProcessor {

	
	
	private LineOrientedFile out;

	private ReadCountMode mode;
	
	
	public ReadsWriterProcessor(String path) {
		this.out = new LineOrientedFile(path);
	}

	
	public void setMode(ReadCountMode mode) {
		this.mode = mode;
	}

	@Override
	public void begin(ProcessorContext context) throws Exception {
		out.startWriting();
	}
	@Override
	public void beginRegion(MutableReferenceGenomicRegion<?> region,
			ProcessorContext context) throws Exception {
		if (!out.getPath().endsWith("sv"))
			out.writef(">%s\n",region.toString());
	}
	
	@Override
	public void read(MutableReferenceGenomicRegion<?> region,
			MutableReferenceGenomicRegion<AlignedReadsData> read,
			ProcessorContext context) throws Exception {
		if (!out.getPath().endsWith("sv"))
			out.writef("%s\n",read);
		else {
			out.writef("%s:%s",read.getReference(),read.getRegion());
			NumericArray counts = read.getData().getTotalCountsForConditions(null, mode);
			for (int i=0; i<read.getData().getNumConditions(); i++)
				out.writef("\t%s",counts.format(i));
			out.writeLine();
		}
	}
	
	
	@Override
	public void endRegion(MutableReferenceGenomicRegion<?> region,
			ProcessorContext context) throws Exception {
	}

	@Override
	public void end(ProcessorContext context) throws Exception {
		out.finishWriting();
	}


	




}
