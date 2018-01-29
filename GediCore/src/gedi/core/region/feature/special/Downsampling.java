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

package gedi.core.region.feature.special;

import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.functions.NumericArrayFunction;

public enum Downsampling {

	Max {
		@Override
		public NumericArray downsample(NumericArray a) {
			double max = a.evaluate(NumericArrayFunction.Max);
			if (max<=1) return a;
			for (int i=0; i<a.length(); i++)
				a.setDouble(i,a.getDouble(i)/max);
			return a;
		}
	},

	Logsc {
		@Override
		public NumericArray downsample(NumericArray a) {
			double max = a.evaluate(NumericArrayFunction.Max);
			if (max<=1) return a;
			double fac = Math.log(max)/Math.log(2)/max;
			for (int i=0; i<a.length(); i++)
				a.setDouble(i,a.getDouble(i)*fac);
			return a;
		}
	},
	
	No{
		@Override
		public NumericArray downsample(NumericArray a) {
			return a;
		}
	},
	
	Digital {
		@Override
		public NumericArray downsample(NumericArray a) {
			for (int i=0; i<a.length(); i++)
				if (a.getDouble(i)>1)
					a.setDouble(i,1);
			return a;
		}
	};
	
	public abstract NumericArray downsample(NumericArray a);
	
	
}
