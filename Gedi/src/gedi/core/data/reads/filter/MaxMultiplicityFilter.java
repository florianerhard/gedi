package gedi.core.data.reads.filter;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.AlignedReadsDataFactory;
import gedi.core.region.ReferenceGenomicRegion;

import java.util.function.UnaryOperator;

public class MaxMultiplicityFilter implements UnaryOperator<ReferenceGenomicRegion<AlignedReadsData>>{

	private AlignedReadsDataFactory fac;
	private int maxMulti;
	
	
	public MaxMultiplicityFilter(int maxMulti) {
		this.maxMulti = maxMulti;
	}


	@Override
	public ReferenceGenomicRegion<AlignedReadsData> apply(ReferenceGenomicRegion<AlignedReadsData> ard) {
		
		if (fac==null || fac.getNumConditions()!=ard.getData().getNumConditions())
			fac = new AlignedReadsDataFactory(ard.getData().getNumConditions());
		
		fac.start();
		for (int d = 0; d<ard.getData().getDistinctSequences(); d++)  {
			if (ard.getData().getMultiplicity(d)<=maxMulti) {
				fac.add(ard.getData(),d);
			}
		}
		if (fac.getDistinctSequences()>0)
			return ard.toMutable().setData(fac.create());
		return null;
	}

	
	
}
