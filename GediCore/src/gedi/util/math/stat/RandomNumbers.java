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
package gedi.util.math.stat;

import gedi.util.ArrayUtils;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;

import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

import cern.colt.bitvector.BitVector;
import cern.jet.random.Binomial;
import cern.jet.random.ChiSquare;
import cern.jet.random.Exponential;
import cern.jet.random.Gamma;
import cern.jet.random.NegativeBinomial;
import cern.jet.random.Normal;
import cern.jet.random.Poisson;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;

/**
 * 
 * Manages a random number generators for various distributions. By only using this class for every
 * random number drawing in the whole framework, some advantages emerge:
 * <ul><li>it is guaranteed that the most quickest possible generator is used for each drawing</li>
 * <li>one can derandomize the algorithms by using {@link RandomNumbers#setSeed(Date)}</li>
 * <li>the number of random number generations can easily be counted</li></ul>
 * 
 * @author Florian Erhard
 *
 */
public class RandomNumbers {

	
	private Date seed = null;
	private Uniform unif = null;
	private Exponential exponential = null;
	private Binomial binom = null;
	private Poisson poisson = null;
	private Normal normal = null;
	private ChiSquare chisq = null;
	private NegativeBinomial negBinom = null;
	private Gamma gamma = null;
	private int unifCount = 0;
	private int exponentialCount = 0;
	private int binomCount = 0;
	private int poissonCount = 0;
	private int gammaCount = 0;
	private int normalCount = 0;
	private int geometricCount = 0;
	
	
	private boolean countGenerations = false;
	
	
	public RandomNumbers(long seed) {
		setSeed(seed);
	}
	
	public RandomNumbers() {
		resetSeed();
	}
	
	/**
	 * Sets the seed to the current date
	 */
	public void resetSeed() {
		setSeed(new Date());
	}
	
	/**
	 * Gets the actual seed of the random number generator.
	 * 
	 * @return seed
	 */
	public long getSeed() {
		return seed.getTime();
	}
	
	/** 
	 * Sets a new seed for the random number generator.
	 * 
	 * @param seed the seed
	 */
	public void setSeed(long seed) {
		setSeed(new Date(seed));
	}
	
	/** 
	 * Sets a new seed for the random number generator.
	 * 
	 * @param seed the seed
	 */
	public void setSeed(Date seed) {
		MersenneTwister mt = new MersenneTwister(seed);
		unif = new Uniform(mt);
		exponential = new Exponential(0, mt);
		binom = new Binomial(10,0.5,mt);
		poisson = new Poisson(1,mt);
		normal = new Normal(0,1,mt);
		chisq = new ChiSquare(1, mt);
		negBinom = new NegativeBinomial(1,0,mt);
		gamma = new Gamma(1,1,mt);
		this.seed = seed;
	}
	
	/**
	 * First is prob of first! Last must be 1. (As it can be caluculated by {@link ArrayUtils#cumSumInPlace(double[], int)}
	 * 
	 * Update: last must not be 1, will be normalized automatically.
	 * @param cumulativeProbs
	 * @return
	 */
	public int getCategorial(double[] cumulativeProbs) {
		double r = getUnif()*cumulativeProbs[cumulativeProbs.length-1];
		
		for (int i=0; i<cumulativeProbs.length; i++) {
			if (cumulativeProbs[i]>=r)
				return i;
		};
		return -1;
	}
	
	/**
	 * First is prob of first! Last must be 1. (As it can be caluculated by {@link ArrayUtils#cumSumInPlace(double[], int)}
	 * 
	 * Update: last must not be 1, will be normalized automatically.
	 * @param cumulativeProbs
	 * @return
	 */
	public int getCategorial(DoubleArrayList cumulativeProbs) {
		double r = getUnif()*cumulativeProbs.getLastDouble();
		
		for (int i=0; i<cumulativeProbs.size(); i++) {
			if (cumulativeProbs.getDouble(i)>=r)
				return i;
		};
		return -1;
	}
	
	
	/**
	 * Gets whether or not to count the number of random number generations
	 * 
	 * @return whether or not to count
	 */
	public boolean isCountGenerations() {
		return countGenerations;
	}


	/**
	 * Sets whether or not to count the number of random number generations
	 * 
	 * @param countGenerations whether or not to count
	 */
	public void setCountGenerations(boolean countGenerations) {
		this.countGenerations = countGenerations;
	}


	/**
	 * Gets the number of random number generations for each distribution (same order as in
	 * {@link RandomNumbers#getNames()} since the last call of <code>resetCounts</code>.
	 * 
	 * @return number of random number generations
	 */
	public int[] getCounts() {
		return new int[] {unifCount, binomCount,exponentialCount,normalCount,poissonCount, geometricCount, gammaCount};
	}
	
