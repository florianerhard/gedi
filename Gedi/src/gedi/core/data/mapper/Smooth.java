package gedi.core.data.mapper;

import java.util.Collection;
import java.util.function.DoubleBinaryOperator;

import gedi.core.data.numeric.DenseGenomicNumericProvider;
import gedi.core.data.numeric.GenomicNumericProvider;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelBlockToValuesMap;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.gui.genovis.pixelMapping.PixelLocationMappingBlock;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.math.stat.kernel.Kernel;
import gedi.util.mutable.MutablePair;
import gedi.util.mutable.MutableTuple;

@GenomicRegionDataMapping(fromType=PixelBlockToValuesMap.class,toType=PixelBlockToValuesMap.class)
public class Smooth implements GenomicRegionDataMapper<PixelBlockToValuesMap, PixelBlockToValuesMap>{

	private Kernel kernel;
	
	public void setKernel(Kernel kernel) {
		this.kernel = kernel.prepare();
	}
	

	@Override
	public PixelBlockToValuesMap map(ReferenceSequence reference,
			GenomicRegion region,PixelLocationMapping pixelMapping,
			PixelBlockToValuesMap data) {
		
		PixelBlockToValuesMap re = new PixelBlockToValuesMap(data, false);
		
		int N = data.getBlocks().size();
		int s =(int)Math.ceil(kernel.halfSize());
		int s2 =(int)Math.floor(kernel.halfSize());
		for (int b=s; b<N-s; b++) {
			NumericArray tar = re.getValues(b);
			for (int f=b-s2; f<=b+s2; f++) {
				NumericArray vals = data.getValues(f);
				for (int d=0; d<vals.length(); d++)
					tar.add(d, vals.getDouble(d)*kernel.applyAsDouble(f-b));
			}
		}
		
		return re;
		
	}


	
}
