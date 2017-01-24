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

package gedi.core.region.feature.features;

import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureDescription;

import java.util.Set;


@GenomicRegionFeatureDescription(toType=String.class)
public class ConstantFeature extends AbstractFeature<String> {

	private String value;

	public ConstantFeature(String value) {
		this.value = value;
		minValues = maxValues = 1;
		minInputs = maxInputs = 0;
	}

	@Override
	protected void accept_internal(Set<String> values) {
		values.add(value);
	}

	@Override
	public GenomicRegionFeature<String> copy() {
		ConstantFeature re = new ConstantFeature(value);
		re.copyProperties(this);
		return re;
	}
	
}
