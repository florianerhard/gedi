package gedi.util.math.stat.testing.multipleTesting;

public class NoCorrection implements MultipleTestingCorrectionMethod{

	@Override
	public int addInsignificant() {
		return 0;
	}

	@Override
	public int addPvalue(double pvalue, CorrectPvalueCallback callback) {
		return 0;
	}

	@Override
	public void correct() {
	}

}
