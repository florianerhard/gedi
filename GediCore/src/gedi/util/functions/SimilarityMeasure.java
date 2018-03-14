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

import java.util.function.Function;



public interface SimilarityMeasure<C> extends Measure<C> {

	@Override
	default MeasureType type() {
		return MeasureType.Similarity;
	}

	public static SimilarityMeasure<double[]> BHATTACHARYYA_COEFFICIENT = (a,b)->{
		double co = 0;
		for (int i=0; i<a.length; i++)
			co+=Math.sqrt(a[i]*b[i]);
		return co;
	};
	
	
	default <O> SimilarityMeasure<O> adapt(Function<O,C> map) {
		return new SimilarityMeasure<O>() {
			@Override
			public double applyAsDouble(O t, O u) {
				return SimilarityMeasure.this.applyAsDouble(map.apply(t),map.apply(u));
			}
		};
	}
	
}
