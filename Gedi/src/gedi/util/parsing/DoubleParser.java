package gedi.util.parsing;

public class DoubleParser implements Parser<Double> {
	@Override
	public Double apply(String s) {
		return Double.parseDouble(s);
	}

	@Override
	public Class<Double> getParsedType() {
		return Double.class;
	}
}