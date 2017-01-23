package gedi.core.data.reads;

import java.util.function.BinaryOperator;

public class AlignedReadsDataSumCountOperator implements BinaryOperator<AlignedReadsData> {

	private AlignedReadsDataFactory fac;
	
	@Override
	public AlignedReadsData apply(AlignedReadsData t, AlignedReadsData u) {
		if (fac==null) fac = new AlignedReadsDataFactory(t.getNumConditions());
		if (fac.getNumConditions()!=t.getNumConditions() || fac.getNumConditions()!=u.getNumConditions())
			throw new RuntimeException("Number of conditions inconsistent!");
		
		fac.start().newDistinctSequence();
		for (int c=0; c<t.getNumConditions(); c++) {
			for (int d=0;d<t.getDistinctSequences(); d++)
				fac.incrementCount(c, t.getCount(d, c));
			for (int d=0;d<u.getDistinctSequences(); d++)
				fac.incrementCount(c, u.getCount(d, c));
		}
		return fac.create();
	}
		
}
