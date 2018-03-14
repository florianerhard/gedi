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
package gedi.util.genomic;

import gedi.core.reference.Strand;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;

import java.util.function.Function;

/**
 * The given reference sequence must refer to a known transcript; if unknown, null is returned!
 * @author erhard
 *
 */
public class TranscriptToGenomeMapper<T> implements Function<ImmutableReferenceGenomicRegion<T>, ImmutableReferenceGenomicRegion<T>> {

	protected Function<String,? extends ReferenceGenomicRegion<T>> transcripts;
	
	
	/**
	 * for setting it in the subclasses constructor!
	 */
	protected TranscriptToGenomeMapper(){
		
	}
	
	public TranscriptToGenomeMapper(Function<String, ImmutableReferenceGenomicRegion<T>> transcripts) {
		this.transcripts = transcripts;
	}



	@Override
	public ImmutableReferenceGenomicRegion<T> apply(ImmutableReferenceGenomicRegion<T> t) {
		ReferenceGenomicRegion<T> genomic = transcripts.apply(t.getReference().getName());
		if (genomic==null) return null;
		
		GenomicRegion reg = t.getRegion();
		if (genomic.getReference().getStrand()==Strand.Minus) reg = reg.reverse(genomic.getRegion().getTotalLength());
		
		return new ImmutableReferenceGenomicRegion<T>(genomic.getReference(),genomic.getRegion().map(reg));
		
	}

}
