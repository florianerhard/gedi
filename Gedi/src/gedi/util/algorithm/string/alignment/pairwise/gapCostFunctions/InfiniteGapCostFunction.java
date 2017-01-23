package gedi.util.algorithm.string.alignment.pairwise.gapCostFunctions;


public class InfiniteGapCostFunction implements GapCostFunction {

	
	@Override
	public float getGapCost(int gap) {
		return Float.POSITIVE_INFINITY;
	}
	
	
	@Override
	public String toString() {
		return "g(n) = Inf";
	}
}
