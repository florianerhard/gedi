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
package gedi.util.functions;

import java.util.function.ToDoubleBiFunction;



public interface Measure<C> extends ToDoubleBiFunction<C, C> {

	
	public enum MeasureType {
		Similarity,Distance, Unknown
	}
	
	default MeasureType type() {
		return MeasureType.Unknown;
	}
	
	default double[][] createMatrix(C[] a) {
		return createMatrix(a,null);
	}
	default double[][] createMatrix(C[] a, double[][] re) {
		if (re==null || re.length!=a.length || re[0].length!=a.length)
			re = new double[a.length][a.length];
		for (int i=0; i<a.length; i++) 
			for (int j=i+1; j<a.length; j++) 
				re[j][i] = re[i][j] = applyAsDouble(a[i], a[j]);
		return re;
	}
	
	
}
