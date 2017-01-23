package gedi.bam.tools;

import gedi.util.StringUtils;

import java.util.Comparator;
import java.util.function.BiPredicate;

import htsjdk.samtools.SAMRecord;

public class SamRecordNameLooseComparator implements BiPredicate<SAMRecord,SAMRecord> {

	@Override
	public boolean test(SAMRecord o1, SAMRecord o2) {
		if (o1.getReadName().equals(o2.getReadName()))
			return true;
		
		if (StringUtils.isInt(o1.getReadName()) && StringUtils.isInt(o2.getReadName()))
			return Integer.parseInt(o1.getReadName())==Integer.parseInt(o2.getReadName());
		
		int pref = StringUtils.getCommonPrefixLength(o1.getReadName(),o2.getReadName());
		String p1 = o1.getReadName().substring(pref);
		String p2 = o2.getReadName().substring(pref);
		
		if (p1.indexOf(' ')>0) p1 = p1.substring(0,p1.indexOf(' '));
		if (p2.indexOf(' ')>0) p2 = p2.substring(0,p2.indexOf(' '));
		
		if (StringUtils.isInt(p1) && StringUtils.isInt(p2))
			return Integer.parseInt(p1)==Integer.parseInt(p2);
		
		
		
		return o1.getReadName().equals(o2.getReadName());
	}
}