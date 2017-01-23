package gedi.lfc.downsampling;

import gedi.lfc.Downsampling;


public class SquareRootDownsampling extends Downsampling {

	@Override
	protected void downsample(double[] counts) {
		for (int i=0; i<counts.length; i++)
			counts[i] = Math.sqrt(counts[i]);
	}

}
