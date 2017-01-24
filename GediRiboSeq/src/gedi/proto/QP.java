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

package gedi.proto;


public class QP {

	public static void main(String[] args) throws Exception {
		

//		// Objective function
//		double[][] P = {
//				{1,0,0,0,0,0},
//				{0,1,0,0,0,0},
//				{0,0,1,0,0,0},
//				{0,0,0,1E-6,0,0},
//				{0,0,0,0,1E-6,0},
//				{0,0,0,0,0,1E-6}
//		};
//		PDQuadraticMultivariateRealFunction objectiveFunction = new PDQuadraticMultivariateRealFunction(P, null, 0);
//
//		//equalities
//		double[][] A = {
//				{0,0,0,1,1,1},
//				{1,0,0,.7,.1,0},
//				{0,1,0,.2,.7,.1},
//				{0,0,1,0,.2,.7}
//				};
//		
//		double[] b = {1000,97,705,198};
//
//		//inequalities
//		ConvexMultivariateRealFunction[] inequalities = {
//			new LinearMultivariateRealFunction(new double[]{0,0,0,-1,0,0}, 0),
//			new LinearMultivariateRealFunction(new double[]{0,0,0,0,-1,0}, 0),
//			new LinearMultivariateRealFunction(new double[]{0,0,0,0,0,-1}, 0)
//		};
//		
//		//optimization problem
//		OptimizationRequest or = new OptimizationRequest();
//		or.setF0(objectiveFunction);
//		or.setInitialPoint(new double[] { b[1]-333,b[2]-333,b[3]-333,333,333,334});
//		or.setFi(inequalities); //if you want x>0 and y>0
//		or.setA(A);
//		or.setB(b);
//		or.setToleranceFeas(1.E-12);
//		or.setTolerance(1.E-12);
//		
//		//optimization
//		JOptimizer opt = new JOptimizer();
//		opt.setOptimizationRequest(or);
//		for (int i=0; i<10000; i++) {
//			
//			opt.optimize();
////			System.out.println(Arrays.toString(opt.getOptimizationResponse().getSolution()));
//
//		}
		
	}
	/*
	m<-matrix(c(0.7,0.2,0,0.1,0.7,0.2,0,0.1,0.7),3)
	c=c(0,1000,0)
	o<-apply(sapply(1:3,function(i) rmultinom(1,prob=m[,i],size=c[i])),1,sum)
	library(quadprog)
	C=rbind(c(0,0,0,1,1,1),cbind(diag(3),m),cbind(matrix(0,3,3),diag(3)))
	D<-matrix(0,6,6)
	diag(D[1:3,1:3])=1
	diag(D[4:6,4:6])=0.000001
	b=c(sum(o),o,0,0,0)
	solve.QP(D,rep(0,6),t(C),b,3)
*/
	
}
