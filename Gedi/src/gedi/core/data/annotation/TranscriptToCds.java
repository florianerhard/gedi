package gedi.core.data.annotation;

import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;

import java.util.function.UnaryOperator;

public class TranscriptToCds implements UnaryOperator<MemoryIntervalTreeStorage<Transcript>> {

	@Override
	public MemoryIntervalTreeStorage<Transcript> apply(
			MemoryIntervalTreeStorage<Transcript> storage) {
		return storage.convert(rgr->rgr.getData().isCoding()?rgr.setRegion(rgr.getData().getCds(rgr.getReference(),rgr.getRegion())):null, Transcript.class);
	}

}
