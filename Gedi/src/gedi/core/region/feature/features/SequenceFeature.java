package gedi.core.region.feature.features;

import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.GenomicRegionFeatureDescription;
import gedi.core.sequence.CompositeSequenceProvider;
import gedi.core.sequence.FastaIndexSequenceProvider;

import java.io.IOException;
import java.util.Set;


@GenomicRegionFeatureDescription(toType=String.class)
public class SequenceFeature extends AbstractFeature<String> {

	private CompositeSequenceProvider seq = new CompositeSequenceProvider();
	
	public SequenceFeature() {
		minValues = maxValues = 1;
		minInputs = maxInputs = 0;
	}
	
	@Override
	public GenomicRegionFeature<String> copy() {
		SequenceFeature re = new SequenceFeature();
		re.copyProperties(this);
		re.seq = seq;
		return re;
	}
	
	public void addFastaIndexFile(String path) throws IOException {
		seq.add(new FastaIndexSequenceProvider(path));
	}

	@Override
	protected void accept_internal(Set<String> values) {
		values.add(seq.getSequence(referenceRegion).toString());
	}

	
}
