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

package gedi.util.math.stat.testing.multipleTesting;

import gedi.util.ArrayUtils;


public abstract class AbstractMultipleTestingCorrection implements MultipleTestingCorrectionMethod{
	
	protected double[] pvals;
	protected CorrectPvalueCallback[] callbacks;
	protected int index;
	protected int insig = 0;
	
	public AbstractMultipleTestingCorrection() {
		this(10);
	}
	
	public AbstractMultipleTestingCorrection(int size) {
		pvals = new double[size];
		callbacks = new CorrectPvalueCallback[size];
	}
	
	@Override
	public int addPvalue(double pvalue, CorrectPvalueCallback callback) {
		if (index>=pvals.length) {
			pvals = ArrayUtils.redimPreserve(pvals, pvals.length*2);
			callbacks = ArrayUtils.redimPreserve(callbacks, pvals.length);
		}
		pvals[index]=pvalue;
		callbacks[index]=callback;
		index++;
		return insig+index;
	}
	
	@Override
	public int addInsignificant() {
		insig++;
		return index+insig;
	}

	@Override
	public void correct() {
		ArrayUtils.parallelSort(pvals, callbacks,0,index);
		correct(pvals,index, insig);
		for (int i=0; i<index; i++) 
			callbacks[i].setCorrectedPvalue(pvals[i]);
		index = 0;
		insig = 0;
	}
	
	
	protected abstract void correct(double[] pvals, int length, int additionalInsignificant);

	
	
}
