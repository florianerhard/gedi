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
