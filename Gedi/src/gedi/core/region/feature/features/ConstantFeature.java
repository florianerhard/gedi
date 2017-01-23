package gedi.core.region.feature.features;

import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureDescription;

import java.util.Set;


@GenomicRegionFeatureDescription(toType=String.class)
public class ConstantFeature extends AbstractFeature<String> {

	private String value;

	public ConstantFeature(String value) {
		this.value = value;
		minValues = maxValues = 1;
		minInputs = maxInputs = 0;
	}

	@Override
	protected void accept_internal(Set<String> values) {
		values.add(value);
	}

	@Override
	public GenomicRegionFeature<String> copy() {
		ConstantFeature re = new ConstantFeature(value);
		re.copyProperties(this);
		return re;
	}
	
}
