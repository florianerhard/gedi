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
package gedi.core.data.reads.filter;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.AlignedReadsDataFactory;
import gedi.core.region.ReferenceGenomicRegion;

import java.util.function.UnaryOperator;

public class MaxMultiplicityFilter implements UnaryOperator<ReferenceGenomicRegion<AlignedReadsData>>{

	private AlignedReadsDataFactory fac;
	private int maxMulti;
	
	
	public MaxMultiplicityFilter(int maxMulti) {
		this.maxMulti = maxMulti;
	}


	@Override
	public ReferenceGenomicRegion<AlignedReadsData> apply(ReferenceGenomicRegion<AlignedReadsData> ard) {
		
		if (fac==null || fac.getNumConditions()!=ard.getData().getNumConditions())
			fac = new AlignedReadsDataFactory(ard.getData().getNumConditions());
		
		fac.start();
		for (int d = 0; d<ard.getData().getDistinctSequences(); d++)  {
			if (ard.getData().getMultiplicity(d)<=maxMulti) {
				fac.add(ard.getData(),d);
			}
		}
		if (fac.getDistinctSequences()>0)
			return ard.toMutable().setData(fac.create());
		return null;
	}

	
	
}
