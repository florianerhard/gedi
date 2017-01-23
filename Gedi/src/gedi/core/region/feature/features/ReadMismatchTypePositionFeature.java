package gedi.core.region.feature.features;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureDescription;

import java.util.ArrayList;
import java.util.Set;


@GenomicRegionFeatureDescription(toType=String.class)
public class ReadMismatchTypePositionFeature extends AbstractFeature<String> {

	

	private String between1 = "->";
	private String between2 = " ";

	public ReadMismatchTypePositionFeature() {
		minInputs = maxInputs = 0;
	}
	
	
	public boolean dependsOnData() {
		return true;
	}


	@Override
	public GenomicRegionFeature<String> copy() {
		ReadMismatchTypePositionFeature re = new ReadMismatchTypePositionFeature();
		re.copyProperties(this);
		re.between1 = between1;
		re.between2 = between2;
		return re;
	}
	
	@Override
	protected void accept_internal(Set<String> values) {
		AlignedReadsData d = (AlignedReadsData) referenceRegion.getData();
		
		int v = d.getVariationCount(0);
		for (int i=0; i<v; i++) {
			
			if (d.isMismatch(0, i)) {
				values.add(d.getMismatchGenomic(0, i)+between1+d.getMismatchRead(0, i)+between2+d.getMismatchPos(0, i));
			}
			
		}
		
	}

	

}
