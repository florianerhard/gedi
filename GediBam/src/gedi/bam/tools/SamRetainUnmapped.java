package gedi.bam.tools;

import gedi.util.FunctorUtils;
import gedi.util.io.text.LineOrientedFile;

import java.util.Iterator;
import java.util.function.UnaryOperator;

import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;

public class SamRetainUnmapped implements UnaryOperator<Iterator<SAMRecord>> {

	private int pos = 1;
	
	private SAMFileHeader header;
	
	
	public SamRetainUnmapped(SAMFileHeader header) {
		this.header = header;
	}




	public Iterator<SAMRecord> apply(Iterator<SAMRecord> it) {
		return FunctorUtils.sideEffectIterator(it,r->{
			if (r.getReadUnmappedFlag()) {
				r.setHeader(header);
				r.setReadUnmappedFlag(false);
				r.setReferenceName("Unmapped");
				r.setAlignmentStart(pos++);
				r.setMappingQuality(255);
				r.setCigarString(r.getReadLength()+"M");
				r.setAttribute("XA", 0);
				r.setAttribute("MD", r.getReadLength()+"");
				r.setAttribute("XA", 0);
			}
				
		});
	}

	
	
}