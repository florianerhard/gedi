package gedi.core.region.feature.features;

import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureDescription;
import gedi.util.math.stat.binning.FixedSizeBinning;
import gedi.util.math.stat.descriptive.Entropy;
import gedi.util.math.stat.factor.Factor;

import java.util.Set;


@GenomicRegionFeatureDescription(toType=Factor.class)
public class EntropyFeature extends BinningFeature {

	
	private int order = 2;
	
	
	public EntropyFeature() {
		minValues = 0;
		maxValues = Integer.MAX_VALUE;
		minInputs = 0;
		maxInputs = Integer.MAX_VALUE;
		binning = new FixedSizeBinning(0, 4, 100);
	}
	
	
	public void setOrder(int order) {
		this.order = order;
	}
	
	
	@Override
	public GenomicRegionFeature<Factor> copy() {
		EntropyFeature re = new EntropyFeature();
		re.copyProperties(this);
		re.order = order;
		re.binning = binning;
		return re;
	}

	@Override
	protected void accept_internal(Set<Factor> values) {
		for (Object seq : getInput(0))
			values.add(binning.apply(Entropy.compute((String)seq, order)));
	}

	
}
