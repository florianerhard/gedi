package gedi.util.parsing;

public class IntegerParser implements Parser<Integer> {
	@Override
	public Integer apply(String s) {
		return Integer.parseInt(s);
	}

	@Override
	public Class<Integer> getParsedType() {
		return Integer.class;
	}
}