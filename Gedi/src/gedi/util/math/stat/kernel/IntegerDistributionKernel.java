package gedi.util.math.stat.kernel;

import org.apache.commons.math3.distribution.IntegerDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;

public class IntegerDistributionKernel implements Kernel {

	protected IntegerDistribution dist;
	protected double maxMassOutside;
	
	protected double mean;
	protected double halfSize;

	/**
	 * centered around mean!
	 * @param dist
	 * @param maxMassOutside
	 */
	public IntegerDistributionKernel(IntegerDistribution dist, double maxMassOutside) {
		this.dist = dist;
		this.maxMassOutside = maxMassOutside;
		updateDistribution();
	}

	protected void updateDistribution() {
		mean = dist.getNumericalMean();
		double left = dist.inverseCumulativeProbability(maxMassOutside/2);
		double right = dist.inverseCumulativeProbability(1-maxMassOutside/2);
		halfSize = Math.max(mean-left, right-mean);
	}

	@Override
	public double applyAsDouble(double operand) {
		return dist.probability((int)(operand-mean));
	}
	
	@Override
	public double halfSize() {
		return halfSize;
	}
	
	
}
