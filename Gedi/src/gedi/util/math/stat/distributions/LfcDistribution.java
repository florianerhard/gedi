package gedi.util.math.stat.distributions;

import jdistlib.Beta;
import jdistlib.math.MathFunctions;

public class LfcDistribution {

	public static double ptol(double p) {
		return Math.log(p/(1-p))/Math.log(2);
	}
	
	public static double ltop(double l) {
		return Math.pow(2,l)/(1+Math.pow(2,l));
	}
	
	public static double dlfc(double l, double a, double b, boolean log_p) {
		double r = (a*l+1)*Math.log(2)-MathFunctions.lbeta(a, b)-(a+b)*Math.log(1+Math.pow(2, l));
		if (!log_p) r = Math.exp(r);
		return r;
	}
	
	public static double plfc(double l, double a, double b, boolean lower_tail, boolean log_p) {
		return Beta.cumulative(ltop(l), a, b, lower_tail, log_p);
	}
	
	public static double qlfc(double alpha, double a, double b, boolean lower_tail, boolean log_p) {
		return Beta.quantile(alpha, a, b, lower_tail, log_p);
	}
	
}
