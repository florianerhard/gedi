package gedi.util.algorithm.string.alignment.pairwise.gapCostFunctions;

import java.util.Locale;


public class LinearGapCostFunction implements GapCostFunction {

	private float gap;
	
	
	public LinearGapCostFunction(float gap) {
		this.gap = gap;
	}

	
	public float getGap() {
		return gap;
	}
	
	@Override
	public float getGapCost(int gap) {
		return gap*this.gap;
	}
	
	
	@Override
	public String toString() {
		return String.format(Locale.US,"g(n) = n * %f",gap);
	}
}
