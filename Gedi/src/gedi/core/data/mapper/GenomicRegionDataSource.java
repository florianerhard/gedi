package gedi.core.data.mapper;


import java.util.function.Consumer;

import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;

public interface GenomicRegionDataSource<TO> extends GenomicRegionDataMapper<Void,TO>  {
	
	
	TO get(ReferenceSequence reference, GenomicRegion region, PixelLocationMapping pixelMapping);

	default TO map(ReferenceSequence reference, GenomicRegion region,PixelLocationMapping pixelMapping,  Void from) {
		return get(reference,region,pixelMapping);
	}
	
	
	
}
