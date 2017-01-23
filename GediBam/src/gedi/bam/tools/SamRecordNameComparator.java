package gedi.bam.tools;

import gedi.util.StringUtils;

import java.util.Comparator;

import htsjdk.samtools.SAMRecord;

public class SamRecordNameComparator implements Comparator<SAMRecord> {

	@Override
	public int compare(SAMRecord o1, SAMRecord o2) {
		
		String n1 = o1.getReadName();
		String n2 = o2.getReadName();
		
		if ((n1.endsWith("/1") || n1.endsWith("/2")) && (n2.endsWith("/1") || n2.endsWith("/2"))) {
			n1 = n1.substring(0, n1.length()-2);
			n2 = n2.substring(0, n2.length()-2);
		}
			
		
		if (n1.equals(n2))
			return 0;
		
		if (StringUtils.isInt(n1) && StringUtils.isInt(n2))
			return Integer.compare(Integer.parseInt(n1),Integer.parseInt(n2));
		
		// for files from sra
		int pref = StringUtils.getCommonPrefixLength(n1,n2);
		if (pref>0 && !n1.contains(":")) {
			String p1 = n1.substring(pref);
			String p2 = n2.substring(pref);
			
			if (p1.indexOf(' ')>0) p1 = p1.substring(0,p1.indexOf(' '));
			if (p2.indexOf(' ')>0) p2 = p2.substring(0,p2.indexOf(' '));
			
			if (p1.length()==0) p1 = "0";
			if (p2.length()==0) p2 = "0";
			
			if (StringUtils.isInt(p1) && StringUtils.isInt(p2))
				return Integer.compare(Integer.parseInt(p1),Integer.parseInt(p2));
		}
		
		
		return n1.compareTo(n2);
	}
}