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

package gedi.riboseq.cleavage;

import gedi.util.ArrayUtils;
import gedi.util.math.stat.RandomNumbers;

public class CleavageModelEstimatorThread extends Thread {

	private CleavageModelEstimator estimator;
	private int c;
	private double[] bestPl;
	private double[] bestPr;
	private double bestU;
	private double bestLL;
	private int repeats;
	private int maxPos;
	
	public CleavageModelEstimatorThread(CleavageModelEstimator estimator, int c, int maxpos, int repeats) {
		this.estimator = estimator;
		this.c = c;
		this.maxPos = maxpos;
		this.repeats = repeats;
	}
	public double[] getBestPl() {
		return bestPl;
	}
	public double[] getBestPr() {
		return bestPr;
	}
	
	public double getBestU() {
		return bestU;
	}
	
	public double getBestLL() {
		return bestLL;
	}
	
	
	public void run () {

		bestLL = Double.NEGATIVE_INFINITY;
		bestPl = new double[estimator.obsMaxLength+1];
		bestPr = new double[estimator.obsMaxLength+1];
		bestU = 0;
		
		RandomNumbers rnd = new RandomNumbers();
		
		for (int rep = 0; rep<Math.max(repeats,1); rep++) {
		
			double u;
			double[] Pl = new double[estimator.obsMaxLength+1];
			double[] Pr = new double[estimator.obsMaxLength+1];
			
			// if repeats<1: fixed init!
			if (repeats<1) {
				estimator.initBySimpleModel(c,Pl,Pr);
			} else {
				// init em algorithm
				for (int i=0; i<=estimator.obsMaxLength; i++) {
					Pl[i] = rnd.getUnif();
					Pr[i] = rnd.getUnif();//dist.density(i);//i-11>=0 && i-11<lv.length && j-11>=0 && j-11<lv.length?lv[i-11]*rv[j-11]:0;
				}
				Pl[maxPos-1]*=4;
				Pl[maxPos]*=10;
				Pl[maxPos+1]*=4;
				
//				for (int i=0; i<=estimator.obsMaxLength; i++) {
//					Pl[i] = 1.0/Pl.length;
//					Pr[i] = 1.0/Pr.length;
//				}
//				Pl[maxPos-1]=Math.max(Pl[maxPos-1], rnd.getNormal(4, 1));
//				Pl[maxPos]=Math.max(Pl[maxPos], rnd.getNormal(10, 2));
//				Pl[maxPos+1]=Math.max(Pl[maxPos+1], rnd.getNormal(4, 1));
			}
			
			ArrayUtils.normalize(Pl);
			ArrayUtils.normalize(Pr);
			
//			Pl =ArrayUtils.concat(ArrayUtils.repeat(0, 10),generatedLeft,ArrayUtils.repeat(0, Pl.length-generatedLeft.length-10));
//			Pr = ArrayUtils.concat(ArrayUtils.repeat(0, 10),generatedRight,ArrayUtils.repeat(0, Pr.length-generatedRight.length-10));
			
			
			double N = 0;
			u=0;
			
			for (int l=estimator.obsMinLength; l<=estimator.obsMaxLength; l++)
				for (int f=0; f<3; f++) {
					// lm=1
					int lm = 1;
					double n = estimator.data[l][f][lm][c];
					N+=n;
					
					u+=n*4.0/3;
					
					lm = 0;
					n = estimator.data[l][f][lm][c];
					N+=n;
				}
			
			u = u/N;
			
			double eps = 1E-14;
			
			// compute log likelihood
			double ll = 0;
			double total = 0;
			
			for (int l=estimator.obsMinLength; l<=estimator.obsMaxLength; l++)
				for (int f=0; f<3; f++) {
					int f1 = (f+3-1)%3; // shift frame due to untemplated addition
					
					int lm = 1;
					double n = estimator.data[l][f][lm][c];
					total+=n;
					if (n>0) {
						double p = 0;
						for (int i=f1; i<Pl.length && l-i-3-1>=0; i+=3)
							p+=Pl[i]*Pr[l-i-3-1];
					
						ll += n*Math.log(p);
					}
					
					lm = 0;
					n = estimator.data[l][f][lm][c];
					total+=n;
					if (n>0) {
						double p = 0;
						for (int i=f1; i<Pl.length && l-i-3-1>=0; i+=3)
							p+=Pl[i]*Pr[l-i-3-1]*u;
						for (int i=f; i<Pl.length && l-i-3>=0; i+=3)
							p+=Pl[i]*Pr[l-i-3]*(1-u);
						
						ll += n*Math.log(p);
					}
						
				}
			
			
	
			
			double[] ql0 = null,ql1 = null,qr0 = null,qr1 = null;
		
			
			for (int it=0; it<estimator.maxiter; it++) {
	
				
				
				ql0 = new double[estimator.obsMaxLength+1];
				qr0 = new double[estimator.obsMaxLength+1];
				ql1 = new double[estimator.obsMaxLength+1];
				qr1 = new double[estimator.obsMaxLength+1];
				double qu = 0;
				
				// E step
				N = 0;
				for (int l=estimator.obsMinLength; l<=estimator.obsMaxLength; l++)
					for (int f=0; f<3; f++) {
						// lm=1
						int lm = 1;
						double n = estimator.data[l][f][lm][c];
						N+=n;
						
						int f1 = (f+3-1)%3; // shift frame due to untemplated addition
						double sum = eps;
						for (int left=f1; left<l-3; left+=3) 
							sum+=Pl[left]*Pr[l-left-3-1];
						for (int left=f1; left<l-3; left+=3)  {
							double s = Pl[left]*Pr[l-left-3-1]/sum*n;
							ql1[left+1]+=s;
							qr1[l-left-1-3]+=s;
						}
						
						qu+=n;//*4.0/3;
						
						
						// lm=0
						lm = 0;
						n = estimator.data[l][f][lm][c];
						N+=n;
					
						f1 = (f+3-1)%3; // shift frame due to untemplated addition
						double sum1 = eps;
						double sum0 = eps;
						
						// proportion of expected unobserved mismatches: from N reads:
						//  (1-u)*N have not untemplateded addition (no mm)
						//  u*1/4*N have an unobserved untemplateded addition (no mm)
						//  u*3/4*N have an observed untemplateded addition (no mm)
						// this is (u*1/4*N) / ( u*1/4*N + (1-u)*N )
						double prop = (u/(4-3*u));
						
						
						for (int left=f1; left<l-3; left+=3) {
							sum1+=Pl[left]*Pr[l-left-3-1]*prop;
						}
						for (int left=f; left<l-3; left+=3) {
							sum0+=Pl[left]*Pr[l-left-3]*(1-prop);
						}
						sum = sum1+sum0;
						
						for (int left=f1; left<l-3; left+=3) {
							double s = Pl[left]*Pr[l-left-3-1]*prop/sum*n;
							ql1[left]+=s;
							qr1[l-left-1-3]+=s;
						}
						for (int left=f; left<l-3; left+=3) {
							double s = Pl[left]*Pr[l-left-3]*(1-prop)/sum*n;
							ql0[left]+=s;
							qr0[l-left-3]+=s;
						}
						qu+=sum1/sum*n;
						
					}
				
//				System.out.println("ql0="+Arrays.toString(ql0));
//				System.out.println("qr0="+Arrays.toString(qr0));
//				System.out.println("ql1="+Arrays.toString(ql1));
//				System.out.println("qr1="+Arrays.toString(qr1));
				
				
	//			System.out.println("qlr0=");
	//			System.out.println(ArrayUtils.matrixToString(qlr0,11,16,11,16,"%.2f"));
	//			System.out.println("qlr1=");
	//			System.out.println(ArrayUtils.matrixToString(qlr1,11,16,11,16,"%.2f"));
	//			System.out.println("qu="+qu);
	//			System.out.println("N="+N);
				
				
				// M step
//				for (int i=1; i<=obsMaxLength; i++) {
//						double exp = Math.min(ql0[i], ql1[i]/3);
//						ql0[i-1]+=exp;
//						ql0[i]-=exp;
//				}
				for (int i=0; i<=estimator.obsMaxLength; i++){
					Pl[i] = ((i+1<ql1.length?ql1[i+1]:0)+ql0[i])/total;
					Pr[i] = (qr1[i]+qr0[i])/total;
				}
				
//				double totalsum = ArrayUtils.sum(qlr0)+ArrayUtils.sum(qlr1);
//				for (int i=0; i<=obsMaxLength; i++)
//					for (int j=0; j<=obsMaxLength; j++)
//						Plr[i][j] = ((i+1<qlr1.length?qlr1[i+1][j]:0)+qlr0[i][j])/totalsum;
				
				u = qu/N;
				
				// compute log likelihood
				double oldll = ll;
				ll = 0;
				
				for (int l=estimator.obsMinLength; l<=estimator.obsMaxLength; l++)
					for (int f=0; f<3; f++) {
						int f1 = (f+3-1)%3; // shift frame due to untemplated addition
						
						int lm = 1;
						double n = estimator.data[l][f][lm][c];
						
						if (n>0) {
							double p = 0;
							for (int i=f1; i<Pl.length && l-i-3-1>=0; i+=3)
								p+=Pl[i]*Pr[l-i-3-1];
						
							ll += n*Math.log(p);
						}
						
						lm = 0;
						n = estimator.data[l][f][lm][c];
						
						if (n>0) {
							double p = 0;
							for (int i=f1; i<Pl.length && l-i-3-1>=0; i+=3)
								p+=Pl[i]*Pr[l-i-3-1]*u;
							for (int i=f; i<Pl.length && l-i-3>=0; i+=3)
								p+=Pl[i]*Pr[l-i-3]*(1-u);
							
							ll += n*Math.log(p);
						}
					}
				
				
				estimator.progress.setDescriptionf("Repeat %d, Iteration %d: LL=%.6g; bestLL=%.6g", rep, it, ll, bestLL);
				estimator.progress.incrementProgress();
				
				
				if (ll-oldll<estimator.deltaCutoff ) break;
				
				
			}
			
//			int max = ArrayUtils.argmax(Pl);
			
			if (ll>bestLL) {
				bestLL = ll;
				bestPl = Pl;
				bestPr = Pr;
				bestU = u;
			}
			
		}
		
		
	}
	
}
