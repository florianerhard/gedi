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
package gedi.plot.renderables.legend;

import gedi.plot.aesthetics.Aesthetic;
import gedi.plot.aesthetics.DoubleAesthetic;
import gedi.plot.scale.DoubleScalingPreprocessed;

public interface DoubleLegend extends Legend<Double,DoubleScalingPreprocessed> {

	
	default void setAesthetic(Aesthetic<Double, DoubleScalingPreprocessed> aesthetic) {
		setDoubleAesthetic((DoubleAesthetic) aesthetic);
	}

	void setDoubleAesthetic(DoubleAesthetic aesthetic);

}
