package gedi.util.math.stat.distributions;

import gedi.util.ArrayUtils;

import java.util.Arrays;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.exception.ConvergenceException;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.exception.NotFiniteNumberException;
import org.apache.commons.math3.exception.NotPositiveException;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.exception.NumberIsTooLargeException;
import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.random.Well19937c;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.util.FastMath;

public class NormalMixtureDistribution extends AbstractRealDistribution {


	private NormalDistribution[] components;
	private double[] mixing;
	private double[] mixingSum;



	public NormalMixtureDistribution(NormalDistribution[] components,
			double[] mixing) {
		super(new Well19937c());
		this.components = components;
		this.mixing = mixing;

		if (ArrayUtils.min(mixing)<0) throw new NotPositiveException(ArrayUtils.min(mixing));
		if (components.length!=mixing.length) throw new DimensionMismatchException(mixing.length, components.length);
		double sum = ArrayUtils.sum(mixing);
		if (Double.isInfinite(sum)) throw new NotFiniteNumberException(sum);
		ArrayUtils.mult(mixing, 1/sum);

		this.mixingSum = mixing.clone();
		ArrayUtils.cumSumInPlace(mixingSum, 1);

	}

	public int getNumComponents() {
		return components.length;
	}

	public NormalDistribution getComponent(int c) {
		return components[c];
	}

	public double getMixing(int c) {
		return mixing[c];
	}

	@Override
	public double density(double x) {
		double re = 0;
		int n = getNumComponents();
		for (int i=0; i<n; i++)
			re+=mixing[i]*components[i].density(x);
		return re;
	}

	@Override
	public double cumulativeProbability(double x) {
		double re = 0;
		int n = getNumComponents();
		for (int i=0; i<n; i++)
			re+=mixing[i]*components[i].cumulativeProbability(x);
		return re;
	}

	@Override
	public double getNumericalMean() {
		double re = 0;
		int n = getNumComponents();
		for (int i=0; i<n; i++)
			re+=mixing[i]*components[i].getNumericalMean();
		return re;
	}

	@Override
	public double getNumericalVariance() {
		double re = 0;
		int n = getNumComponents();
		double mean = getNumericalMean();
		for (int i=0; i<n; i++){
			double mm = mean-components[i].getNumericalMean();
			re+=mixing[i]*(mm*mm+components[i].getNumericalVariance());
		}
		return re;
	}

	@Override
	public double getSupportLowerBound() {
		return Double.NEGATIVE_INFINITY;
	}

	@Override
	public double sample() {
		double r = random.nextDouble();

		for (int i=0; i<mixingSum.length; i++) {
			if (mixingSum[i]>=r)
				return components[i].sample();
		};

		return components[components.length-1].sample();
	}

	@Override
	public double getSupportUpperBound() {
		return Double.POSITIVE_INFINITY;
	}

	@Override
	public boolean isSupportLowerBoundInclusive() {
		return false;
	}

	@Override
	public boolean isSupportUpperBoundInclusive() {
		return false;
	}

	@Override
	public boolean isSupportConnected() {
		return true;
	}


	public double logLikelihood(double[] data) {
		int n = data.length;
		double re =0;
		for (int i = 0; i < n; i++) {
			final double rowDensity = density(data[i]);
			re += FastMath.log(rowDensity);
		}
		return re;
	}

