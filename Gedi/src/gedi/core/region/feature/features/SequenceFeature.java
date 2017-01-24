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
import gedi.core.sequence.CompositeSequenceProvider;
import gedi.core.sequence.FastaIndexSequenceProvider;

import java.io.IOException;
import java.util.Set;


@GenomicRegionFeatureDescription(toType=String.class)
public class SequenceFeature extends AbstractFeature<String> {

	private CompositeSequenceProvider seq = new CompositeSequenceProvider();
	
	public SequenceFeature() {
		minValues = maxValues = 1;
		minInputs = maxInputs = 0;
	}
	
	@Override
	public GenomicRegionFeature<String> copy() {
		SequenceFeature re = new SequenceFeature();
		re.copyProperties(this);
		re.seq = seq;
		return re;
	}
	
	public void addFastaIndexFile(String path) throws IOException {
		seq.add(new FastaIndexSequenceProvider(path));
	}

	@Override
	protected void accept_internal(Set<String> values) {
		values.add(seq.getSequence(referenceRegion).toString());
	}

	
}
