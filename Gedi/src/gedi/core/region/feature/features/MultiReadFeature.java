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
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureDescription;

import java.util.ArrayList;
import java.util.Set;


@GenomicRegionFeatureDescription(toType=Object.class)
public class MultiReadFeature extends AbstractFeature<Object> {

	
	private int maxMultiplicity = 1;
	private String ambiguous = "ambiguous";
	private String unique = "unique";
	
	private boolean outputMultiplicity = false;

	public MultiReadFeature() {
		minValues = maxValues = 1;
		minInputs = maxInputs = 0;
	}
	
	public void setOutputMultiplicity(boolean outputMultiplicity) {
		this.outputMultiplicity = outputMultiplicity;
	}
	
	
	public boolean dependsOnData() {
		return true;
	}
	
	public void setMaxMultiplicity(int maxMultiplicity) {
		this.maxMultiplicity = maxMultiplicity;
	}
	
	public void setAmbiguous(String ambiguous) {
		this.ambiguous = ambiguous;
	}
	
	public void setUnique(String unique) {
		this.unique = unique;
	}
	

	@Override
	protected void accept_internal(Set<Object> values) {
		AlignedReadsData d = (AlignedReadsData) referenceRegion.getData();
		if (outputMultiplicity)
			values.add(d.getMultiplicity(0));
		else 
			values.add(d.getMultiplicity(0)>maxMultiplicity?ambiguous:unique);
	}

	@Override
	public GenomicRegionFeature<Object> copy() {
		MultiReadFeature re = new MultiReadFeature();
		re.copyProperties(this);
		re.maxMultiplicity = maxMultiplicity;
		re.ambiguous = ambiguous;
		re.unique = unique;
		re.outputMultiplicity = outputMultiplicity;
		return re;
	}
	
	

}
