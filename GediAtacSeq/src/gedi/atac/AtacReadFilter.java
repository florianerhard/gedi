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

package gedi.atac;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.AlignedReadsDataFactory;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.region.MutableReferenceGenomicRegion;

import java.util.function.Function;

public class AtacReadFilter implements Function<MutableReferenceGenomicRegion<AlignedReadsData>,MutableReferenceGenomicRegion<AlignedReadsData>>{

	private boolean removeM = true;
	AlignedReadsDataFactory fac;
	
	
	public AtacReadFilter() {
	}
	
	public AtacReadFilter(boolean removeM) {
		this.removeM = removeM;
	}
	
	
	
	@Override
	public MutableReferenceGenomicRegion<AlignedReadsData> apply(
			MutableReferenceGenomicRegion<AlignedReadsData> t) {
		
		if (removeM && t.getReference().getName().equals("chrM"))return null;
	
		if (fac==null) fac = new AlignedReadsDataFactory(t.getData().getNumConditions());
		
		fac.start();
		fac.newDistinctSequence();
		fac.setMultiplicity(1);
		int[] count = new int[t.getData().getNumConditions()];
		
		for (int d=0; d<t.getData().getDistinctSequences(); d++) {
			if (t.getData().getMultiplicity(d)==1)
				t.getData().addCountsForConditionInt(d, count, ReadCountMode.All);
		}
		fac.setCount(count);
		
		
		return t.setData(fac.createDigital());
	}

}
