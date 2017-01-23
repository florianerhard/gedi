package gedi.util.math.stat.testing.multipleTesting;


public class BonferroniCorrection extends AbstractMultipleTestingCorrection {

	@Override
	protected void correct(double[] pvals, int length, int additionalInsignificant) {
		int total = length+additionalInsignificant;
		for (int i=0; i<length; i++) 
			pvals[i] = Math.min(1, total*pvals[i]);
	}
	
}
