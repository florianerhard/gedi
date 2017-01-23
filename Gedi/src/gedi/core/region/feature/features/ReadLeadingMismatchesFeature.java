package gedi.core.region.feature.features;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureDescription;

import java.util.Set;


@GenomicRegionFeatureDescription(toType=Integer.class)
public class ReadLeadingMismatchesFeature extends AbstractFeature<Integer> {

	private boolean count = false;

	public ReadLeadingMismatchesFeature() {
		minInputs = maxInputs = 0;
	}
	
	
	public boolean dependsOnData() {
		return true;
	}
	
	public void setCount(boolean count) {
		this.count = count;
	}
	
	@Override
	public GenomicRegionFeature<Integer> copy() {
		ReadLeadingMismatchesFeature re = new ReadLeadingMismatchesFeature();
		re.copyProperties(this);
		re.count = count;
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
			else if (d.isSoftclip(0, i)) {
				values.add(d.getSoftclipPos(0, i));
			}
		}
		
		int mm = 0;
		for (; mm<values.size() &&values.contains (mm); mm++);
		values.clear();
		values.add(count?mm:Math.min(mm, 1));
	}

	

}
