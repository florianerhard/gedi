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
package gedi.lfc.gui;

import gedi.core.data.mapper.GenomicRegionDataMapper;
import gedi.core.data.mapper.GenomicRegionDataMapping;
import gedi.core.data.reads.ContrastMapping;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelBlockToValuesMap;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.util.ArrayUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;

import java.util.Arrays;

import org.apache.commons.math3.distribution.BetaDistribution;

@GenomicRegionDataMapping(fromType={PixelBlockToValuesMap.class},toType=PixelBlockToValuesMap.class)
public class LfcMapper implements GenomicRegionDataMapper<PixelBlockToValuesMap, PixelBlockToValuesMap>{

	private ContrastMapping contrast;
	private double lower=0.05;
	private double upper=0.95;
	
	

	@Override
	public PixelBlockToValuesMap map(ReferenceSequence reference,
			GenomicRegion region,PixelLocationMapping pixelMapping,
			PixelBlockToValuesMap data) {
		
		
		double[] counts = new double[2];
		
		PixelBlockToValuesMap re = new PixelBlockToValuesMap(data.getBlocks(), 2, NumericArrayType.Double);
		for (int i=0; i<pixelMapping.size(); i++) {
			NumericArray in = data.getValues(i);
			Arrays.fill(counts, 0);
			
			for (int c=0; c<in.length(); c++) {
				int cond = contrast==null?(c*2)/in.length():contrast.getMappedIndex(c);
				if (cond!=-1)
					counts[cond]+=in.getDouble(c);
			}
			NumericArray out = re.getValues(i);
			if (ArrayUtils.max(counts)>0) {
//				out.set(0, Math.log(counts[0]/counts[1])/Math.log(2));
				
				BetaDistribution dist = new BetaDistribution(counts[0]+1, counts[1]+1);
				out.set(0,pToLfc(dist.inverseCumulativeProbability(lower)));
				out.set(1,pToLfc(dist.inverseCumulativeProbability(upper)));
			} else {
				out.setDouble(0, Double.NaN);
				out.setDouble(1, Double.NaN);
			}
		}
		
		return re;
	}


	private double pToLfc(double p) {
		return Math.log(p/(1-p))/Math.log(2);
	}
	
	
}
