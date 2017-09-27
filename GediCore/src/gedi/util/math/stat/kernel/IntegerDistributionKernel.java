/**
 * 
 *    Copyright 2017 Florian Erhard
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */

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
