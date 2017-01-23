package gedi.core.data.mapper;



public interface DisablingGenomicRegionDataMapper<FROM,TO> extends GenomicRegionDataMapper<FROM, TO> {
	
	boolean isDisabled();

	
}
