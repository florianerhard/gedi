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

package gedi.plot;

import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Function;

import gedi.plot.scale.transformer.DoubleInvertibleTransformer;
import gedi.util.datastructure.dataframe.DataColumn;
import jdistlib.disttest.NormalityTest;

public class GPlotUtils {

	public static final int CONTINUOUS_THRESHOLD = 10;
	
	/** 
	 * Chose either linear or log10: which one produces a more normal distribution 
	 * @param c
	 * @return
	 */
	public static DoubleInvertibleTransformer inferScaling(DataColumn<?> c) {
		double[] x = new double[c.size()];
		for (int i=0; i<x.length; i++) {
			x[i] = c.getDoubleValue(i);
			if (x[i]<=0) return DoubleInvertibleTransformer.identity;
		}
		double lin = sumdiff(x);
		for (int i=0; i<x.length; i++)
			x[i] = Math.log(x[i]);
		double log = sumdiff(x);
		return lin<log?DoubleInvertibleTransformer.identity:DoubleInvertibleTransformer.log10;
	}

	public static boolean isContinuousColumn(DataColumn<?> c) {
		if (!c.isDouble()) return false;
		HashSet<Double> vals = new HashSet<>();
		for (int i=0; i<c.size(); i++) {
			vals.add(c.getDoubleValue(i));
			if (vals.size()>CONTINUOUS_THRESHOLD) 
				return true;
		}
		return false;
	}
	
	private static double sumdiff(double[] x) {
		Arrays.sort(x);
		double sum = 0;
		for (int i=0; i<x.length; i++)
			sum += Math.abs((x[i]-x[0])/(x[x.length-1]-x[0])-(i+0.5)/x.length);
		return sum;
	}

}