	/**
	 * Gets the names of the built-in distributions.
	 * 
	 * @return names of the distributions
	 */
	public String[] getNames() {
		return new String[] {"Uniform", "Binomial","Exponential","Normal","Poisson","Geometric","Gamma"};
	}
	
	/**
	 * Resets the counts for each distribution to zero.
	 */
	public void resetCounts() {
		unifCount = binomCount = exponentialCount = normalCount = poissonCount = gammaCount = 0;
	}
	
	/**
	 * Gets a random number from the uniform distribution between 0 and 1.
	 * 
	 * @return random number between 0 and 1
	 * 
	 * @see Uniform#nextDouble()
	 */
	public double getUnif() {
		if (countGenerations) unifCount++;
		return unif.nextDouble();
	}
	
	/**
	 * Gets n random numbers from the uniform distribution between 0 and 1.
	 * 
	 * @return random number between 0 and 1
	 * 
	 * @see Uniform#nextDouble()
	 */
	public ExtendedIterator<Double> getUnif(int count) {
		if (countGenerations) unifCount+=count;
		return EI.repeat(count,()->unif.nextDouble());
	}

	/**
	 * Gets a random number from the exponential distribution.
	 * 
	 * @param d	mean 
	 * @return 	random number
	 * 
	 *  @see Exponential#nextDouble()
	 */
	public double getExponential(double d) {
		if (Double.isInfinite(d)) return d;
		if (countGenerations) exponentialCount++;
		
		return exponential.nextDouble(d);
	}
	
	/**
	 * Gets n random numbers from the exponential distribution.
	 * 
	 * @param d	mean 
	 * @return 	random number
	 * 
	 *  @see Exponential#nextDouble()
	 */
	public ExtendedIterator<Double> getExponential(int count, double d) {
		if (Double.isInfinite(d)) return EI.repeat(count,d);
		if (countGenerations) exponentialCount+=count;
		
		return EI.repeat(count,()->exponential.nextDouble(d));
	}
	
	/**
	 * Gets a random number from the geometric distribution.
	 * 
	 * @param p probability
	 * @return 	random number
	 * 
	 *  @see Exponential#nextDouble()
	 */
	public int getGeometrical(double p) {
		if (countGenerations) geometricCount++;
		
		return negBinom.nextInt(1, p);
	}
	
	/**
	 * Gets n random numbers from the geometric distribution.
	 * 
	 * @param p probability
	 * @return 	random number
	 * 
	 *  @see Exponential#nextDouble()
	 */
	public ExtendedIterator<Integer> getGeometrical(int count, double p) {
		if (countGenerations) geometricCount+=count;
		
		return EI.repeat(count,()->negBinom.nextInt(1, p));
	}

	/**
	 * Gets a random number from the binomial distribution.
	 * 
	 * @param i		size
	 * @param prob	probability
	 * @return		random number
	 * 
	 * @see Binomial#nextInt(int, double)
	 */
	public int getBinom(int i, double prob) {
		if (prob==0 || i==0)
			return 0;
		else if (prob==1)
			return i;
		else {
			if (countGenerations) binomCount++;
			return binom.nextInt(i, prob);
		}
	}

	/**
	 * Gets n a random numbers from the binomial distribution.
	 * 
	 * @param i		size
	 * @param prob	probability
	 * @return		random number
	 * 
	 * @see Binomial#nextInt(int, double)
	 */
	public ExtendedIterator<Integer> getBinom(int count, int i, double prob) {
		if (prob==0 || i==0)
			return EI.repeat(count, 0);
		else if (prob==1)
			return EI.repeat(count, i);
		else{
			if (countGenerations) binomCount+=count;
			return EI.repeat(count,()->binom.nextInt(i, prob));
		}
			
	}

	
	/**
	 * Gets a uniformly chosen random number between <code>min</code> (inclusive) and <code>max</code> (exclusive)
	 *  
	 * @param min 	inclusive minimum
	 * @param max 	exclusive maximum
	 * @return 		uniformly chosen random number
	 * 
	 * @see Uniform#nextIntFromTo(int, int)
	 */
	public int getUnif(int min, int max) {
		if (min+1==max) return min;
		if (countGenerations) unifCount++;
		return unif.nextIntFromTo(min, max-1);
	}
	
