package gedi.util.parsing;

public class LongParser implements Parser<Long> {
	@Override
	public Long apply(String s) {
		return Long.parseLong(s);
	}

	@Override
	public Class<Long> getParsedType() {
		return Long.class;
	}
}