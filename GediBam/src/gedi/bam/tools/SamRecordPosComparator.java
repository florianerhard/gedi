package gedi.bam.tools;

import gedi.util.GeneralUtils;
import gedi.util.StringUtils;

import java.util.Comparator;

import htsjdk.samtools.SAMRecord;

public class SamRecordPosComparator implements Comparator<SAMRecord> {

	
	private boolean strandspecific = true;
	
	
	public SamRecordPosComparator() {
	}
	
	public SamRecordPosComparator(boolean strandspecific) {
		this.strandspecific = strandspecific;
	}
	
	
	@Override
	public int compare(SAMRecord o1, SAMRecord o2) {
		int re = o1.getReferenceName().compareTo(o2.getReferenceName());
		if (re==0)
			re = Integer.compare(o1.getAlignmentStart(),o2.getAlignmentStart());
		if (re==0 && strandspecific)
			re = -Integer.compare(o1.getReadNegativeStrandFlag()?-1:1,o2.getReadNegativeStrandFlag()?-1:1);
		return re;
	}
}