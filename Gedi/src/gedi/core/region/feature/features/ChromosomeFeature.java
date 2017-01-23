package gedi.core.region.feature.features;

import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureDescription;

import java.util.ArrayList;
import java.util.Set;


@GenomicRegionFeatureDescription(toType=String.class)
public class ChromosomeFeature extends AbstractFeature<String> {

	
	public ChromosomeFeature() {
		minValues = maxValues = 1;
		minInputs = maxInputs = 0;
	}

	@Override
	protected void accept_internal(Set<String> values) {
		values.add(referenceRegion.getReference().toPlusMinusString());
	}

	@Override
	public GenomicRegionFeature<String> copy() {
		ChromosomeFeature re = new ChromosomeFeature();
		re.copyProperties(this);
		return re;
	}
	
}
