package gedi.core.region.feature.features;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.AlignedReadsMismatch;
import gedi.core.data.reads.AlignedReadsVariation;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureDescription;

import java.util.ArrayList;
import java.util.Set;
import java.util.function.Predicate;


@GenomicRegionFeatureDescription(toType=Integer.class)
public class ReadMismatchCountFeature extends AbstractFeature<Integer> {

	private Ignorer ignore = null;

	public ReadMismatchCountFeature() {
		minValues = maxValues = 1;
		minInputs = maxInputs = 0;
	}


	public boolean dependsOnData() {
		return true;
	}


	public void setIgnoreLeadingMismatch() {
		ignore = (d,p)->d.getMismatchPos(0, p)==0;
//		ignore = m->m.getPosition()==0;
	}
	
	public void setIgnoreTtoC() {
		ignore = (d,p)->d.getMismatchRead(0, p).charAt(0)=='C' && d.getMismatchGenomic(0, p).charAt(0)=='T';
//		ignore = m->m.getReadSequence().charAt(0)=='C' && m.getReferenceSequence().charAt(0)=='T';
	}


	@Override
	public GenomicRegionFeature<Integer> copy() {
		ReadMismatchCountFeature re = new ReadMismatchCountFeature();
		re.copyProperties(this);
		re.ignore = ignore;
		return re;
	}


	@Override
	protected void accept_internal(Set<Integer> values) {
		AlignedReadsData d = (AlignedReadsData) referenceRegion.getData();

		int re = 0;
		int v = d.getVariationCount(0);
		for (int i=0; i<v; i++) {
			if (d.isMismatch(0, i)) {
				if (ignore==null || !ignore.ignore(d,i)) 
//				if (ignore==null || !ignore.test((AlignedReadsMismatch) d.getVariation(0, i)))
					re++;
			}
		}

		values.add(re);

	}


	private static interface Ignorer {
		boolean ignore(AlignedReadsData d, int p);
	}

}
