package gedi.util.math.stat.testing;

import jdistlib.ChiSquare;


/**
 * Not threadsafe!
 * @author erhard
 *
 */
public class PValueCombiner {

	private double accum = 0;
	private int count;
	
	
	public void add(double pval) {
		accum +=Math.log(pval);
		count++;
	}
	
	public void clear() {
		accum = 0;
		count = 0;
	}
	public double combineFisher() {
		if (Double.isInfinite(accum) && accum<0) return 0;
		return ChiSquare.cumulative(-2*accum, 2*count, false, false);
	}
			
	
	public static double combineFisher(double[] pvals) {
		return combineFisher(pvals,0,pvals.length);
	}
	public static double combineFisher(double[] pvals, int start, int length) {
		double x = 0;
		for (int i=start+length-1; i>=start; i--)
			x+=Math.log(pvals[i]);
		if (Double.isInfinite(x) && x<0) return 0;
		return ChiSquare.cumulative(-2*x, 2*length, false, false);
	}
	
}
