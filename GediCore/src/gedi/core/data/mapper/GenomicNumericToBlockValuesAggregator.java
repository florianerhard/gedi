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

package gedi.core.data.mapper;

import java.util.function.DoubleBinaryOperator;

import gedi.core.data.numeric.DenseGenomicNumericProvider;
import gedi.core.data.numeric.GenomicNumericProvider;
import gedi.core.data.numeric.GenomicNumericProvider.PositionNumericIterator;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelBlockToValuesMap;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.gui.genovis.pixelMapping.PixelLocationMappingBlock;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.mutable.MutablePair;

@GenomicRegionDataMapping(fromType=GenomicNumericProvider.class,toType=PixelBlockToValuesMap.class)
public class GenomicNumericToBlockValuesAggregator implements GenomicRegionDataMapper<GenomicNumericProvider, PixelBlockToValuesMap>{

	private DoubleBinaryOperator fun;
	
	public GenomicNumericToBlockValuesAggregator(DoubleBinaryOperator fun) {
		this.fun = fun;
	}

	@Override
	public PixelBlockToValuesMap map(ReferenceSequence reference,
			GenomicRegion region,PixelLocationMapping pixelMapping,
			GenomicNumericProvider data) {
		
		PixelBlockToValuesMap re = new PixelBlockToValuesMap(pixelMapping, data.getNumDataRows(), NumericArrayType.Double);
		
		for (int b=0; b<pixelMapping.size(); b++) {
			PixelLocationMappingBlock block = pixelMapping.get(b);
			NumericArray vals = re.getValues(b);
		
			
			PositionNumericIterator vit = data.iterateValues(reference, new ArrayGenomicRegion(block.getStartBp(),block.getStopBp()+1));
			int last = block.getStartBp()-1;
			boolean gap = false;
			boolean initial = true;
			
			while (vit.hasNext()) {
				int p = vit.nextInt();
				if (last+1!=p)
					gap = true;
				if (initial) {
					for (int i=0; i<data.getNumDataRows(); i++) 
						vals.setDouble(i, vit.getValue(i));
					initial = false;
				} else {
					for (int i=0; i<data.getNumDataRows(); i++) 
						vals.setDouble(i, fun.applyAsDouble(vals.getDouble(i), vit.getValue(i)));
				}
			}
			if (gap)
				for (int i=0; i<data.getNumDataRows(); i++) 
					vals.setDouble(i, fun.applyAsDouble(vals.getDouble(i), 0));
		}
		
		
		
		return re;
	}


	
}
