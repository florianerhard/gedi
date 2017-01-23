package gedi.util.parsing;

public class StringParser implements Parser<String> {
	@Override
	public String apply(String s) {
		return s;
	}

	@Override
	public Class<String> getParsedType() {
		return String.class;
	}
}