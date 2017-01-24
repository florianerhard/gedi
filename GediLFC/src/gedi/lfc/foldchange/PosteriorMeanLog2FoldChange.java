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

package gedi.lfc.foldchange;

import gedi.lfc.Log2FoldChange;

import org.apache.commons.math3.special.Gamma;

public class PosteriorMeanLog2FoldChange implements Log2FoldChange{
	
	private double priorA = 1;
	private double priorB = 1;
	
	/**
	 * These are the prior parameters, i.e. for no pseudocounts, use 1!
	 * @param priorA
	 * @param priorB
	 */
	public PosteriorMeanLog2FoldChange(double priorA, double priorB) {
		this.priorA = priorA;
		this.priorB = priorB;
	}


	@Override
	public double computeFoldChange(double a, double b) {
		if (Double.isNaN(a) || Double.isNaN(b)) return Double.NaN;
		return (Gamma.digamma(a+priorA)-Gamma.digamma(b+priorB))/Math.log(2);
	}

}
