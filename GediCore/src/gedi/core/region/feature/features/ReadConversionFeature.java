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
import gedi.core.data.reads.AlignedReadsMismatch;
import gedi.core.data.reads.AlignedReadsVariation;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.GenomicRegion;
import gedi.core.region.MissingInformationIntronInformation;
import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureDescription;
import gedi.util.SequenceUtils;

import java.util.ArrayList;
import java.util.Set;
import java.util.function.Predicate;


@GenomicRegionFeatureDescription(toType=Integer.class)
public class ReadConversionFeature extends AbstractFeature<Integer> {

	private char from = 'T';
	private char to = 'C';
	

	public ReadConversionFeature() {
		minValues = maxValues = 1;
		minInputs = maxInputs = 0;
	}

	public ReadConversionFeature setConversion(char genomic, char read) {
		this.from = genomic;
		this.to = read;
		return this;
	}

	public boolean dependsOnData() {
		return true;
	}


	@Override
	public GenomicRegionFeature<Integer> copy() {
		ReadConversionFeature re = new ReadConversionFeature();
		re.copyProperties(this);
		re.from = from;
		re.to = to;
		return re;
	}
	
	@Override
	protected void accept_internal(Set<Integer> values) {
		AlignedReadsData d = (AlignedReadsData) referenceRegion.getData();

		int re = 0;
		int v = d.getVariationCount(0);
		for (int i=0; i<v; i++) {
			char g = from;
			char r = to;
			if (d.isVariationFromSecondRead(0, i)) {
				g = SequenceUtils.getDnaComplement(g);
				r = SequenceUtils.getDnaComplement(r);
			}
			
			if (d.isMismatch(0, i) && d.getMismatchGenomic(0, i).charAt(0)==g && d.getMismatchRead(0, i).charAt(0)==r) {
				re++;
			}
		}

		values.add(re);

	}




}
