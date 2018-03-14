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
import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureDescription;

import java.util.Set;


@GenomicRegionFeatureDescription(toType=Integer.class)
public class ReadLeadingMismatchesFeature extends AbstractFeature<Integer> {

	private boolean count = false;

	public ReadLeadingMismatchesFeature() {
		minInputs = maxInputs = 0;
		dependsOnData=true;
	}
	
	
	public void setCount(boolean count) {
		this.count = count;
	}
	
	@Override
	public GenomicRegionFeature<Integer> copy() {
		ReadLeadingMismatchesFeature re = new ReadLeadingMismatchesFeature();
		re.copyProperties(this);
		re.count = count;
		return re;
	}


	@Override
	protected void accept_internal(Set<Integer> values) {
		AlignedReadsData d = (AlignedReadsData) referenceRegion.getData();
		
		int v = d.getVariationCount(0);
		for (int i=0; i<v; i++) {
			if (d.isMismatch(0, i)) {
				values.add(d.getMismatchPos(0, i));
			}
			else if (d.isSoftclip(0, i) && d.isSoftclip5p(0, i)) {
				values.add(0);
			}
		}
		
		int mm = 0;
		for (; mm<values.size() &&values.contains (mm); mm++);
		values.clear();
		values.add(count?mm:Math.min(mm, 1));
	}

	

}
