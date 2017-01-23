package gedi.util.parsing;

public class QuotedStringParser implements Parser<String> {
	
	public boolean canParse(String s) {
		return s.startsWith("\"") && s.endsWith("\"");
	}
	
	@Override
	public String apply(String s) {
		return canParse(s)?s.substring(1, s.length()-1):s;
	}

	@Override
	public Class<String> getParsedType() {
		return String.class;
	}
}