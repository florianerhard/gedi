package gedi.util.algorithm.string.alignment.pairwise.gapCostFunctions;

import java.util.Locale;


public class AffineGapCostFunction implements GapCostFunction {

	private float gapOpen;
	private float gapExtend;
	
	
	public AffineGapCostFunction(float gapOpen, float gapExtend) {
		this.gapOpen = gapOpen;
		this.gapExtend = gapExtend;
	}

	
	public float getGapOpen() {
		return gapOpen;
	}
	
	public float getGapExtend() {
		return gapExtend;
	}

	@Override
	public float getGapCost(int gap) {
		return gapOpen+gap*gapExtend;
	}
	
	
	@Override
	public String toString() {
		return String.format(Locale.US,"g(n) = %f + n * %f",gapOpen,gapExtend);
	}
}
