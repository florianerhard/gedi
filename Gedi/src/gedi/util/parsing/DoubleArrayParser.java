package gedi.util.parsing;

import gedi.util.StringUtils;

public class DoubleArrayParser implements Parser<double[]> {
	@Override
	public double[] apply(String s) {
		return StringUtils.parseDouble(StringUtils.split(s, ','));
	}

	@Override
	public Class<double[]> getParsedType() {
		return double[].class;
	}
}