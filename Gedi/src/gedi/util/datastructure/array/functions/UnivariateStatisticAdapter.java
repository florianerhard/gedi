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
