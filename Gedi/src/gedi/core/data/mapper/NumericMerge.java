package gedi.core.data.mapper;

import java.util.Collection;
import java.util.function.DoubleBinaryOperator;

import gedi.core.data.numeric.DenseGenomicNumericProvider;
import gedi.core.data.numeric.GenomicNumericProvider;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelBlockToValuesMap;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.mutable.MutablePair;
import gedi.util.mutable.MutableTuple;

@GenomicRegionDataMapping(fromType=MutableTuple.class,toType=PixelBlockToValuesMap.class)
public class NumericMerge implements GenomicRegionDataMapper<MutableTuple, PixelBlockToValuesMap>{


	@Override
	public PixelBlockToValuesMap map(ReferenceSequence reference,
			GenomicRegion region,PixelLocationMapping pixelMapping,
			MutableTuple data) {
		return new PixelBlockToValuesMap(data);
		
	}


	
}
