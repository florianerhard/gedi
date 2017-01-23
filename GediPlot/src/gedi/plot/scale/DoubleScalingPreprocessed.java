package gedi.plot.scale;

import gedi.util.PaintUtils;
import gedi.util.datastructure.dataframe.DataColumn;
import gedi.util.mutable.MutableInteger;

public class DoubleScalingPreprocessed {

	private double min;
	private double max;
	
	public DoubleScalingPreprocessed(double min, double max) {
		this.min = min;
		this.max = max;
	}
	
	
	public double getMin() {
		return min;
	}
	
	public double getMax() {
		return max;
	}
	
	public void mix(DoubleScalingPreprocessed other) {
		other.min = this.min = Math.min(min, other.min);
		other.max = this.max = Math.max(max, other.max);
	}

	@Override
	public String toString() {
		return "DoubleScalingPreprocessed [min=" + min + ", max=" + max + "]";
	}
	
	
	
	public static DoubleScalingPreprocessed compute(DataColumn<?> c) {
		double min = Double.NaN;
		double max = Double.NaN;
		for (int r=0; r<c.size(); r++) {
			double v = c.getDoubleValue(r);
			if (!Double.isNaN(v) && !Double.isInfinite(v)) {
				if(Double.isNaN(min)) {
					min = max = v;
				} else {
					min = Math.min(min, v);
					max = Math.max(max, v);
				}
			}
		}
		if (Double.isNaN(min))
			return new DoubleScalingPreprocessed(0, 1);
		return new DoubleScalingPreprocessed(min, max);
	}

	
	
}
