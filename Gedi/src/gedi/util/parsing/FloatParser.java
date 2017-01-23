package gedi.util.parsing;

public class FloatParser implements Parser<Float> {
	@Override
	public Float apply(String s) {
		return Float.parseFloat(s);
	}

	@Override
	public Class<Float> getParsedType() {
		return Float.class;
	}
}