/**
 * 
 *    Copyright 2017 Florian Erhard
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */
package gedi.util.datastructure.array.functions;

import gedi.util.ArrayUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;

import java.util.Arrays;
import java.util.function.DoublePredicate;
import java.util.function.ToDoubleFunction;

import org.apache.commons.math3.stat.descriptive.moment.GeometricMean;
import org.apache.commons.math3.stat.descriptive.moment.Kurtosis;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.Skewness;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.apache.commons.math3.stat.descriptive.rank.Max;
import org.apache.commons.math3.stat.descriptive.rank.Min;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.apache.commons.math3.stat.descriptive.summary.Product;
import org.apache.commons.math3.stat.descriptive.summary.Sum;
import org.apache.commons.math3.stat.descriptive.summary.SumOfLogs;
import org.apache.commons.math3.stat.descriptive.summary.SumOfSquares;

@FunctionalInterface
public interface NumericArrayFunction extends ToDoubleFunction<NumericArray> {

	
	public static final NumericArrayFunction GeometricMean = new StorelessUnivariateStatisticAdapter(new GeometricMean());
	public static final NumericArrayFunction Kurtosis = new StorelessUnivariateStatisticAdapter(new Kurtosis());
	public static final NumericArrayFunction Max = new StorelessUnivariateStatisticAdapter(new Max());
	public static final NumericArrayFunction Mean = new StorelessUnivariateStatisticAdapter(new Mean());
	public static final NumericArrayFunction Min = new StorelessUnivariateStatisticAdapter(new Min());
	public static final NumericArrayFunction Product = new StorelessUnivariateStatisticAdapter(new Product());
	public static final NumericArrayFunction Skewness = new StorelessUnivariateStatisticAdapter(new Skewness());
	public static final NumericArrayFunction StandardDeviation = new StorelessUnivariateStatisticAdapter(new StandardDeviation());
	public static final NumericArrayFunction Sum = new StorelessUnivariateStatisticAdapter(new Sum());
	public static final NumericArrayFunction SumOfLogs = new StorelessUnivariateStatisticAdapter(new SumOfLogs());
	public static final NumericArrayFunction SumOfSquares = new StorelessUnivariateStatisticAdapter(new SumOfSquares());
	public static final NumericArrayFunction Variance = new StorelessUnivariateStatisticAdapter(new Variance());
	
	public static final NumericArrayFunction LowerQuartile = new UnivariateStatisticAdapter(new Percentile(25));
	public static final NumericArrayFunction Median = new UnivariateStatisticAdapter(new Percentile(50));
	public static final NumericArrayFunction UpperQuartile = new UnivariateStatisticAdapter(new Percentile(75));
	
	public static final NumericArrayFunction SinhMean = n-> {
		double re = 0;
		for (int i=0; i<n.length(); i++) {
			double c = n.getDouble(i);
			double asinh = Math.log(c+Math.sqrt(c*c+1));
			re+=asinh;
		}
		return Math.sinh(re/n.length());
	};
	
	public static final NumericArrayFunction SaveMax = n-> {
		double re = Double.NEGATIVE_INFINITY;
		for (int i=0; i<n.length(); i++) {
			double c = n.getDouble(i);
			if (!Double.isInfinite(c) && !Double.isNaN(c))
				re = Math.max(re,c);
		}
		return Double.isInfinite(re)?Double.NaN:re;
	};

	public static final NumericArrayFunction SaveSum = n->{
		double re = 0;
		for (int i=0; i<n.length(); i++) {
			double c = n.getDouble(i);
			if (!Double.isInfinite(c) && !Double.isNaN(c))
				re = re+c;
		}
		return re;
	};
	
	public static final NumericArrayFunction GeometricMeanRemoveNonPositive = n->{
		DoubleArrayList r = new DoubleArrayList();
		for (int i=0; i<n.length(); i++) {
			double c = n.getDouble(i);
			if (c>0)
				r.add(c);
		}
		if (r.isEmpty()) return 0;
		return new GeometricMean().evaluate(r.toDoubleArray());
	};
	
	
	public static final NumericArrayFunction SaveMin = n-> {
		double re = Double.POSITIVE_INFINITY;
		for (int i=0; i<n.length(); i++) {
			double c = n.getDouble(i);
			if (!Double.isInfinite(c) && !Double.isNaN(c))
				re = Math.min(re,c);
		}
		return Double.isInfinite(re)?Double.NaN:re;
	};

	public static final NumericArrayFunction Mad = n-> {
		double med = Median.applyAsDouble(n);
		NumericArray a = NumericArray.createMemory(n.length(), NumericArrayType.Double);
		for (int i=0; i<n.length(); i++) {
			double c = n.getDouble(i);
			a.setDouble(i, Math.abs(c-med));
		}
		return Median.applyAsDouble(a);
	};
	
	public static final NumericArrayFunction Count = n-> {
		return n.length();
	};
	
	
	public static final NumericArrayFunction Entropy = n-> {
		double re = 0;
		double s = Sum.applyAsDouble(n);
		for (int i=0; i<n.length(); i++) {
			double v = n.getDouble(i);
			re+=v==0?0:(v/s*Math.log(v/s)/Math.log(2));
		}
		return -re;
	};

	public static NumericArrayFunction fraction(final DoublePredicate predicate) {
		return new NumericArrayFunction() {
			
			@Override
			public double applyAsDouble(NumericArray value) {
				int re = 0;
				for (int i=0; i<value.length(); i++)
					if (predicate.test(value.getDouble(i)))
						re++;
				return (double)re/value.length();
			}
		};
	}
	
	public static NumericArrayFunction count(final DoublePredicate predicate) {
		return new NumericArrayFunction() {
			
			@Override
			public double applyAsDouble(NumericArray value) {
				int re = 0;
				for (int i=0; i<value.length(); i++)
					if (predicate.test(value.getDouble(i)))
						re++;
				return re;
			}
		};
	}

	public static NumericArrayFunction quantile(double q) {
		return new UnivariateStatisticAdapter(new Percentile(q*100));
	}
	
	public static NumericArrayFunction orderStatistics(int rank) {
		return new NumericArrayFunction() {

			@Override
			public double applyAsDouble(NumericArray value) {
				return ArrayUtils.orderStatistic(value.toDoubleArray(),rank);
			}
			
		};
	}
	
	public static NumericArrayFunction trimmedMean(double trimQuantile) {
		return new NumericArrayFunction() {
			@Override
			public double applyAsDouble(NumericArray value) {
				double[] a = value.toDoubleArray();
				Arrays.sort(a);
				int trim = (int) (a.length*trimQuantile);
				return ArrayUtils.sum(a, trim, a.length-trim)/(a.length-trim-trim);
			}
			
		};
	}
	
	
	
}
