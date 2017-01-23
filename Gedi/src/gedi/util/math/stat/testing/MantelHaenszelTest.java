package gedi.util.math.stat.testing;

import java.util.Locale;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;


public class MantelHaenszelTest {


	private ChiSquaredDistribution chisq = new ChiSquaredDistribution(1);
	private NormalDistribution norm = new NormalDistribution();
	private boolean correct = true;
	private double confLevel = 0.95;
	private H1 h1 = H1.NOT_EQUAL;
	
	private double pval;
	private double stat;
	private double lowerConf;
	private double upperConf;
	private double estimate;

	private TwoByTwoByKTable table;
	
	
	public TwoByTwoByKTable getTable() {
		
		return table;
	}
	
	public H1 getH1() {
		return h1;
	}
	
	public void setH1(H1 h1) {
		this.h1 = h1;
	}
	
	public void setConfLevel(double confLevel) {
		this.confLevel = confLevel;
	}
	
	public double getConfLevel() {
		return confLevel;
	}
	
	public double getPval() {
		return pval;
	}
	
	public double getStat() {
		return stat;
	}
	
	public void setCorrection(boolean correct) {
		this.correct = correct;
	}
	
	public boolean isCorrection() {
		return correct;
	}
	
	public double getLowerConf() {
		return lowerConf;
	}
	
	public double getUpperConf() {
		return upperConf;
	}
	
	public double getEstimate() {
		return estimate;
	}
	
	/**
	 * Returns this!
	 * @param table
	 * @return
	 * @throws MathException
	 */
	public MantelHaenszelTest compute(TwoByTwoByKTable table) {
		this.table = table;

		double delta = table.getDelta();
		double yates = correct && (delta >= 0.5)? 0.5: 0;
		double deltayates = delta-yates;
		stat =  deltayates*deltayates/table.getVarianceEstimate();
		
		if (h1==H1.NOT_EQUAL) 
			pval = stat>30?0:(1-chisq.cumulativeProbability(stat));
		else if (h1==H1.LESS_THAN)
			pval = norm.cumulativeProbability(Math.signum(delta)*Math.sqrt(stat));
		else
			pval = 1-norm.cumulativeProbability(Math.signum(delta)*Math.sqrt(stat));

		estimate = table.getOddsRatio();
		double sd =table.computeSD();

		if (h1==H1.LESS_THAN){
			lowerConf = 0;
			upperConf = estimate*Math.exp(norm.inverseCumulativeProbability(confLevel)*sd);
		} else if (h1==H1.GREATER_THAN) {
			lowerConf = estimate*Math.exp(norm.inverseCumulativeProbability(1-confLevel)*sd);
			upperConf = Double.POSITIVE_INFINITY;
		} else {
			lowerConf = estimate*Math.exp(norm.inverseCumulativeProbability((1-confLevel)/2)*sd);
			upperConf = estimate*Math.exp(-norm.inverseCumulativeProbability((1-confLevel)/2)*sd);
		}
		return this;
	}


	@Override
	public String toString() {
		String h1text = "";
		if (h1==H1.NOT_EQUAL) h1text = "not equal to";
		else if (h1==H1.LESS_THAN) h1text = "less than";
		else h1text = "greater than";
		return String.format(Locale.US,"common odds ratio estimate: %.5f, " +
				"X-squared = %.4f, " +
				"p-value = %.5g, " +
				"H1: true common odds ratio is %s 1, " +
				"%.2f percent confidence interval: [%.5f,%.5f]",
				estimate,stat,pval,h1text,confLevel,lowerConf,upperConf);
	}

	public static class TwoByTwoByKTable {
		private int[] a,b,c,d;
		private int[] n;
		
		public TwoByTwoByKTable(int[] a, int[] b, int[] c, int[] d) {
			this.a = a;
			this.b = b;
			this.c = c;
			this.d = d;
			if (a.length!=b.length || a.length!=c.length || a.length!=d.length)
				throw new IllegalArgumentException("All four arrays must have same length!");
			
			n = new int[a.length];
			for (int i=0; i<getK(); i++) 
				n[i] = a[i]+b[i]+c[i]+d[i];
		}

		public int getK() {
			return a.length;
		}

		public double computeSD() {

			double x = 0;
			double y = 0;
			double z = 0;
			double diag =0;
			double offd = 0;

			for (int i=0; i<getK(); i++) {
				double nsq = n[i]*n[i];
				x+=(a[i]+d[i])*a[i]*d[i]/nsq;
				y+=((a[i]+d[i])*b[i]*c[i] + (b[i]+c[i])*a[i]*d[i])/nsq;
				z+=(b[i]+c[i])*b[i]*c[i]/nsq;

				diag+=a[i]*d[i]/(double)n[i];
				offd+=b[i]*c[i]/(double)n[i];
			}
			return Math.sqrt( x/(2*diag*diag) + y/(2*diag*offd) + z/(2*offd*offd));
		}

		public double getOddsRatio() {
			double diag =0;
			double offd = 0;
			for (int i=0; i<getK(); i++) {
				diag+=a[i]*d[i]/(double)n[i];
				offd+=b[i]*c[i]/(double)n[i];
			}
			return diag/offd;
		}

		public double getVarianceEstimate() {
			double re = 0;
			for (int i=0; i<getK(); i++)
				re+=(a[i]+b[i])*(a[i]+c[i])*(d[i]+b[i])*(d[i]+c[i])/(double)(n[i]*n[i]*(n[i]-1));
			return re;
		}


		public double getDelta() {
			double delta = 0;
			for (int i=0; i<getK(); i++)
				delta+=a[i]-(a[i]+b[i])*(a[i]+c[i])/(double)n[i];
			return Math.abs(delta);
		}

	}

}
