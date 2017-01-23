package gedi.util.math.stat.descriptive;

/**
 * Single-pass constant space computation of mean and variance according to [Knuth.The Art of Computer Programming, volume 2 (1998)]
 * @author erhard
 *
 */
public class MeanVarianceOnline {

	private int n=0;
	private double mean = 0;
	private double M2 = 0;

	public boolean add(double x) {
		if (Double.isNaN(x) || Double.isInfinite(x)) 
			return false;
		n++;
		double d = x-mean;
		mean+=d/n;
		M2+=d*(x-mean);
		return true;
	}

	public double getMean() {
		return mean;
	}

	public double getVariance() {
		return M2/(n-1);
	}

	public double getStandardDeviation() {
		return Math.sqrt(getVariance());
	}

	public int getCount() {
		return n;
	}


}
