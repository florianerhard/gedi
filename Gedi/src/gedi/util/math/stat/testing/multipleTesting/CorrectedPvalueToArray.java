package gedi.util.math.stat.testing.multipleTesting;

public class CorrectedPvalueToArray implements CorrectPvalueCallback {

	private double[] a;
	private int index;
	
	public CorrectedPvalueToArray(double[] a, int index) {
		this.a = a;
		this.index = index;
	}

	@Override
	public void setCorrectedPvalue(double corrected) {
		a[index] = corrected;
	}

}
