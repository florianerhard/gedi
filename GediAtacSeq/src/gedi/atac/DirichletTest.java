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

package gedi.atac;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.DoubleBinaryOperator;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.commons.math3.special.Gamma;

import gedi.core.data.mapper.GenomicRegionDataMapper;
import gedi.core.data.mapper.GenomicRegionDataMapping;
import gedi.core.data.mapper.GenomicRegionDataSink;
import gedi.core.data.numeric.DenseGenomicNumericProvider;
import gedi.core.data.numeric.GenomicNumericProvider;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelBlockToValuesMap;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.gui.genovis.pixelMapping.PixelLocationMappingBlock;
import gedi.util.ArrayUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.dynamic.DynamicObject;
import gedi.util.math.stat.kernel.Kernel;
import gedi.util.mutable.MutablePair;
import gedi.util.mutable.MutableTuple;

@GenomicRegionDataMapping(fromType=PixelBlockToValuesMap.class,toType=Void.class)
public class DirichletTest implements GenomicRegionDataSink<PixelBlockToValuesMap>{


	@Override
	public void accept(ReferenceSequence reference, GenomicRegion region,
			PixelLocationMapping pixelMapping, PixelBlockToValuesMap data) {
		
		int bases = 0;
		int bins = data.size();
		
		double[] alpha = new double[bins];
		
		for (int b=0; b<data.size(); b++) {
			PixelLocationMappingBlock bl = data.getBlock(b);
			bases+=bl.getStopBp()+1-bl.getStartBp();
			
			alpha[b] = NumericArrayFunction.Sum.applyAsDouble(data.getValues(b))+1;
		}
		
		double[] p = alpha.clone();
		ArrayUtils.normalize(p);
		
		double lh1 = logProbability(alpha, p);
		Arrays.fill(p, 1.0/bins);
		double lh0 = logProbability(alpha, p);
		
		double lr = 2*lh1-2*lh0;
		
		ChiSquaredDistribution d = new ChiSquaredDistribution(bins-2);
		
		System.out.printf("Bases=%d\nBins=%d\nLR=%.4g\np=%.5g",bases,bins,lr,1-d.cumulativeProbability(lr));
		
		
		
	}



	public static double logProbability(double[] alpha, double[] p) {
		double re = 0;
		double asum = 0;
		double gsum = 0;
		for (int i=0; i<p.length; i++) {
			re+=Math.log(p[i])*(alpha[i]-1);
			asum+=alpha[i];
			gsum+=Gamma.logGamma(alpha[i]);
		}
		return re+Gamma.logGamma(asum)-gsum;
	}



	@Override
	public void acceptMeta(DynamicObject meta) {
	}
	
	
}
