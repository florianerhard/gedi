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

package gedi.util;

import gedi.util.datastructure.tree.redblacktree.Interval;
import gedi.util.math.stat.RandomNumbers;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

import java.util.function.DoubleUnaryOperator;

import org.apache.commons.math3.linear.RealMatrix;

public class MathUtils {
	public static final int[] logTable265 = 
    {
            0,0,1,1,2,2,2,2,3,3,3,3,3,3,3,3,
            4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,4,
            5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,
            5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,5,
            6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
            6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
            6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
            6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,6,
            7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
            7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
            7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
            7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
            7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
            7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
            7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,
            7,7,7,7,7,7,7,7,7,7,7,7,7,7,7,7
    };
	public static RandomNumbers random = new RandomNumbers();

	public static int log2(long n) {
		int re;
		for (re=0; n>0; n>>>=1)
			re++;
		return re;
	}
	
	public static int log2_256(int n) {
		return logTable265[n];
	}

	/**
	 * Computes a on top of b. 
	 * 
	 * @param b
	 * @param a
	 * @return
	 */
	public static long choose(int b, int a) {
		b = a-b>b?a-b:b;
		return decreasingFactorials(a, a-b)/faculty(a-b); 
	}
	
	public static long decreasingFactorials(int a, int n) {
		long re = 1;
		for (int i=a; i>a-n; i--)
			re*=i;
		return re;
	}
	
	public static long faculty(int a) {
		return decreasingFactorials(a, a);
	}
	
	public static String histogram2dToGnuplot(IHistogram2D histogram) {
		IHistogram1D xp = histogram.projectionX();
		IHistogram1D yp = histogram.projectionY();
		
		StringBuilder sb = new StringBuilder();
		for (int x=0; x< histogram.xAxis().bins(); x++) {
			for (int y=0; y< histogram.yAxis().bins(); y++) {
				sb.append(histogram.xAxis().binCentre(x));
				sb.append("\t");
				sb.append(histogram.yAxis().binCentre(y));
				sb.append("\t");
				sb.append(histogram.binHeight(x, y));
				sb.append("\t");
				sb.append(xp.binHeight(x));
				sb.append("\t");
				sb.append(yp.binHeight(y));
				sb.append("\n");
			}				
			sb.append("\n");
		}
		return sb.toString();
	}

	public static double colSum(RealMatrix m, int c) {
		double re = 0;
		for (int i=0; i<m.getColumnDimension(); i++)
			re+=m.getEntry(i, c);
		return re;
	}
	
	public static int getIntersection(Interval a, Interval b) {
		return Math.max(0, Math.min(a.getStop(), b.getStop())-Math.max(a.getStart(), b.getStart())+1);
	}
	
	public static int signum(int a) {
		if (a==0) return 0;
		return a<0?-1:1;
	}

	/**
	 * Throws an exception if n is either a real or to big to be represented by a byte.
	 * @param n
	 * @return
	 */
	public static byte byteValueExact(Number n) {
		if (n instanceof Byte) return n.byteValue();
		double d = n.doubleValue();
		long l = n.longValue();
		if (d==(double)l) {
			if (l>=Byte.MIN_VALUE && l<=Byte.MAX_VALUE)
				return (byte) l;
		}
		throw new NumberFormatException();
	}

	
	/**
	 * Throws an exception if n is either a real or to big to be represented by a byte.
	 * @param n
	 * @return
	 */
	public static short shortValueExact(Number n) {
		if (n instanceof Short || n instanceof Byte) return n.shortValue();
		double d = n.doubleValue();
		long l = n.longValue();
		if (d==(double)l) {
			if (l>=Short.MIN_VALUE && l<=Short.MAX_VALUE)
				return (short) l;
		}
		throw new NumberFormatException();
	}

	
	/**
	 * Throws an exception if n is either a real or to big to be represented by a byte.
	 * @param n
	 * @return
	 */
	public static int intValueExact(Number n) {
		if (n instanceof Integer || n instanceof Short || n instanceof Byte) return n.intValue();
		double d = n.doubleValue();
		long l = n.longValue();
		if (d==(double)l) {
			if (l>=Integer.MIN_VALUE && l<=Integer.MAX_VALUE)
				return (int) l;
		}
		throw new NumberFormatException();
	}

	/**
	 * Throws an exception if n is either a real or to big to be represented by a byte.
	 * @param n
	 * @return
	 */
	public static long longValueExact(Number n) {
		if (n instanceof Long || n instanceof Integer || n instanceof Short || n instanceof Byte) return n.longValue();
		double d = n.doubleValue();
		long l = n.longValue();
		if (d==(double)l) {
			return l;
		}
		throw new NumberFormatException();
	}

	/**
	 * Maps min to max linearly to 0-1
	 * @param min
	 * @param max
	 * @return
	 */
	public static DoubleUnaryOperator linearRange(double min, double max) {
		return x->(x-min)/(max-min);
	}

	public static double saveMin(double a, double b) {
		if (Double.isNaN(a)) return b;
		if (Double.isNaN(b)) return a;
		if (Double.isInfinite(a)) return b;
		if (Double.isInfinite(b)) return a;
		return Math.min(a, b);
	}

	public static double saveMax(double a, double b) {
		if (Double.isNaN(a)) return b;
		if (Double.isNaN(b)) return a;
		if (Double.isInfinite(a)) return b;
		if (Double.isInfinite(b)) return a;
		return Math.max(a, b);
	}

	public static int  nextPowerOfTwo(int value) {
		if (value == 0) return 1;
		value--;
		value |= value >> 1;
		value |= value >> 2;
		value |= value >> 4;
		value |= value >> 8;
		value |= value >> 16;
		return value + 1;
	}
	
	
	
}
