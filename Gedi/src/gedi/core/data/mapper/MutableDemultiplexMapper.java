package gedi.core.data.mapper;

import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.util.mutable.Mutable;

@GenomicRegionDataMapping(fromType=Mutable.class,toType=Object.class)
public class MutableDemultiplexMapper implements GenomicRegionDataMapper<Mutable, Object>{

	private int index;
	public MutableDemultiplexMapper(int index) {
		this.index = index;
	}


	@Override
	public Object map(ReferenceSequence reference,
			GenomicRegion region,PixelLocationMapping pixelMapping,
			Mutable data) {
		return data.get(index);
		
	}


	
}
