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

package gedi.core.data.mapper;

import gedi.gui.genovis.pixelMapping.PixelBlockToValuesMap;
import gedi.util.datastructure.array.NumericArray;

import java.util.function.UnaryOperator;

@GenomicRegionDataMapping(fromType=PixelBlockToValuesMap.class,toType=PixelBlockToValuesMap.class)
public class NumericNormalize extends NumericCompute {

	

	public NumericNormalize(double[] totals) {
		super(new RpmNormalize(totals));
	}

	private static class RpmNormalize implements UnaryOperator<NumericArray> {

		private double[] totals;

		public RpmNormalize(double[] totals) {
			this.totals = totals;
		}

		@Override
		public NumericArray apply(NumericArray t) {
			if (totals.length!=1 && totals.length!=t.length())
				throw new RuntimeException("Given normalization constants do not match the input data!");
			NumericArray re = t.copy();
			for (int i=0; i<re.length(); i++)
				re.mult(i,1E6/totals[totals.length==1?0:i]);
			return re;
		}
		
	}


	
}
