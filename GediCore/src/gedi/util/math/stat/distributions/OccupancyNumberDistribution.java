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
package gedi.util.math.stat.distributions;


import static java.lang.Math.*;
import static org.apache.commons.math3.special.Gamma.logGamma;
import static org.apache.commons.math3.util.CombinatoricsUtils.binomialCoefficientLog;
import static org.apache.commons.math3.util.CombinatoricsUtils.factorialLog;
import static org.apache.commons.math3.util.ArithmeticUtils.gcd;

import java.math.BigInteger;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.fraction.BigFraction;

import gedi.util.ArrayUtils;


/**
 * What is the probability to observe a entries with count b in a histogram of k bins with total sum n
 * @author flo
 *
 */
public class OccupancyNumberDistribution {

	private int b,k,n;

	private double[] aToProb = null;
	private double[] aToProxProb = null;
	private NormalDistribution normal;
	private int useApproxForM = 20;

	public OccupancyNumberDistribution(int b, int k, int n) {
		this.b = b;
		this.k = k;
		this.n = n;
	}

	public OccupancyNumberDistribution(int b, int k, int n, int useApproxForM) {
		this.b = b;
		this.k = k;
		this.n = n;
		this.useApproxForM = useApproxForM;
	}

	
	public double expectation() {
		return expectation(b, k, n);
	}
	
	public double variance() {
		return variance(b,k,n);
	}
	
	
	public static double expectation(int b, int k, int n) {
		return exp(log(k)+binomialCoefficientLog(n, b)-b*log(k)+(n-b)*log(1-1.0/k));
	}
	
	public static double variance(int b, int k, int n) {
		double exp = expectation(b,k,n);
		int oc = Math.max(0,n-2*b);
		return exp(log(k)+log(k-1)+factorialLog(n)-2*factorialLog(b)-factorialLog(oc)+2*b*log(1.0/k)+(oc)*log(1-2.0/k))+(1-exp)*exp;
	}
	
	public static double covariance(int b1, int b2, int k, int n) {
		double exp1 = expectation(b1,k,n);
		double exp2 = expectation(b2,k,n);
		int oc = Math.max(0,n-b1-b2);
		return exp(log(k)+log(k-1)+factorialLog(n)-factorialLog(b1)-factorialLog(b2)-factorialLog(oc)+(b1+b2)*log(1.0/k)+(oc)*log(1-2.0/k))-exp1*exp2;
	}
	
	

	public int ml() {
		computeIfNeccessary();
		return ArrayUtils.argmax(aToProb);
	}


	public int getMaximalA() {
		return b==0?k:Math.min(k,n/b);
	}

	public double getZ(int a) {
		return (a-expectation())/sqrt(variance());
	}


	public double approximateProbability(int a) {
		if (aToProxProb==null)
			aToProxProb = computeApproximateNormal();
		return a>=0&&a<aToProxProb.length?aToProxProb[a]:0;
	}


	public double probability(int a) {
		computeIfNeccessary();
		return a>=0&&a<aToProb.length?aToProb[a]:0;
	}
	
	public double cumulativeProbability(int a) {
		computeIfNeccessary();
		double re = 0;
		a = min(a,aToProb.length);
		for (int i=0; i<=a; i++)
			re+=aToProb[i];
		return re;
	}


	private void computeIfNeccessary() {
		if (aToProb==null) {
			if (getMaximalA()>useApproxForM)
				aToProxProb = this.aToProb = computeApproximateNormal();
			else
				computeRational();
		}
	}
	
	private double[] computeApproximateNormal() {
		int maxA = getMaximalA();
		double[] re = new double[maxA+1];
		if (normal==null)
			normal = new NormalDistribution(expectation(),sqrt(variance()));
		for (int a=0; a<=maxA; a++) {
			re[a] = normal.density(a);
		}
		ArrayUtils.normalize(re);
		return re;
	}
	
	private void computeDouble() {
		int maxA = getMaximalA();
		aToProb = new double[maxA+1];
		
		// 1. compute v(a,b,k,n) i.e. the prob to get b at least a times at fixed positions
		for (int a=0; a<=maxA; a++) {
			aToProb[a] = exp(logv(a,b,k,n)+binomialCoefficientLog(k, a));
		}
			
		
		// 2. compute vd(a,b,k,n) i.e. the prob to get b exactly a times at fixed positions
		// 3. compute vs(a,b,k,n) i.e. the prob to get b exactly a times at any positions
		for (int a=maxA-1; a>=0; a--) {
			for (int i=1; i<k-a+1 && a+i<aToProb.length; i++) {
				if (aToProb[a+i]>0) {
					aToProb[a] -= exp(Math.log(aToProb[a+i])+binomialCoefficientLog(a+i, i));
				}
			}
			
			if (aToProb[a]<0 || Double.isNaN(aToProb[a])) aToProb[a] = 0;
		}
		
	}
	
