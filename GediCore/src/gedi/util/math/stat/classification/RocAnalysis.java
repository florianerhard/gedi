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
package gedi.util.math.stat.classification;

import gedi.util.io.text.LineOrientedFile;

import java.io.IOException;
import java.util.Locale;


public abstract class RocAnalysis {

	protected String format = "%e";
	
	public abstract void addScore(double score, boolean isTrue);
	public abstract int getNumPoints();
	public abstract int getTp(int index);
	public abstract int getFp(int index);
	public abstract int getTn(int index);
	public abstract int getFn(int index);
	public abstract double getCutoff(int index);
	public abstract int getNumPositives();
	public abstract int getNumNegatives();
	protected abstract void compute();
	public abstract void switchDirection();
	
	public double getCutoffForFdr(double d) {
		for (int i=getNumPoints()-1; i>=0; i--) {
			if (getFdr(i)<=d) return getCutoff(i);
		}
		return getCutoff(0);
	}
	public double getCutoffForFpr(double d) {
		for (int i=getNumPoints()-1; i>=0; i--) {
			if (getFpr(i)>d) return getCutoff(i+1);
		}
		return getCutoff(0);
	}
	
	public double getCutoffForTpr(double d) {
		for (int i=0; i<getNumPoints(); i++) {
			if (getTpr(i)>d) return getCutoff(i);
		}
		return getCutoff(0);
	}
	
	public int getIndexForCutoff(double cutoff) {
		for (int i=getNumPoints()-1; i>=0; i--) {
			if (getCutoff(i)<cutoff) return i+1;
		}
		return 0;
	}
	
	public double getAucFprTpr() {
		compute();
		double auc = 0;
		double lastAucX = 0;
		double lastAucY = 0;
		for (int i=0; i<getNumPoints(); i++) {
			int tp = getTp(i);
			int fp = getFp(i);
			double tpr = (double)tp/getNumPositives();
			double fpr = (double)fp/getNumNegatives();
			
			double x = fpr;
			double y = tpr;
			
			if (i>0)
				auc+=(lastAucX-x)*(y+lastAucY)/2.0;
			
			lastAucX = x;
			lastAucY = y;
			
		}
		
		return auc;
	}
	
	
	public double getAucPpvTpr() {
		compute();
		double auc = 0;
		double lastAucX = 0;
		double lastAucY = 0;
		for (int i=0; i<getNumPoints(); i++) {
			int tp = getTp(i);
			int fp = getFp(i);
			double tpr = (double)tp/getNumPositives();
			double ppv =  (double)tp/(tp+fp);
			
			double y = ppv;
			double x = tpr;
				
			if (i>0)
				auc+=(lastAucX-x)*(y+lastAucY)/2.0;
			
			lastAucX = x;
			lastAucY = y;
			
		}
		
		auc+=(lastAucX)*(1+lastAucY)/2.0;
		
		return auc;
	}
	
	public double getAcc(int index) {
		compute();
		return ((double)getTp(index)+getFp(index))/(getNumPositives()+getNumNegatives());
	}

	public double getFdr(int index) {
		compute();
		return (double)getFp(index)/(getFp(index)+getTp(index));
	}


	public double getFpr(int index) {
		compute();
		return (double)getFp(index)/getNumNegatives();
	}

	public double getNpv(int index) {
		compute();
		return (double)getTn(index)/(getTn(index)+getFn(index));
	}

	public double getPpv(int index) {
		compute();
		return (double)getTp(index)/(getTp(index)+getFp(index));
	}


	public double getTpr(int index) {
		compute();
		return(double)getTp(index)/getNumPositives();
	}
	
	public void write(LineOrientedFile out) throws IOException {
		write(out,true,format);
	}
	
	public void write(LineOrientedFile out, boolean weakestToo, String format) throws IOException {
		compute();
		boolean alreadyWriting = out.isWriting();
		if (!alreadyWriting)
			out.startWriting();
		out.writeLine(";cutoff\tTP\tTN\tFP\tFN\tTPR\tFPR\tACC\tPPV\tNPV\tFDR");
		for (int i=weakestToo?0:1; i<=getNumPoints(); i++) {
			out.writeLine(String.format(Locale.US,"%.4f\t%d\t%d\t%d\t%d\t"+format+"\t"+format+"\t"+format+"\t"+format+"\t"+format+"\t"+format,
					getCutoff(i),
					getTp(i),
					getTn(i),
					getFp(i),
					getFn(i),
					getTpr(i),
					getFpr(i),
					getAcc(i),
					getPpv(i),
					getNpv(i),
					getFdr(i)));
		}
		if (!alreadyWriting)
			out.finishWriting();
	}
	
	@Override
	public String toString() {
		compute();
		StringBuilder sb = new StringBuilder();
		sb.append(";cutoff\tTP\tTN\tFP\tFN\tTPR\tFPR\tACC\tPPV\tNPV\tFDR\n");
		for (int i=0; i<getNumPoints(); i++) {
			sb.append(String.format(Locale.US,"%.4f\t%d\t%d\t%d\t%d\t"+format+"\t"+format+"\t"+format+"\t"+format+"\t"+format+"\t"+format+"\n",
					getCutoff(i),
					getTp(i),
					getTn(i),
					getFp(i),
					getFn(i),
					getTpr(i),
					getFpr(i),
					getAcc(i),
					getPpv(i),
					getNpv(i),
					getFdr(i)));
		}
		return sb.toString();
	}
	
}
