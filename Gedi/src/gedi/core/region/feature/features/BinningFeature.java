package gedi.core.region.feature.features;

import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureDescription;
import gedi.util.math.stat.binning.Binning;
import gedi.util.math.stat.binning.FixedSizeBinning;
import gedi.util.math.stat.factor.Factor;

import java.util.Set;


@GenomicRegionFeatureDescription(toType=Factor.class)
public class BinningFeature extends AbstractFeature<Factor> {

	
	protected Binning binning = new FixedSizeBinning(0, 1, 100);
	
	
	public BinningFeature() {
		minValues = 0;
		maxValues = Integer.MAX_VALUE;
		minInputs = 0;
		maxInputs = Integer.MAX_VALUE;
	}
	
	public void set(Binning binning) {
		this.binning = binning;
	}
	
	
	@Override
	public GenomicRegionFeature<Factor> copy() {
		BinningFeature re = new BinningFeature();
		re.copyProperties(this);
		re.binning = binning;
		return re;
	}

	@Override
	protected void accept_internal(Set<Factor> values) {
		for (Object v : getInput(0))
			values.add(binning.apply(((Number)v).doubleValue()));
	}

	
}
