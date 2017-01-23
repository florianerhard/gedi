package gedi.util.datastructure.array.functions;

import gedi.util.datastructure.array.NumericArray;

import org.apache.commons.math3.stat.descriptive.UnivariateStatistic;

public class UnivariateStatisticAdapter implements NumericArrayFunction {

	private UnivariateStatistic commons;
	
	
	public UnivariateStatisticAdapter(UnivariateStatistic commons) {
		this.commons = commons;
	}

	@Override
	public double applyAsDouble(NumericArray value) {
		return commons.evaluate(value.toDoubleArray());
	}
	
}
