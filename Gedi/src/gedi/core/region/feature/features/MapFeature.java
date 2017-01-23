package gedi.core.region.feature.features;

import gedi.core.data.annotation.Transcript;
import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureDescription;

import java.util.Set;


@GenomicRegionFeatureDescription(toType=Object.class)
public class MapFeature extends AbstractFeature<Object> {

	public MapFeature() {
		minValues = maxValues = 1;
		minInputs = maxInputs = 1;
	}

	@Override
	protected void accept_internal(Set<Object> values) {
		values.addAll(getInput(0));
	}

	@Override
	public GenomicRegionFeature<Object> copy() {
		MapFeature re = new MapFeature();
		re.copyProperties(this);
		return re;
	}
	
}
