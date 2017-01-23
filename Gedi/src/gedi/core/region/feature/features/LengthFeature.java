package gedi.core.region.feature.features;

import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureDescription;

import java.util.Set;


@GenomicRegionFeatureDescription(toType=Integer.class)
public class LengthFeature extends AbstractFeature<Integer> {

	public LengthFeature() {
		minValues = maxValues = 1;
		minInputs = maxInputs = 0;
	}

	@Override
	protected void accept_internal(Set<Integer> values) {
		values.add(referenceRegion.getRegion().getTotalLength());
	}

	@Override
	public GenomicRegionFeature<Integer> copy() {
		LengthFeature re = new LengthFeature();
		re.copyProperties(this);
		return re;
	}
	
}
