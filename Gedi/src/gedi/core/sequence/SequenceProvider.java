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
