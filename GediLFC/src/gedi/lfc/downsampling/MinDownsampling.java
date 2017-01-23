package gedi.lfc.downsampling;

import gedi.lfc.Downsampling;

public class MinDownsampling extends Downsampling {

	@Override
	protected void downsample(double[] counts) {
		double n = Double.POSITIVE_INFINITY;
		for (int i=0; i<counts.length; i++)
			if (counts[i]>0)
				n = Math.min(n, counts[i]);
		if (Double.isInfinite(n))
			return;

		for (int i=0; i<counts.length; i++)
			counts[i] = counts[i]/n;
	}
}

