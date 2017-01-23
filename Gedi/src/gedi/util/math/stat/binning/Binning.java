package gedi.util.math.stat.binning;

import gedi.util.math.stat.factor.Factor;

import java.util.function.DoubleFunction;
import java.util.function.DoubleToIntFunction;
import java.util.function.Function;


/**
 * Start inclusive, end exclusive
 * @author erhard
 *
 */
public interface Binning extends DoubleToIntFunction, DoubleFunction<Factor> {

	double getBinMin(int bin);
	double getBinMax(int bin);
	
	default double getBinCenter(int bin) {
		return (getBinMin(bin)+getBinMax(bin))/2;
	}
	
	default double getMin() {
		return getBinMin(0);
	}

	default double getMax() {
		return getBinMax(getBins()-1);
	}
	
	default Factor apply(double d) {
		return getFactor(applyAsInt(d));
	}

	Factor getFactor(int index);
	boolean isInBounds(double value);
	int getBins();
	boolean isInteger();
	
}
