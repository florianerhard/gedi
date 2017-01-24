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

package gedi.lfc.downsampling;

import gedi.lfc.Downsampling;
import gedi.util.ArrayUtils;

public class MaxDownsampling extends Downsampling {

	@Override
	protected void downsample(double[] counts) {
		double max = ArrayUtils.max(counts);
		if (max>0)
			ArrayUtils.mult(counts, 1/max);
	}

}
