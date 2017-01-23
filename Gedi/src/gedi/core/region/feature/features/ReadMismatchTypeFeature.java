package gedi.core.region.feature.features;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureDescription;

import java.util.ArrayList;
import java.util.Set;


@GenomicRegionFeatureDescription(toType=String.class)
public class ReadMismatchTypeFeature extends AbstractFeature<String> {

	

	private String between = "->";

	public ReadMismatchTypeFeature() {
		minInputs = maxInputs = 0;
	}
	
	
	
	public boolean dependsOnData() {
		return true;
	}
	
	public void setBetween(String between) {
		this.between = between;
	}

	@Override
	protected void accept_internal(Set<String> values) {
		AlignedReadsData d = (AlignedReadsData) referenceRegion.getData();
		
		int v = d.getVariationCount(0);
		for (int i=0; i<v; i++) {
			
			if (d.isMismatch(0, i)) {
				values.add(d.getMismatchGenomic(0, i)+between+d.getMismatchRead(0, i));
			}
			
		}
		
	}



	@Override
	public GenomicRegionFeature<String> copy() {
		ReadMismatchTypeFeature re = new ReadMismatchTypeFeature();
		re.copyProperties(this);
		re.between = between;
		return re;
	}

	

}
