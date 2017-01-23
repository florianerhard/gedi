package gedi.util.math.stat.testing;

import java.util.Locale;

import org.apache.commons.math3.distribution.NormalDistribution;


/**
 * H1.less_than means: ranks of positive less than negative
 * @author erhard
 *
 */
public class ExtendedMannWhitneyTest {

	private double R1;
	private int n1;
	private int n2;
	private long tieCorrection;
	private int currentRank = 1;
	
	public void clear() {
		R1 = 0;
		n1 = 0;
		n2 = 0;
		currentRank = 1;
		tieCorrection = 0;
	}
	
	public void add(boolean pos) {
		if (pos)
			add(1,0);
		else
			add(0,1);
	}
	
	public void add(int pos, int neg) {
		n1+=pos;
		n2+=neg;
		int num = pos+neg;
		double rank = currentRank;
		if (num>1)
			rank = currentRank+(num-1)/2.0;
			
		R1+=rank*pos;
		if (num>1)
			tieCorrection+=num*(num+1)*(num-1);
		
		currentRank+=num;
	}
	
	public boolean isValid() {
		return n1>=2 && n2>=2;
	}
	
	public boolean isValidApproximation() {
		return isValid() && (n1>25 || n2>25);
	}
	
	public MannWhitneyTestStatistic getTestStatistic() {
		return getTestStatistic(false);
	}
	
	public MannWhitneyTestStatistic getTestStatistic(boolean ignoreWarning) {
		double u1 = R1-(n1*(n1+1)/2.0);
		double r2 = (n1+n2)*(n1+n2+1)/2-R1;
		if (!isValidApproximation() && !ignoreWarning)
			throw new IllegalArgumentException("Cannot use approximation!");
		return new MannWhitneyTestStatistic(R1,r2,u1, n1*n2-u1, n1, n2, tieCorrection);
	}
	
	public static class MannWhitneyTestStatistic {
		private double R1;
		private double R2;
		private double U1;
		private double U2;
		private int n1;
		private int n2;
		private long tieCorrection;
		private NormalDistribution normal = new NormalDistribution();
		
		private MannWhitneyTestStatistic(double r1, double r2,double u1, double u2, int n1,
				int n2, long tieCorrection) {
			this.R1 = r1;
			this.R2 = r2;
			U1 = u1;
			U2 = u2;
			this.n1 = n1;
			this.n2 = n2;
			this.tieCorrection = tieCorrection;
		}
		
		public double getR1() {
			return R1;
		}
		
		public double getR2() {
			return R2;
		}
		
		public double getU(H1 h1) {
			if (h1==H1.GREATER_THAN)
				return U2;
			else if (h1==H1.LESS_THAN)
				return U1;
			else
				return Math.min(U1, U2);
		}
		
		/**
		 * H1.less_than means: ranks of positive less than negative
		 * @param h1
		 * @return
		 */
		public double getPValue(H1 h1) {
			double d = getZ(h1);
	        if(h1 == H1.NOT_EQUAL)
	            return d<0?2*normal.cumulativeProbability(d):2-2*normal.cumulativeProbability(d);
	        else
	            return normal.cumulativeProbability(d);
		}

		public double getZ(H1 h1)
	    {
	        double d = n1+n2;
	        double d1 = (d * d * d - d - (double)tieCorrection) / 12D;
	        double d2 = getU(h1) - 0.5D * n1*n2;
	        double d3 = (d2 - (d2 >= 0.0D ? 0.5D : -0.5D)) / Math.sqrt((n1*n2 / (d * (d - 1.0D))) * d1);
	        return d3;
	    }
		
		public long getTieCorrection() {
			return tieCorrection;
		}
		
		public String toString(H1 h1) {
			return String.format(Locale.US,"%s: U=%g z=%.4f p=%g R1=%g R2=%g n1=%d n2=%d t=%d", h1, getU(h1), getZ(h1), getPValue(h1), getR1(), getR2(), n1, n2,getTieCorrection());
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(toString(H1.NOT_EQUAL));
			sb.append("\n");
			sb.append(toString(H1.LESS_THAN));
			sb.append("\n");
			sb.append(toString(H1.GREATER_THAN));
			return sb.toString();
		}
	}
	
}
