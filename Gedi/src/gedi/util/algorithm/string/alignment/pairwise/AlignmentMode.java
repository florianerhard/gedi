package gedi.util.algorithm.string.alignment.pairwise;

public enum AlignmentMode {
	Global,Local,Freeshift,PrefixSuffix,BothPrefix;
	
	public static AlignmentMode fromString(String s) {
		if (s.toLowerCase().startsWith("g"))
			return Global;
		else if (s.toLowerCase().startsWith("l"))
			return Local;
		else if (s.toLowerCase().startsWith("f"))
			return Freeshift;
		else if (s.toLowerCase().startsWith("p"))
			return PrefixSuffix;
		else if (s.toLowerCase().startsWith("b"))
			return BothPrefix;
		else
			return null;
	}
}
