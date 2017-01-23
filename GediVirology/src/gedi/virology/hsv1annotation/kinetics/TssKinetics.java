package gedi.virology.hsv1annotation.kinetics;

import gedi.core.data.annotation.NameAnnotation;
import gedi.core.data.numeric.GenomicNumericProvider.PositionNumericIterator;
import gedi.core.data.numeric.diskrmq.DiskGenomicNumericProvider;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.lfc.Downsampling;
import gedi.lfc.downsampling.LogscDownsampling;
import gedi.util.ArrayUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.functions.ExtendedIterator;

import java.util.Arrays;

public class TssKinetics extends KineticProvider {

	private MemoryIntervalTreeStorage<NameAnnotation> mrna;
	private DiskGenomicNumericProvider tss;
	
	private int tssRegion = 5;
	private Downsampling downsampling = new LogscDownsampling();

	public TssKinetics(MemoryIntervalTreeStorage<NameAnnotation> mrna,
			DiskGenomicNumericProvider tss, NumericArray sizeFactors, String replicate) {
		super("TSS",replicate,sizeFactors);
		this.mrna = mrna;
		this.tss = tss;
	}
	

	@Override
	public ExtendedIterator<ReferenceGenomicRegion<? extends NumericArray>> ei(
			ReferenceGenomicRegion<?> region) {
		double[] buff = new double[tss.getNumDataRows()];
		
		return mrna.ei(region)
			.map(mr->mr.map(new ImmutableReferenceGenomicRegion<Void>(mr.getReference(), new ArrayGenomicRegion(0,tssRegion))))
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
	
	
	
}
