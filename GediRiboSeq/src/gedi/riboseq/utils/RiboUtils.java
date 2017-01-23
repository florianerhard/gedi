package gedi.riboseq.utils;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.ParseUtils;
import gedi.util.StringUtils;
import gedi.util.functions.EI;
import gedi.util.parsing.ReferenceGenomicRegionParser;

import java.util.HashSet;
import java.util.function.Predicate;

import cern.colt.bitvector.BitVector;

public class RiboUtils {

	public static boolean isLeadingMismatchInsideGenomicRegion(AlignedReadsData ard, int distinct) {
		for (int i=0; i<ard.getVariationCount(distinct); i++) {
			if (ard.isMismatch(distinct, i) && ard.getMismatchPos(distinct, i)==0)
				return true;
			if (ard.isSoftclip(distinct, i) && ard.getSoftclipPos(distinct, i)==0 && ard.getSoftclip(distinct, i).length()==1)
				return false;
		}
		throw new RuntimeException("No leading mismatch!");
	}
	
	public static boolean hasLeadingMismatch(AlignedReadsData ard, int distinct) {
		for (int i=0; i<ard.getVariationCount(distinct); i++) {
			if (ard.isMismatch(distinct, i) && ard.getMismatchPos(distinct, i)==0)
				return true;
			if (ard.isSoftclip(distinct, i) && ard.getSoftclipPos(distinct, i)==0 && ard.getSoftclip(distinct, i).length()==1)
				return true;
		}
		return false;
	}
	
	public static char getLeadingMismatch(AlignedReadsData ard, int distinct) {
		for (int i=0; i<ard.getVariationCount(distinct); i++) {
			if (ard.isMismatch(distinct, i) && ard.getMismatchPos(distinct, i)==0)
				return ard.getMismatchRead(distinct, i).charAt(0);
			if (ard.isSoftclip(distinct, i) && ard.getSoftclipPos(distinct, i)==0 && ard.getSoftclip(distinct, i).length()==1)
				return 'N';
		}
		return '\0';
	}

	public static Predicate<ReferenceGenomicRegion<AlignedReadsData>> parseReadFilter(String spec) {
		if (spec.startsWith("[") && spec.endsWith("]")) {
			HashSet<String> refs = EI.wrap(StringUtils.split(spec.substring(1, spec.length()-1), ',')).map(s->StringUtils.trim(s)).toCollection(new HashSet<String>());
			return rgr->refs.contains(rgr.getReference().getName());
		}
		ReferenceGenomicRegionParser<Void> p = new ReferenceGenomicRegionParser<Void>();
		if (p.canParse(spec)) {
			MutableReferenceGenomicRegion ref = p.apply(spec);
			return read->ref.intersects(read);
		}
		BitVector bv = ParseUtils.parseRangeBv(spec, 500);
		return rgr->bv.getQuick(rgr.getRegion().getTotalLength());
	}
	
}
