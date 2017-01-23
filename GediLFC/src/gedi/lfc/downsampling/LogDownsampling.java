package gedi.lfc.downsampling;

import gedi.lfc.Downsampling;

public class LogDownsampling extends Downsampling {

	@Override
	protected void downsample(double[] counts) {

		for (int i=0; i<counts.length; i++)
			counts[i] = counts[i]<=1?counts[i]:(Math.log(counts[i])/Math.log(2));

	}
}

