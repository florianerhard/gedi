package gedi.bam.tools;

import gedi.util.StringUtils;

import java.util.Comparator;

import htsjdk.samtools.SAMRecord;

public class SamRecordLongIdNameComparator implements Comparator<SAMRecord> {

	@Override
	public int compare(SAMRecord o1, SAMRecord o2) {
		
		String n1 = o1.getReadName();
		String n2 = o2.getReadName();
		
		if (!StringUtils.isInt(n1) || !StringUtils.isInt(n2))
			throw new RuntimeException("Ids must be integers!");
		
		return Long.compare(Long.parseLong(n1),Long.parseLong(n2));
		
	}
}