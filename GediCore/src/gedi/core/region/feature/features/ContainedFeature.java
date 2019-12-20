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

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.region.GenomicRegionPosition;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureDescription;

import java.util.Set;


@GenomicRegionFeatureDescription(fromType=ReferenceGenomicRegion.class,toType=ReferenceGenomicRegion.class)
public class ContainedFeature extends AbstractFeature<ReferenceGenomicRegion<?>> {

	
	
	
	
	
	public ContainedFeature() {
		minInputs = maxInputs = 1;
	}
	
	@Override
	public GenomicRegionFeature<ReferenceGenomicRegion<?>> copy() {
		ContainedFeature re = new ContainedFeature();
		re.copyProperties(this);
		return re;
	}


	@Override
	protected void accept_internal(Set<ReferenceGenomicRegion<?>> values) {
	
		Set<ReferenceGenomicRegion<?>> inputs = getInput(0);
		
		for (ReferenceGenomicRegion<?> rgr : inputs) {
			if (referenceRegion.getData() instanceof AlignedReadsData) {
				AlignedReadsData d = (AlignedReadsData)referenceRegion.getData();
				if (d.isConsistentlyContained(referenceRegion,rgr,0))
					values.add(rgr);
			}
			else if (rgr.getRegion().containsUnspliced(referenceRegion.getRegion()))
				values.add(rgr);
			
		}
		
	}
	

}
