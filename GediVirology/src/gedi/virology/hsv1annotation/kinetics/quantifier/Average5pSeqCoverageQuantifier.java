package gedi.virology.hsv1annotation.kinetics.quantifier;

import gedi.core.data.annotation.NameAnnotation;
import gedi.core.data.numeric.GenomicNumericProvider.PositionNumericIterator;
import gedi.core.data.numeric.diskrmq.DiskGenomicNumericProvider;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.ArrayUtils;
import gedi.util.datastructure.array.NumericArray;

import java.util.Arrays;

public class Average5pSeqCoverageQuantifier extends KineticQuantifier {

	private DiskGenomicNumericProvider tss;
	private int skip5p = 0;
	
	public Average5pSeqCoverageQuantifier(String label,
			String repl, NumericArray sizeFactors,DiskGenomicNumericProvider tss ) {
		super(label,repl, sizeFactors);
		this.tss = tss;
	}
	
	public Average5pSeqCoverageQuantifier skip5p(int skip5p) {
		this.skip5p = skip5p;
		return this;
	}

	@Override
	public NumericArray[] quantify(ReferenceGenomicRegion<?> fullRegion, ReferenceGenomicRegion<NameAnnotation>[] regions) {
		if (regions.length==0) return new NumericArray[0];
		
		NumericArray[] re = new NumericArray[regions.length];
		double[] buff = new double[tss.getNumDataRows()];
		
		for (int i=0; i<re.length; i++) {
			PositionNumericIterator pit = tss.iterateValues(regions[i]);
			Arrays.fill(buff, 0);
			double[] total = buff.clone();
			while (pit.hasNext()) {
				int pos = regions[i].induce(pit.nextInt());
				if (pos>=skip5p) {
					pit.getValues(buff);
					downsampling.getDownsampled(buff, buff);
					ArrayUtils.add(total, buff);
				}
			}
			ArrayUtils.mult(total, 1.0/(regions[i].getRegion().getTotalLength()-skip5p));
			re[i] = NumericArray.wrap(total);
		}
		
		return re;
	}

}
