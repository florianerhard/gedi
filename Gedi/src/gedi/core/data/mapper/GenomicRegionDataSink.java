package gedi.core.data.mapper;


import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;

public interface GenomicRegionDataSink<FROM> extends GenomicRegionDataMapper<FROM,Void>  {
	
	
	void accept(ReferenceSequence reference, GenomicRegion region, PixelLocationMapping pixelMapping, FROM data);
	
	
	default Void map(ReferenceSequence reference, GenomicRegion region, PixelLocationMapping pixelMapping, FROM from) {
		accept(reference,region,pixelMapping,from);
		return null;
	}
	
	default boolean hasSideEffect() {
		return true;
	}
	
	
}
