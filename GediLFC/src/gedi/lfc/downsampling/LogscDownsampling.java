package gedi.lfc.downsampling;

import gedi.lfc.Downsampling;
import gedi.util.ArrayUtils;

public class LogscDownsampling extends Downsampling {

	@Override
	protected void downsample(double[] counts) {
		
		int max = ArrayUtils.argmax(counts);
		if (counts[max]<=1) return;
		double fac = Math.log(counts[max])/Math.log(2)/counts[max];
		for (int i=0; i<counts.length; i++)
			counts[i] *= fac;
		
	}

}
