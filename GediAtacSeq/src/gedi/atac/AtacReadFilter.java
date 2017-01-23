package gedi.atac;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.AlignedReadsDataFactory;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.region.MutableReferenceGenomicRegion;

import java.util.function.Function;

public class AtacReadFilter implements Function<MutableReferenceGenomicRegion<AlignedReadsData>,MutableReferenceGenomicRegion<AlignedReadsData>>{

	private boolean removeM = true;
	AlignedReadsDataFactory fac;
	
	
	public AtacReadFilter() {
	}
	
	public AtacReadFilter(boolean removeM) {
		this.removeM = removeM;
	}
	
	
	
	@Override
	public MutableReferenceGenomicRegion<AlignedReadsData> apply(
			MutableReferenceGenomicRegion<AlignedReadsData> t) {
		
		if (removeM && t.getReference().getName().equals("chrM"))return null;
	
		if (fac==null) fac = new AlignedReadsDataFactory(t.getData().getNumConditions());
		
		fac.start();
		fac.newDistinctSequence();
		fac.setMultiplicity(1);
		int[] count = new int[t.getData().getNumConditions()];
		
		for (int d=0; d<t.getData().getDistinctSequences(); d++) {
			if (t.getData().getMultiplicity(d)==1)
				t.getData().addCountsForConditionInt(d, count, ReadCountMode.All);
		}
		fac.setCount(count);
		
		
		return t.setData(fac.createDigital());
	}

}
