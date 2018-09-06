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
package gedi.util.math.optim;

import gedi.util.ArrayUtils;
import gedi.util.math.optim.LBFGS.Function;
import gedi.util.math.optim.LBFGS.Result;
import gedi.util.math.optim.LBFGS.Status;

public class ConstrainedBarrierLBFGS {


	public static Result constrOptim(double[] theta, Function fun, double[][] ui, double[] ci) {
		double mu = 1E-4;
		double outereps = 1E-5;
		
		if (ArrayUtils.min(sub(mult(ui,theta),ci))<=0) throw new IllegalArgumentException("Initial value is not in the interior of the feasible region");

		double[] gradient = new double[theta.length];
		double[] thetaold = new double[theta.length];
		double obj = fun.evaluate(theta, gradient, 0, 0);
		
		double r = R(fun,gradient,theta,theta,ui,ci,mu);
		Result a = null;
		double objold = 0;
		
		int i=0;
		for (; i<100; i++) {
			objold = obj;
			double rold = r;
			System.arraycopy(theta, 0, thetaold, 0, theta.length);
			a = LBFGS.lbfgs(thetaold, (th,gr,x,y)->R(fun,gr,theta,thetaold,ui,ci,mu));
			r = a.objective;
			if (Double.isFinite(r) && Double.isFinite(rold) && Math.abs(r-rold)<(0.001+Math.abs(r))*outereps)
				break;
			System.arraycopy(thetaold, 0, theta, 0, theta.length);
			obj = fun.evaluate(theta, gradient, 0, 0);
			if (obj>objold)
				break;
		}
		
		if (i==100) {
			a.additionalStatus = 7;
			a.status = Status.LBFGSERR_UNKNOWNERROR;
		}
		if (obj>objold) {
			a.additionalStatus = 11;
			a.status = Status.LBFGSERR_UNKNOWNERROR;
		}
		
		a.objective = fun.evaluate(theta, gradient, 0, 0);
		return a;
	}

	private static double R(Function fun, double[] gradient, double[] theta, double[] thetaold, double[][] ui, double[] ci, double mu) {                                                                                                                                                  
		double[] uitheta = mult(ui,theta);                                                                                                                                                            
		double[] gi = sub(uitheta,ci);
		for (int i=0; i<gi.length; i++)
			if (gi[i]<0) return Double.NaN;
		double[] giold = sub(mult(ui,thetaold),ci);                                                                                                                                      
		double bar = sum(sub(mult(giold,log(gi)),uitheta));
		if (Double.isInfinite(bar))
			bar = Double.NEGATIVE_INFINITY;
		double re = fun.evaluate(theta, gradient, 0, 0) - mu * bar;

		for (int r=0; r<ui.length; r++) {
			for (int c=0; c<ui[r].length; c++) {
				gradient[c]-=mu*(ui[r][c]*giold[r]/gi[r]-ui[r][c]);
			}	
		}

		return re;
	}

	private static double sum(double[] a) {
		double re = 0;
		for (int i=0; i<a.length; i++)
			re += a[i];
		return re;
	}

	private static double[] mult(double[] a, double[] b) {
		double[] re = new double[a.length];
		for (int i=0; i<a.length; i++)
			re[i] = a[i]*b[i];
		return re;
	}

	private static double[] log(double[] a) {
		double[] re = new double[a.length];
		for (int i=0; i<a.length; i++)
			re[i] = Math.log(a[i]);
		return re;
	}

	private static double[] sub(double[] a, double[] b) {
		double[] re = new double[a.length];
		for (int i=0; i<a.length; i++)
			re[i] = a[i]-b[i];
		return re;
	}

	private static double[] mult(double[][] m, double[] v) {
		double[] re = new double[m.length];
		for (int r=0; r<m.length; r++) {
			for (int c=0; c<m[r].length; c++) {
				re[r]+=m[r][c]*v[c];
			}	
		}
		return re;
	}

}
