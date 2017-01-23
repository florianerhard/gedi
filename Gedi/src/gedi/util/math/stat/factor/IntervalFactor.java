package gedi.util.math.stat.factor;

import gedi.util.math.stat.binning.Binning;


public class IntervalFactor extends Factor {

	private double bottom;
	private double top;
	
	private boolean bottomInklusive;
	private boolean topInklusive;
	
	
	
	private IntervalFactor(Factor ori, double bottom, double top, boolean bottomInclusive, boolean topInclusive) {
		super(ori, ori.index, ori.below, ori.above);
		this.bottom = bottom;
		this.top = top;
		this.bottomInklusive = bottomInclusive;
		this.topInklusive = topInclusive;
	}

	public double getBottom() {
		return bottom;
	}
	public double getTop() {
		return top;
	}
	public boolean isBottomInklusive() {
		return bottomInklusive;
	}
	public boolean isTopInklusive() {
		return topInklusive;
	}
	public boolean contains(double val) {
		if (val<bottom || val>top) return false;
		if (val==bottom) return bottomInklusive;
		if (val==top) return topInklusive;
		return true;
	}

	public static Factor create(Binning binning) {
		double d = Double.POSITIVE_INFINITY;
		for (int b=0; b<binning.getBins(); b++) 
			d = Math.min(d,binning.getBinMax(b)-binning.getBinMin(b));
		
		int decimals = (int) Math.max(0, Math.ceil(-Math.log10(d)));
		String format = binning.isInteger()?"[%.0f-%.0f]":"[%."+decimals+"f-%."+decimals+"f)";
		
		
		String[] names = new String[binning.getBins()];
		for (int i=0; i<names.length; i++)
			names[i] = binning.isInteger()&&binning.getBinMin(i)==binning.getBinMax(i)?String.format("%.0f",binning.getBinMin(i)):String.format(format,binning.getBinMin(i),binning.getBinMax(i));
		
		return Factor.create(names, f->new IntervalFactor(f,binning.getBinMin(f.index),binning.getBinMax(f.index),true,false));
}
	
	
	
	
}