	/**
	 * Default maximum number of iterations allowed per fitting process.
	 */
	private static final int DEFAULT_MAX_ITERATIONS = 1000;
	/**
	 * Default convergence threshold for fitting.
	 */
	private static final double DEFAULT_THRESHOLD = 1E-5;
	public static NormalMixtureDistribution fit(double[] data, int components) {
		return fit(init(data,components),data,DEFAULT_MAX_ITERATIONS,DEFAULT_THRESHOLD);
	}
	public static NormalMixtureDistribution fit(NormalMixtureDistribution initialMixture, double[] data, final int maxIterations,
			final double threshold) {

		if (maxIterations < 1) {
			throw new NotStrictlyPositiveException(maxIterations);
		}

		if (threshold < Double.MIN_VALUE) {
			throw new NotStrictlyPositiveException(threshold);
		}

		final int n = data.length;

		final int k = initialMixture.getNumComponents();
		
		if (k==1) return new NormalMixtureDistribution(new NormalDistribution[]{new NormalDistribution(new Mean().evaluate(data), new StandardDeviation().evaluate(data))}, new double[]{1});

		int numIterations = 0;
		double previousLogLikelihood = 0d;

		double logLikelihood = Double.NEGATIVE_INFINITY;

		// Initialize model to fit to initial mixture.
		NormalMixtureDistribution fittedModel = new NormalMixtureDistribution(initialMixture.components,initialMixture.mixing);

		while (numIterations++ <= maxIterations &&
				FastMath.abs(previousLogLikelihood - logLikelihood) > threshold) {
			previousLogLikelihood = logLikelihood;
			logLikelihood = 0d;


			// E-step: compute the data dependent parameters of the expectation
			// function.
			// The percentage of row's total density between a row and a
			// component
			final double[][] gamma = new double[n][k];
			// Sum of gamma for each component
			final double[] gammaSums = new double[k];

			for (int i = 0; i < n; i++) {
				final double rowDensity = fittedModel.density(data[i]);
				logLikelihood += FastMath.log(rowDensity);

				for (int j = 0; j < k; j++) {
					gamma[i][j] = fittedModel.mixing[j] * fittedModel.components[j].density(data[i]) / rowDensity;
					gammaSums[j] += gamma[i][j];
				}
			}
			logLikelihood/=n;
//			System.out.println(logLikelihood);


			// M-step: compute the new parameters based on the expectation
			// function.
			final double[] newWeights = gammaSums.clone();
			ArrayUtils.mult(newWeights, 1.0/n);

			NormalDistribution[] comp = new NormalDistribution[k];
			for (int j = 0; j < k; j++) {
				double m = 0;
				for (int i=0; i<n; i++) {
					m+=gamma[i][j]*data[i];
				}
				m/=gammaSums[j];

				double var = 0;
				for (int i=0; i<n; i++) {
					double d = m-data[i];
					var+=gamma[i][j]*d*d;
				}
				var/=gammaSums[j];

				comp[j]=new NormalDistribution(m, Math.sqrt(var));
			}

			// Update current model
			fittedModel = new NormalMixtureDistribution(comp,newWeights);
		}

		if (FastMath.abs(previousLogLikelihood - logLikelihood) > threshold) {
			// Did not converge before the maximum number of iterations
			throw new ConvergenceException();
		}

		return fittedModel;
	}

	public static NormalMixtureDistribution init(final double[] data,
			final int numComponents)
					throws NotStrictlyPositiveException,
					DimensionMismatchException {
		
		if (numComponents==1) return new NormalMixtureDistribution(new NormalDistribution[]{new NormalDistribution(new Mean().evaluate(data), new StandardDeviation().evaluate(data))}, new double[]{1});
		
		if (data.length < 2) {
			throw new NotStrictlyPositiveException(data.length);
		}
		if (numComponents < 1) {
			throw new NumberIsTooSmallException(numComponents, 2, true);
		}
		if (numComponents > data.length) {
			throw new NumberIsTooLargeException(numComponents, data.length, true);
		}

		final int numRows = data.length;
		double[] sortedData = data.clone();
		Arrays.sort(sortedData);

		// components of mixture model to be created
		double[] mixing = new double[numComponents];
		NormalDistribution[] comp = new NormalDistribution[numComponents];

		// create a component based on data in each bin
		for (int k = 0; k < numComponents; k++) {
			// minimum index (inclusive) from sorted data for this bin
			final int minIndex = (k * numRows) / numComponents;

			// maximum index (exclusive) from sorted data for this bin
			final int maxIndex = Math.min(numRows,((k + 1) * numRows) / numComponents);

			double m = new Mean().evaluate(sortedData, minIndex, maxIndex-minIndex);
			double sd = new StandardDeviation().evaluate(sortedData,minIndex,maxIndex-minIndex);
			mixing[k] = 1d / numComponents;
			comp[k] = new NormalDistribution(m,sd);
		}

		return new NormalMixtureDistribution(comp,mixing);
	}
}
