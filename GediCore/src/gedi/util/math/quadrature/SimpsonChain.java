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

package gedi.util.math.quadrature;

import java.util.Stack;
import java.util.function.DoubleUnaryOperator;

import gedi.util.ArrayUtils;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.math.function.PiecewiseLinearFunction;

public class SimpsonChain {

	private DoubleUnaryOperator f;
	private double eps;

	private double[] x;
	private double[] y;
	
	private PiecewiseLinearFunction fun;

	public SimpsonChain(double a, double b, DoubleUnaryOperator f, double eps, int maxDepth) {
		this.f = f;
		this.eps = eps;
		double c = (a + b)/2, h = b - a;                                                                  
		double fa = f.applyAsDouble(a), fb = f.applyAsDouble(b), fc = f.applyAsDouble(c);                                                           
		double S = (h/6)*(fa + 4*fc + fb);         
		SimpsonElement tree = new SimpsonElement(a, b, S, fa, fb, fc, maxDepth);
		
		DoubleArrayList x = new DoubleArrayList();
		DoubleArrayList y = new DoubleArrayList();
		
		Stack<SimpsonElement> dfs = new Stack<>();
		dfs.push(tree);
		while (!dfs.isEmpty()) {
			SimpsonElement se = dfs.pop();
			if (se.left==null) {
				x.add(se.b);
				y.add(se.F);
			} else {
				dfs.push(se.right);
				dfs.push(se.left);
			}
		}
		
		this.x = x.toDoubleArray();
		y.cumSum(1);
		this.y = y.toDoubleArray();
		ArrayUtils.mult(this.y, 1/this.y[this.y.length-1]);
		this.fun = new PiecewiseLinearFunction(this.x, this.y);
	}

	public int count() {
		return x.length;
	}
	
	public double integral() {
		return y[y.length-1];
	}
	
	public double density(double x) {
		return f.applyAsDouble(x)/y[y.length-1];
	}

	public double cumulativeProbability(double x) {
		return fun.applyAsDouble(x);
	}
	
	private class SimpsonElement {
		private SimpsonElement left;
		private SimpsonElement right;


//		private double a;
		private double b;
		private double F;

		public SimpsonElement(double a, double b,double S, double fa, double fb, double fc, int bottom) {
			double c = (a + b)/2, h = b - a;                                                                  
			double d = (a + c)/2, e = (c + b)/2;                                                              
			double fd = f.applyAsDouble(d), fe = f.applyAsDouble(e);                                                                      
			double Sleft = (h/12)*(fa + 4*fd + fc);                                                           
			double Sright = (h/12)*(fc + 4*fe + fb);                                                          
			double S2 = Sleft + Sright;                                                                       
//			this.a = a;
			this.b = b;
			this.F = S2 + (S2 - S)/15;
			if (bottom > 0 && Math.abs((S2 - S)/F) > 15*eps) {
				left = new SimpsonElement(a, c, Sleft, fa, fc, fd, bottom-1);
				right = new SimpsonElement(c, b, Sright, fc, fb, fe, bottom-1);
			}
		}
	}

}
