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
import gedi.core.region.feature.special.UnfoldGenomicRegionStatistics;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.functions.EI;

import java.util.ArrayList;
import java.util.Set;
import java.util.function.UnaryOperator;


public abstract class AbstractReadMismatchFeature extends AbstractFeature<UnfoldGenomicRegionStatistics> {

	

	public AbstractReadMismatchFeature() {
		minInputs = maxInputs = 0;
	}
	
	public boolean dependsOnData() {
		return true;
	}
	
	@Override
	protected void accept_internal(Set<UnfoldGenomicRegionStatistics> values) {
		AlignedReadsData d = (AlignedReadsData) referenceRegion.getData();
		
		int v = d.getVariationCount(0);
		values.add(()->EI.seq(0, v).filter(i->d.isMismatch(0, i)).map(i->getReturnValue(d.getMismatchPos(0, i),d.getMismatchGenomic(0, i).toString(),d.getMismatchRead(0, i).toString())));
//		for (int i=0; i<v; i++) {
//			
//			if (d.isMismatch(0, i)) {
//				values.add(d.getMismatchGenomic(0, i)+between+d.getMismatchRead(0, i));
//			}
//		}
		
	}
	

	protected abstract String getReturnValue(int pos, String g, String r) ;


	

}
