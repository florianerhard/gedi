package gedi.lfc.downsampling;

import gedi.lfc.Downsampling;
import gedi.util.ArrayUtils;

public class MaxDownsampling extends Downsampling {

	@Override
	protected void downsample(double[] counts) {
		double max = ArrayUtils.max(counts);
		if (max>0)
			ArrayUtils.mult(counts, 1/max);
	}

}
