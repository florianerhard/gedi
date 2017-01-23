package gedi.util.math.stat.testing.multipleTesting;

public interface MultipleTestingCorrectionMethod {

	public int addPvalue(double pvalue, CorrectPvalueCallback callback);
	public int addInsignificant();
	
	/**
	 * Corrects all previously added pvalues and clears this correction of them!
	 */
	public void correct();
	
	
}
