package gedi.bam.tools;

import gedi.util.FunctorUtils;
import gedi.util.io.text.LineOrientedFile;

import java.util.Iterator;
import java.util.function.UnaryOperator;

import htsjdk.samtools.SAMRecord;

public class SamRemoveUnmapped implements UnaryOperator<Iterator<SAMRecord>>, AutoCloseable {

	private LineOrientedFile out;
	
	public SamRemoveUnmapped() {
	}

	public SamRemoveUnmapped(LineOrientedFile out) {
		this.out = out;
	}

	public SamRemoveUnmapped(String path) {
		this.out = new LineOrientedFile(path);
	}

	public Iterator<SAMRecord> apply(Iterator<SAMRecord> it) {
		return FunctorUtils.filteredIterator(it,r->{
			if (!r.getReadUnmappedFlag())
				return true;
			
			if (out!=null) {
				try {
					if (!out.isWriting()) out.startWriting();
					out.writef(">%s\n%s\n",r.getReadName(),r.getReadString());
				} catch (Exception e) {
					throw new RuntimeException("Could not write unmapped reads!",e);
				}
				
			}
			return !r.getReadUnmappedFlag();
		});
	}

	@Override
	public void close() throws Exception {
		if (out!=null && out.isWriting())
			out.finishWriting();
	}
	
	
}