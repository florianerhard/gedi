package gedi.ensembl;

import gedi.core.reference.ReferenceSequence;
import gedi.util.StringUtils;

public class Ensembl {

	
	public static boolean isStandardChromosome(ReferenceSequence ref) {
		String s = ref.getName();
		if (s.startsWith("chr")) s=s.substring(3);
		else return false;
		return s.equals("M") || s.equals("X") || s.equals("Y") || StringUtils.isInt(s);
	}

	public static String correctChr(String s) {
		if (s.equals("MT") || s.equals("M")) return "chrM";
		if (s.equals("X") || s.equals("Y")) return "chr"+s;
		if(StringUtils.isInt(s)) return "chr"+s;
		return s;
	}
}
