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

import gedi.core.genomic.Genomic;
import gedi.core.region.GenomicRegionPosition;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureDescription;

import java.util.Set;


@GenomicRegionFeatureDescription(fromType=Void.class,toType=String.class)
public class GenomeFeature extends AbstractFeature<String> {

	private Genomic g;
	
	public GenomeFeature(Genomic g) {
		minInputs = maxInputs = 0;
		this.g = g;
	}
	
	@Override
	public GenomicRegionFeature<String> copy() {
		GenomeFeature re = new GenomeFeature(g);
		re.copyProperties(this);
		return re;
	}


	@Override
	protected void accept_internal(Set<String> values) {
		
		Genomic or = g.getOrigin(referenceRegion.getReference());
		if (or!=null)
			values.add(or.getId());
		
	}
	

}
