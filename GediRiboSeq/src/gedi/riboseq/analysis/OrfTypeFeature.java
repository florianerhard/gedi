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
package gedi.riboseq.analysis;

import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureDescription;
import gedi.core.region.feature.features.AbstractFeature;
import gedi.riboseq.inference.orf.Orf;
import gedi.riboseq.inference.orf.PriceOrf;

import java.util.Set;


@GenomicRegionFeatureDescription(toType=String.class)
public class OrfTypeFeature extends AbstractFeature<String> {

	public OrfTypeFeature() {
		minValues = maxValues = 1;
		minInputs = maxInputs = 0;
	}

	@Override
	protected void accept_internal(Set<String> values) {
		PriceOrf orf = (PriceOrf) referenceRegion.getData();
		values.add(orf.getType().name());
	}

	@Override
	public GenomicRegionFeature<String> copy() {
		OrfTypeFeature re = new OrfTypeFeature();
		re.copyProperties(this);
		return re;
	}
	
}
