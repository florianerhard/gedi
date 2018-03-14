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

import gedi.util.ArrayUtils;
import gedi.util.math.stat.testing.ExtendedMannWhitneyTest;
import gedi.util.math.stat.testing.ExtendedMannWhitneyTest.MannWhitneyTestStatistic;

import java.util.Arrays;


public class CompleteRocAnalysis extends RocAnalysis {

	private double[] t = new double[100];
	private double[] f = new double[100];
	private int ti = 0;
	private int fi = 0;
	
	private int[] tp;
	private int[] fp;
	private double[] cutoff;
	private int num;
	
	private boolean computed = false;
	private boolean reverseFlag;
	
	public CompleteRocAnalysis() {
		this(false);
	}
	/**
	 * Whether or not to invert meaning of score
	 * @param reverseFlag
	 */
	public CompleteRocAnalysis(boolean reverseFlag) {
		this.reverseFlag = reverseFlag;
	}

	@Override
	/**
	 * The larger the score, the better!
	 */
	public void addScore(double score, boolean isTrue) {
		if (reverseFlag)
			score = -score;
		
		if (isTrue) {
			if (ti>=t.length) 
				t = ArrayUtils.redimPreserve(t, t.length*2);
			t[ti++]=score;
		} else {
			if (fi>=f.length) 
				f = ArrayUtils.redimPreserve(f, f.length*2);
			f[fi++]=score;
		}
		computed=false;
	}
	
	@Override
	public void switchDirection() {
		ArrayUtils.mult(t, -1);
		ArrayUtils.mult(f, -1);
		computed = false;
	}
	
	public MannWhitneyTestStatistic computeWilcoxon() {
		return computeWilcoxon(false);
	}
	
	public MannWhitneyTestStatistic computeWilcoxon(boolean ignoreWarning) {
		compute();
		ExtendedMannWhitneyTest test = new ExtendedMannWhitneyTest();
		int i=0, j=0;
		while (i<ti && j<fi) {
			double d = Math.min(t[i],f[j]);
			int ni = i;
			int nj = j;
			for (;ni<ti && t[ni]==d;ni++);
			for (;nj<fi && f[nj]==d;nj++);
			test.add(ni-i, nj-j);
			i=ni;
			j=nj;
		}
		while (i<ti) {
			int ni = i;
			for (;ni<ti && t[ni]==t[i];ni++);
			test.add(ni-i, 0);
			i=ni;
		}
		while (j<fi) {
			int nj = j;
			for (;nj<fi && f[nj]==f[j];nj++);
			test.add(0, nj-j);
			j=nj;
		}
		return test.getTestStatistic(ignoreWarning);
	}
	

	@Override
	public int getNumPoints() {
		compute();
		return num;
	}
	
	public int getNumPositives() {
		return ti;
	}
	
	public int getNumNegatives() {
		return fi;
	}
	
	protected void compute() {
		if (computed)return;
		computed = true;
		tp=new int[ti+fi+1];
		fp=new int[ti+fi+1];
		cutoff=new double[ti+fi+1];
		
		Arrays.sort(t, 0, ti);
		Arrays.sort(f, 0, fi);
		int index = 0;
		double old = Double.NaN;
		
		tp[0]=0;
		fp[0]=0;
		int i=0,j=0;
		while(i<ti || j<fi) {
			if (j>=fi||(i<ti && t[i]<f[j])) {
				if (old==t[i])index--;
				cutoff[index]=t[i];
				tp[index++]++;
				old = t[i];
				i++;
			} else {
				if (old==f[j])index--;
				cutoff[index]=f[j];
				fp[index++]++;
				old = f[j];
				j++;
			}
		}
		
		for (i=index-2; i>=0; i--) {
			tp[i]+=tp[i+1];
			fp[i]+=fp[i+1];
		}
		cutoff[index]=Double.POSITIVE_INFINITY;
		num=index;
	}

	
	@Override
	public double getCutoff(int index) {
		compute();
		return reverseFlag?-cutoff[index]:cutoff[index];
	}

	@Override
	public int getFn(int index) {
		compute();
		return tp[0]-tp[index];
	}

	@Override
	public int getFp(int index) {
		compute();
		return fp[index];
	}

	@Override
	public int getTn(int index) {
		compute();
		return fp[0]-fp[index]; 
	}

	@Override
	public int getTp(int index) {
		compute();
		return tp[index];
	}

	
	
}
