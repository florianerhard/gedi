package gedi.virology.hsv1annotation.kinetics;

import gedi.core.data.annotation.NameAnnotation;
import gedi.core.data.numeric.GenomicNumericProvider.PositionNumericIterator;
import gedi.core.data.numeric.diskrmq.DiskGenomicNumericProvider;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.ArrayUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;

import java.util.ArrayList;
import java.util.Arrays;

public class TtsKinetics extends KineticProvider {

	private MemoryIntervalTreeStorage<NameAnnotation> mrna;
	private DiskGenomicNumericProvider tss;
	
	private int maxUpstream = 500;

	public TtsKinetics(MemoryIntervalTreeStorage<NameAnnotation> mrna,
			DiskGenomicNumericProvider tss, NumericArray sizeFactors, String replicate) {
		super("TTS",replicate,sizeFactors);
		this.mrna = mrna;
		this.tss = tss;
	}
	

	@Override
	public ExtendedIterator<ReferenceGenomicRegion<? extends NumericArray>> ei(
			ReferenceGenomicRegion<?> region) {
		double[] buff = new double[tss.getNumDataRows()];
		
		return mrna.ei(region)
			.map(mr->getUpstream(mr))
			.map(down->restrict(down))
			.unique(false)
			.map(r->{
				PositionNumericIterator pit = tss.iterateValues(r);
				Arrays.fill(buff, 0);
				double[] total = buff.clone();
				while (pit.hasNext()) {
					pit.nextInt();
					pit.getValues(buff);
					downsampling.getDownsampled(buff, buff);
					ArrayUtils.add(total, buff);
				}
				return new ImmutableReferenceGenomicRegion<NumericArray>(r.getReference(), r.getRegion(), NumericArray.wrap(total));
			});		
	}
	
	public ReferenceGenomicRegion<Void> getUpstream(ReferenceGenomicRegion<?> r) {
		GenomicRegion reg = r.getRegion();
		return new ImmutableReferenceGenomicRegion<Void>(r.getReference(), r.getReference().getStrand().equals(Strand.Plus)?new ArrayGenomicRegion(reg.getEnd()-maxUpstream,reg.getEnd()):new ArrayGenomicRegion(reg.getStart(),reg.getStart()+maxUpstream));
	}
	
	public <T> ReferenceGenomicRegion<T> restrict(ReferenceGenomicRegion<T> r) {
		ArrayList<GenomicRegion> other = mrna.ei(r).map(m->m.getRegion()).list();
		if (other.size()==0) return r;
		
		if (r.getReference().getStrand().equals(Strand.Plus)){
			int first = EI.wrap(other).mapToInt(o->o.getEnd()).filterInt(i->r.getRegion().contains(i)).max();
			if (first==Integer.MIN_VALUE)
				return r;
			return r.toMutable().transformRegion(reg->reg.subtract(new ArrayGenomicRegion(first,reg.getEnd())));
		}
		
		int first = EI.wrap(other).mapToInt(o->o.getStart()).filterInt(i->r.getRegion().contains(i)).min();
		if (first==Integer.MAX_VALUE)
			return r;
		return r.toMutable().transformRegion(reg->reg.subtract(new ArrayGenomicRegion(reg.getStart(),first)));
	}
	
}
