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
package gedi.core.sequence;

import java.util.Set;

import gedi.core.data.annotation.ReferenceSequenceLengthProvider;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.SequenceUtils;

public interface SequenceProvider extends ReferenceSequenceLengthProvider {

	/**
	 * In 5' to 3' direction! Returns null if ref is unknown; throws an Exception if region is outside of ref's bounds
	 * @param ref
	 * @param region
	 * @return
	 */
	default CharSequence getSequence(ReferenceGenomicRegion<?> rgr) {
		if (rgr==null) return null;
		return getSequence(rgr.getReference(),rgr.getRegion());
	}
	
	
	/**
	 * In 5' to 3' direction! Returns null if ref is unknown; throws an Exception if region is outside of ref's bounds
	 * @param ref
	 * @param region
	 * @return
	 */
	default CharSequence getSequence(ReferenceSequence ref, GenomicRegion region) {
		CharSequence re = getPlusSequence(ref.getName(),region);
		if (re==null) return null;
		if (ref.getStrand()==Strand.Minus)
			re = SequenceUtils.getDnaReverseComplement(re);
		return re;
	}
	default char getSequence(ReferenceSequence ref, int pos) {
		char re = getPlusSequence(ref.getName(),pos);
		if (ref.getStrand()==Strand.Minus)
			re = SequenceUtils.getDnaComplement(re);
		return re;
	}
	default boolean knowsSequence(String name) {
		return getSequenceNames().contains(name);
	}
	
	
	Set<String> getSequenceNames();
	CharSequence getPlusSequence(String name, GenomicRegion region);
	char getPlusSequence(String name, int pos);
	
	
}