	public long getUnif(long min, long max) {
		if (min+1==max) return min;
		if (countGenerations) unifCount++;
		return unif.nextLongFromTo(min, max-1);
	}
	
	
	/**
	 * Gets a uniformly chosen random number between <code>min</code> (inclusive) and <code>max</code> (exclusive)
	 *  
	 * @param min 	inclusive minimum
	 * @param max 	exclusive maximum
	 * @return 		uniformly chosen random number
	 * 
	 * @see Uniform#nextIntFromTo(int, int)
	 */
	public ExtendedIterator<Integer> getUnif(int count, int min, int max) {
		if (min+1==max) return EI.repeat(count, min);;
		if (countGenerations) unifCount+=count;
		return EI.repeat(count,()->unif.nextIntFromTo(min, max-1));
	}
	
	public ExtendedIterator<Long> getUnif(int count, long min, long max) {
		if (min+1==max) return EI.repeat(count, min);;
		if (countGenerations) unifCount+=count;
		return EI.repeat(count,()->unif.nextLongFromTo(min, max-1));
	}
	
	/**
	 * Gets a random permutation of the numbers [0,n)
	 * @param n
	 * @return
	 */
	public int[] getPermutation(int n) {
		int[] a = new int[n];
		for (int i=0; i<n; i++)
			a[i] = i;
		for (int k=0; k<n-1; k++) {
			int m = getUnif(k,n);
			int tmp = a[k];
			a[k] = a[m];
			a[m] = tmp;
		}
		return a;
	}


	/**
	 * Gets a random number from the Poisson distribution.
	 * 
	 * @param mean 	mean
	 * @return		random number
	 * 
	 * @see Poisson#nextInt(double)
	 */
	public int getPoisson(double mean) {
		if (mean==0) return 0;
		if (countGenerations) poissonCount++;
		return poisson.nextInt(mean);
	}
	
	/**
	 * Gets n random numbers from the Poisson distribution.
	 * 
	 * @param mean 	mean
	 * @return		random number
	 * 
	 * @see Poisson#nextInt(double)
	 */
	public ExtendedIterator<Integer> getPoisson(int count, double mean) {
		if (mean==0) return EI.repeat(count, 0);
		if (countGenerations) poissonCount++;
		return EI.repeat(count,()->poisson.nextInt(mean));
	}
	
	public double getChisq(int df) {
		return chisq.nextDouble(df);
	}
	
	public ExtendedIterator<Double> getChisq(int count, int df) {
		return EI.repeat(count,()->chisq.nextDouble(df));
	}
	
	public double getGamma(double shape, double scale) {
		return gamma.nextDouble(shape, scale);
	}
	
	public ExtendedIterator<Double> getGamma(int count, double shape, double scale) {
		return EI.repeat(count,()->gamma.nextDouble(shape, scale));
	}
	
	public double[] getDirichlet(double[] alpha) {
		return getDirichlet(alpha,null);
	}
	public double[] getDirichlet(double[] alpha, double[] re) {
		if (re==null || re.length!=alpha.length) re = new double[alpha.length];
		double sum = 0;
		for (int i=0; i<re.length; i++) 
			sum+= re[i] = getGamma(alpha[i], 1);
		for (int i=0; i<re.length; i++)
			re[i]/=sum;
		return re;
	}
	
	
	public ExtendedIterator<double[]> getDirichlet(int count, double[] alpha) {
		return getDirichlet(count,alpha,null);
	}
	public ExtendedIterator<double[]> getDirichlet(int count, double[] alpha, double[] re) {
		return EI.repeat(count, ()->getDirichlet(alpha,re));
	}
	
	
	public double getBeta(double alpha, double beta) {
		double a = getGamma(alpha, 1);
		double b = getGamma(beta, 1);
		return a/(a+b);
	}
	
	public ExtendedIterator<Double> getBeta(int count, double alpha, double beta) {
		return EI.repeat(count, ()->getBeta(alpha,beta));
	}
	
	public double[] getDirichletUniform(double[] re) {
		double sum = 0;
		for (int i=0; i<re.length; i++) 
			sum+= re[i] = getGamma(1, 1);
		for (int i=0; i<re.length; i++)
			re[i]/=sum;
		return re;
	}
	
	public ExtendedIterator<double[]> getDirichletUniform(int count, double[] re) {
		return EI.repeat(count, ()->getDirichletUniform(re));
	}
	
	/**
	 * Gets a random number from the Normal distribution
	 * 
	 * @return	random number
	 * 
	 * @see Normal#nextDouble()
	 */
	public double getNormal() {
		if (countGenerations) normalCount++;
		return normal.nextDouble();
	}
	
	/**
	 * Gets a random number from the Normal distribution
	 * 
	 * @return	random number
	 * 
	 * @see Normal#nextDouble()
	 */
	public ExtendedIterator<Double> getNormal(int count) {
		if (countGenerations) normalCount+=count;
		return EI.repeat(count, ()->normal.nextDouble());
	}
	
