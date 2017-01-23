package gedi.util.math.stat.testing.multipleTesting;


public class BenjaminiHochbergCorrection extends AbstractMultipleTestingCorrection {

	@Override
	protected void correct(double[] pvals, int length, int additionalInsignificant) {
		double oldPv = Double.NaN;
		int oldRank = -1;
		int total = length+additionalInsignificant;
		for (int i=length-1; i>=0; i--) {
			double p = pvals[i];
			if (p==oldPv)
				pvals[i]=pvals[oldRank];
			else {
				oldRank = i;
				oldPv = p;
				pvals[i] = Math.min(1, p*total/(i+1));
			}
		}
	}
	
}
