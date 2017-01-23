package gedi.util.math.stat.kernel;

import org.apache.commons.math3.distribution.RealDistribution;

public class RealDistributionKernel implements Kernel {

	protected RealDistribution dist;
	protected double maxMassOutside;

	protected double mean;
	protected double halfSize;

	/**
	 * centered around mean!
	 * @param dist
	 * @param maxMassOutside
	 */
	public RealDistributionKernel(RealDistribution dist, double maxMassOutside) {
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
		return dist.density(operand-mean);
	}
	
	@Override
	public double halfSize() {
		return halfSize;
	}
	
	
}
