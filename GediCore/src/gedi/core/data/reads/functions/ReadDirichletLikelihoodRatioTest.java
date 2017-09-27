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

package gedi.core.data.reads.functions;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.util.ArrayUtils;
import gedi.util.StringUtils;
import gedi.util.io.text.LineOrientedFile;

import java.io.IOException;
import java.util.function.ToDoubleFunction;

import org.apache.commons.math3.special.Gamma;

public class ReadDirichletLikelihoodRatioTest implements ToDoubleFunction<AlignedReadsData> {

	
	private double[] n;
	
	// one number, line by line!
	public ReadDirichletLikelihoodRatioTest(String path) throws IOException {
		n = StringUtils.parseDouble(new LineOrientedFile(path).readAllLines("#"));
		ArrayUtils.normalize(n);
	}
	
	
	@Override
	public double applyAsDouble(AlignedReadsData value) {

		double[] c = value.getTotalCountsForConditions(ReadCountMode.Weight);
		
		double[] a = n.clone();
		ArrayUtils.mult(a, ArrayUtils.sum(c));
		
		
		double lh1 = logProbability(a, n);
		double lh0 = logProbability(c, n);
		
		double lr = 2*lh1-2*lh0;
		
		return lr;
	}

	
	
	public static double logProbability(double[] alpha1, double[] p) {
		double re = 0;
		double asum = 0;
		double gsum = 0;
		for (int i=0; i<p.length; i++) {
			re+=Math.log(p[i])*(alpha1[i]);
			asum+=alpha1[i]+1;
			gsum+=Gamma.logGamma(alpha1[i]+1);
		}
		return re+Gamma.logGamma(asum)-gsum;
	}
}
