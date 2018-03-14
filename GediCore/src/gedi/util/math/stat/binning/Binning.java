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
