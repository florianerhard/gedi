package gedi.util.parsing;

import java.util.Arrays;
import java.util.HashSet;

public class BooleanParser implements Parser<Boolean> {
	
	private HashSet<String> trues = new HashSet<String>(Arrays.asList("true","True","TRUE","T","x","y","yes"));
	private HashSet<String> falses = new HashSet<String>(Arrays.asList("false","False","FALSE","F",""," ","n","no"));
	
	@Override
	public Boolean apply(String s) {
		if (trues.contains(s)) return true;
		if (falses.contains(s)) return false;
		throw new IllegalArgumentException();
	}

	@Override
	public Class<Boolean> getParsedType() {
		return Boolean.class;
	}
}