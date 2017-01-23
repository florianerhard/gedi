package gedi.riboseq.inference.codon;

import gedi.util.StringUtils;

public enum CodonType {
	Start, Stop, NoncanonicalStart, Any;

	public static CodonType get(String codonTriplett) {
		if (codonTriplett==null || codonTriplett.length()!=3) return null;
		if (codonTriplett.equals("ATG"))
			return Start;
		if (codonTriplett.equals("TAA") || codonTriplett.equals("TGA") || codonTriplett.equals("TAG"))
			return Stop;
		if (StringUtils.hamming("ATG", codonTriplett)==1)
			return NoncanonicalStart;
		return Any;
	}
}
