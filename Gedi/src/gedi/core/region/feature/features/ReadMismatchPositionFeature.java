package gedi.core.region.feature.features;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureDescription;

import java.util.ArrayList;
import java.util.Set;


@GenomicRegionFeatureDescription(toType=Integer.class)
public class ReadMismatchPositionFeature extends AbstractFeature<Integer> {

	

	public ReadMismatchPositionFeature() {
		minInputs = maxInputs = 0;
	}
	
	
	public boolean dependsOnData() {
		return true;
	}
	
	@Override
	public GenomicRegionFeature<Integer> copy() {
		ReadMismatchPositionFeature re = new ReadMismatchPositionFeature();
		re.copyProperties(this);
		return re;
	}


	@Override
	protected void accept_internal(Set<Integer> values) {
		AlignedReadsData d = (AlignedReadsData) referenceRegion.getData();
		
		int v = d.getVariationCount(0);
		for (int i=0; i<v; i++) {
			
			if (d.isMismatch(0, i)) {
				values.add(d.getMismatchPos(0, i));
			}
			
		}
		
	}

	

}
