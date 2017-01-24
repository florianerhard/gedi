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

package gedi.core.region;

import gedi.core.data.annotation.Transcript;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;

public enum GenomicRegionPosition {
	Start{
		@Override
		public int position(ReferenceSequence reference, GenomicRegion region, int offset) {
			return region.getStart()+offset;
		}
	},
	Stop {
		@Override
		public int position(ReferenceSequence reference, GenomicRegion region, int offset) {
			return region.getStop()+offset;
		}
	},
	End {
		@Override
		public int position(ReferenceSequence reference, GenomicRegion region, int offset) {
			return region.getEnd()+offset;
		}
	},
	FivePrime {
		@Override
		public int position(ReferenceSequence reference, GenomicRegion region, int offset) {
			return reference.getStrand()==Strand.Minus?region.getStop()-offset:region.getStart()+offset;
		}
	},
	ThreePrime {
		@Override
		public int position(ReferenceSequence reference, GenomicRegion region, int offset) {
			return reference.getStrand()==Strand.Minus?region.getStart()-offset:region.getStop()+offset;
		}
	}, 
	/** 
	 * If length is odd, the center is unique (e.g. 5 bp, the center is at index 2). If it is even, the center
	 * is the base closer to the 5' end. If region is spliced, the mapped induced position is taken!
	 */
	Center {
		@Override
		public int position(ReferenceSequence reference, GenomicRegion region, int offset) {
			int s = region.getTotalLength();
			if ((s&1)==1) s = s/2;
			else s=reference.getStrand()==Strand.Minus?s/2-1:s/2;
			return region.map(s)+(reference.getStrand()==Strand.Minus?offset:-offset);
		}
	},
	
	StartCodon {
		@Override
		public int position(ReferenceSequence reference, GenomicRegion region, int offset) {
			throw new RuntimeException("Must be called with referencesequence!");
		}
		
		@Override
		public int position(ReferenceGenomicRegion<?> ref, int offset) {
			if (!(ref.getData() instanceof Transcript)) throw new RuntimeException("Can only be called with Transcript data!");
			Transcript t = (Transcript) ref.getData();
			if (!t.isCoding()) throw new RuntimeException("Not a coding trancript!");
			if (ref.getReference().getStrand()==Strand.Minus)
				return t.getCodingEnd()-1;
			return t.getCodingStart();
		}
		
		@Override
		public boolean isValidInput(ReferenceGenomicRegion<?> ref) {
			if (!(ref.getData() instanceof Transcript))return false;
			Transcript t = (Transcript) ref.getData();
			if (!t.isCoding()) return false;
			return true;
		}
	},
	StopCodon {
		@Override
		public int position(ReferenceSequence reference, GenomicRegion region, int offset) {
			throw new RuntimeException("Must be called with referencesequence!");
		}
		
		@Override
		public int position(ReferenceGenomicRegion<?> ref, int offset) {
			if (!(ref.getData() instanceof Transcript)) throw new RuntimeException("Can only be called with Transcript data!");
			Transcript t = (Transcript) ref.getData();
			if (!t.isCoding()) throw new RuntimeException("Not a coding trancript!");
			if (ref.getReference().getStrand()==Strand.Minus)
				return t.getCodingStart()+2;
			return t.getCodingEnd()-3;
		}
		
		@Override
		public boolean isValidInput(ReferenceGenomicRegion<?> ref) {
			if (!(ref.getData() instanceof Transcript))return false;
			Transcript t = (Transcript) ref.getData();
			if (!t.isCoding()) return false;
			return true;
		}
	}
	
	;
	
	/**
	 * Offset is added depending on the value (start, stop w.r.t. genomic position, to 5' to 3' direction otherwise)
	 * @param reference
	 * @param region
	 * @param offset
	 * @return
	 */
	public abstract int position(ReferenceSequence reference, GenomicRegion region, int offset);
	
	public int position(ReferenceGenomicRegion<?> ref, int offset) {
		return position(ref.getReference(), ref.getRegion(), offset);
	}
	
	public boolean isValidInput(ReferenceGenomicRegion<?> ref) {
		return true;
	}
	
	public int position(ReferenceSequence reference, GenomicRegion region) {
		return position(reference, region, 0);
	}
	
	public int position(ReferenceGenomicRegion<?> ref) {
		return position(ref.getReference(), ref.getRegion(), 0);
	}
}