	/**
	 * Gets a random number from the Normal distribution with given mean and stddev.
	 * 
	 * @param mean		the mean	
	 * @param stddev	the stddev
	 * @return			random number
	 * 
	 * @see Normal#nextDouble(double, double)
	 */
	public double getNormal(double mean, double stddev) {
		if (countGenerations) normalCount++;
		return normal.nextDouble(mean,stddev);
	}
	
	public ExtendedIterator<Double> getNormal(int count, double mean, double stddev) {
		if (countGenerations) normalCount+=count;
		return EI.repeat(count, ()->normal.nextDouble(mean,stddev));
	}

	public boolean getBool() {
		return unif.nextBoolean();
	}
	
	public ExtendedIterator<Boolean> getBool(int count) {
		return EI.repeat(count, ()->unif.nextBoolean());
	}
	
	/**
	 * 
	 * @param prob probability for true
	 * @return
	 */
	public boolean getBool(double prob) {
		return unif.nextDouble()<prob;
	}
	
	public ExtendedIterator<Boolean> getBool(int count,double prob) {
		return EI.repeat(count, ()->unif.nextDouble()<prob);
	}
	

	public <T> T getRandomElement(T[] ar) {
		return ar[getUnif(0, ar.length)];
	}
	
	public <T> ExtendedIterator<T> getRandomElement(int count, T[] ar) {
		return EI.repeat(count, ()->ar[getUnif(0, ar.length)]);
	}

	/**
	 * Gets a random index in bv that is set to true
	 * @param bv
	 * @return
	 */
	public int getIndex(BitVector bv) {
		int card = bv.cardinality();
		
		if (card==0) return -1;
		
		if (card>20) {
			int re = getUnif(0, bv.size());
			while (!bv.getQuick(re))
				re = getUnif(0, bv.size());
			return re;
		} else {
			int ind = getUnif(0, card);
			for (int i=0; i<bv.size(); i++) {
				if (bv.getQuick(i))
					if (ind--==0) return i;
			}
			return -1;
		}
	}

	/**
	 * Inplace!
	 * @param a
	 * @return
	 */
	public <T> T[] shuffle(T[] a) {
		for (int k=0; k<a.length-1; k++) {
			int m =getUnif(k, a.length);
			T tmp = a[k];
			a[k] = a[m];
			a[m] = tmp;
		}
		return a;
	}
	
	public <T> T[] shuffle(T[] a, int s, int e) {
		for (int k=s; k<e-1; k++) {
			int m =getUnif(k, e);
			T tmp = a[k];
			a[k] = a[m];
			a[m] = tmp;
		}
		return a;
	}
	
	public <T> List<T> shuffle(List<T> c) {
		int size = c.size();
        if (size < 5 || c instanceof RandomAccess) {
            for (int i=size; i>1; i--){
                 c.set(i-1, c.set(getUnif(0,i), c.get(i-1)));
            }
        } else {
            Object arr[] = c.toArray();

            // Shuffle array
            for (int i=size; i>1; i--) {
            	int j = getUnif(0,i);
            	Object tmp = arr[i-1];
                arr[i-1] = arr[j];
                arr[j] = tmp;
            }
            // Dump array back into list
            // instead of using a raw type here, it's possible to capture
            // the wildcard but it will require a call to a supplementary
            // private method
            ListIterator it = c.listIterator();
            for (int i=0; i<arr.length; i++) {
                it.next();
                it.set(arr[i]);
            }
        }
        return c;
	}
	
	public String shuffle(String a) {
		return new String(shuffle(a.toCharArray()));
	}
	
	public char[] shuffle(char[] a) {
		for (int k=0; k<a.length-1; k++) {
			int m =getUnif(k, a.length);
			char tmp = a[k];
			a[k] = a[m];
			a[m] = tmp;
		}
		return a;
	}
	public int[] shuffle(int[] a) {
		for (int k=0; k<a.length-1; k++) {
			int m =getUnif(k, a.length);
			int tmp = a[k];
			a[k] = a[m];
			a[m] = tmp;
		}
		return a;
	}
	
	public double[] shuffle(double[] a) {
		for (int k=0; k<a.length-1; k++) {
			int m =getUnif(k, a.length);
			double tmp = a[k];
			a[k] = a[m];
			a[m] = tmp;
		}
		return a;
	}
	

	
	private static RandomNumbers global = new  RandomNumbers();
	public static RandomNumbers getGlobal() {
		return global;
	}
	
	
}