	private void computeRational() {
		int maxA = b==0?k:Math.min(k,n/b);
		aToProb = new double[maxA+1];

		BigFraction[] aToProb = new BigFraction[maxA+1];
		
		BigInteger bfac = factorial(b);
		
		long start = System.currentTimeMillis();
		double maxDiff = 0;
		
		aToProb[maxA] = BigFraction.ONE;
		for (int a=maxA-1; a>=0; a--) {
			int m = Math.min(k-a+1,aToProb.length-a);
			aToProb[a] = BigFraction.ZERO;
			for (int i=1; i<m; i++) {
				BigInteger rat = binomialCoefficientLargeInteger(k-a, i).multiply(factorial(n-a*b, i*b));
				if (n-a*b-i*b>0) rat=rat.multiply(BigInteger.valueOf(k-a-i).pow(n-a*b-i*b));
				if (m-i>0) rat = rat.multiply(bfac.pow(m-i));
				aToProb[a] = aToProb[a].add(new BigFraction(rat, BigInteger.ONE).multiply(aToProb[a+i]));
			}
			
			BigInteger rat = bfac.pow(m).multiply(BigInteger.valueOf(k-a).pow(n-a*b));

			aToProb[a] = BigFraction.ONE.subtract(aToProb[a].multiply(new BigFraction(BigInteger.ONE,rat)));
			this.aToProb[a] = new BigFraction(binomialCoefficientLargeInteger(k, a),BigInteger.ONE).multiply(aToProb[a].multiply(rationalv(a, b, k, n))).doubleValue();

			maxDiff = max(maxDiff,abs(this.aToProb[a]-approximateProbability(a)));
			if (System.currentTimeMillis()-start>500) {
				aToProxProb = this.aToProb = computeApproximateNormal();
				return;
			}
		}
//		System.out.printf(Locale.US,"%d\t%d\t%d\t%d\t%.4g\t%.4f\n",b,k,n,maxDigit,maxDiff,(System.currentTimeMillis()-start)/1000.0);
	}
	
	private static BigInteger binomialCoefficientLargeInteger(final int n, final int k) {
		if ((n == k) || (k == 0)) {
			return BigInteger.valueOf(1);
		}
		if ((k == 1) || (k == n - 1)) {
			return BigInteger.valueOf(n);
		}
		if (k > n/2) {
			return binomialCoefficientLargeInteger(n, n - k);
		}

		BigInteger result = BigInteger.valueOf(1);
		int i = n - k + 1;
        for (int j = 1; j <= k; j++) {
            final int d = gcd(i, j);
            result = result.divide(BigInteger.valueOf(j/d)).multiply(BigInteger.valueOf(i/d));
            i++;
        }
		return result;
	}
	
	private static final BigInteger factorial(int b) {
		if (b==0) return BigInteger.ONE;
		BigInteger re = BigInteger.valueOf(b);
		for (int i=b-1; i>1; i--)
			re = re.multiply(BigInteger.valueOf(i));
		return re;
	}
	
	private static final BigInteger factorial(int b, int k) {
		if (b==0||k==0) return BigInteger.ONE;
		BigInteger re = BigInteger.valueOf(b);
		for (int i=b-1; i>b-k; i--)
			re = re.multiply(BigInteger.valueOf(i));
		return re;
	}
	
	private static final BigFraction rationalv(int a,int b,int k,int n) {
		BigFraction re = BigFraction.ONE;
		for (int i=n; i>n-a*b; i--)
			re = re.multiply(new BigFraction(i, 1));
		BigFraction ba= new BigFraction(1, 1);
		for (int i=1; i<=b; i++)
			ba = ba.multiply(new BigFraction(i, 1));
		re = re.divide(a==0?BigFraction.ONE:ba.pow(a));

		
		BigFraction f1 = a*b==0?BigFraction.ONE:new BigFraction(1,k).pow(a*b);
		BigFraction f2 = n-a*b==0?BigFraction.ONE:new BigFraction(k-a,k).pow(n-a*b);

		return re.multiply(f1).multiply(f2);
	}

	private static final double logv(int a,int b,double k,int n) {
		return logGamma(n+1)-logGamma(n-a*b+1)-a*logGamma(b+1)+(n-a*b)*log(k-a)-n*log(k);
	}


}
