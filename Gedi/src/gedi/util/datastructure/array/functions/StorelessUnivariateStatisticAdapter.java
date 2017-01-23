package gedi.util.datastructure.array.functions;

import gedi.util.datastructure.array.NumericArray;

import org.apache.commons.math3.stat.descriptive.StorelessUnivariateStatistic;

public class StorelessUnivariateStatisticAdapter implements NumericArrayFunction {

	private StorelessUnivariateStatistic commons;
	
	
	public StorelessUnivariateStatisticAdapter(StorelessUnivariateStatistic commons) {
		this.commons = commons.copy();
		commons.clear();
	}

	@Override
	public double applyAsDouble(NumericArray value) {
		StorelessUnivariateStatistic local = commons.copy();
		for (int i=0; i<value.length(); i++)
			local.increment(value.getDouble(i));
		return local.getResult();
	}
	
}
