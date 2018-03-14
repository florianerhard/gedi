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
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegionPosition;
import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureDescription;
import gedi.core.sequence.CompositeSequenceProvider;
import gedi.core.sequence.FastaIndexSequenceProvider;

import java.io.IOException;
import java.util.Set;


@GenomicRegionFeatureDescription(toType=String.class)
public class SequenceFeature extends AbstractFeature<String> {

	private CompositeSequenceProvider seq = new CompositeSequenceProvider();
	
	private GenomicRegionPosition fromPosition = GenomicRegionPosition.FivePrime;
	private GenomicRegionPosition toPosition = GenomicRegionPosition.ThreePrime;
	private int fromOffset = 0;
	private int toOffset = 0;
	
	
	public SequenceFeature() {
		minValues = maxValues = 1;
		minInputs = maxInputs = 0;
	}
	
	public void setFrom(GenomicRegionPosition position, int offset) {
		this.fromOffset = offset;
		this.fromPosition = position;
	}
	
	public void setTo(GenomicRegionPosition position, int offset) {
		this.toOffset = offset;
		this.toPosition = position;
	}
	
	@Override
	public GenomicRegionFeature<String> copy() {
		SequenceFeature re = new SequenceFeature();
		re.copyProperties(this);
		re.seq = seq;
		re.fromOffset = fromOffset;
		re.toOffset = toOffset;
		re.fromPosition = fromPosition;
		re.toPosition = toPosition;
		return re;
	}
	
	public void addFastaIndexFile(String path) throws IOException {
		seq.add(new FastaIndexSequenceProvider(path));
	}
	

	public void addGenomic(Genomic g) throws IOException {
		seq.add(g);
	}

	@Override
	protected void accept_internal(Set<String> values) {
		
		if (!fromPosition.isValidInput(referenceRegion) || !toPosition.isValidInput(referenceRegion))
			return;
		
		int from = fromPosition.position(referenceRegion, fromOffset);
		int to = toPosition.position(referenceRegion, toOffset);
		
		if (from>to) {
			int tmp=from;
			from=to;
			to=tmp;
		}
		
		ArrayGenomicRegion reg = new ArrayGenomicRegion(from,to);
		reg = reg.subtract(referenceRegion.getRegion().invert());
		int l = seq.getLength(referenceRegion.getReference().getName());
		if (reg.getStart()>=0 && reg.getEnd()<=l) 
			values.add(seq.getSequence(referenceRegion.getReference(),reg).toString());
	}

	
}
