package gedi.virology.hsv1annotation.kinetics.provider;

import gedi.core.data.annotation.NameAnnotation;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.functions.ExtendedIterator;
import gedi.util.mutable.MutableInteger;

public class TssProvider implements KineticRegionProvider {

	private MemoryIntervalTreeStorage<NameAnnotation> mrna;
	
	private int tssRegion = 5;

	public TssProvider(MemoryIntervalTreeStorage<NameAnnotation> mrna) {
		this.mrna = mrna;
	}
	
	@Override
	public String getType() {
		return "TSS";
	}
	

	@Override
	public ExtendedIterator<ImmutableReferenceGenomicRegion<NameAnnotation>> ei(
			ReferenceGenomicRegion<?> region) {
		
		MutableInteger n = new MutableInteger(1);
		return mrna.ei(region)
			.map(mr->new ImmutableReferenceGenomicRegion<Void>(mr.getReference(), mr.map(new ArrayGenomicRegion(0,tssRegion))))
			.unique(true)
			.map(r->new ImmutableReferenceGenomicRegion<NameAnnotation>(r.getReference(), r.getRegion(), new NameAnnotation("TSS "+(n.N++))));
	}
	
	
	
}
