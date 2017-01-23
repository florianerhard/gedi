package gedi.util.math.stat.binning;

import gedi.util.ArrayUtils;

import java.util.Arrays;

public class IntegerBinning extends AbstractBinning {

	
	private double[] boundaries;

	public IntegerBinning(int... boundaries) {
		super(true);
		this.boundaries = ArrayUtils.toDouble(boundaries);
		Arrays.sort(this.boundaries);
	}
	
	@Override
	public int applyAsInt(double value) {
		int index = Arrays.binarySearch(boundaries, value);
		if (index<0) return -index-2;
		return index;
	}

	@Override
	public double getBinMin(int bin) {
		return boundaries[bin];
	}

	@Override
	public double getBinMax(int bin) {
		return boundaries[bin+1];
	}
	
	@Override
	public boolean isInBounds(double value) {
		return value>=boundaries[0] && value<boundaries[boundaries.length-1];
	}
	
	@Override
	public int getBins() {
		return boundaries.length-1;
	}
	
	
}
