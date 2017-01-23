package gedi.util.parsing;

import gedi.core.region.GenomicRegion;

public class GenomicRegionParser implements Parser<GenomicRegion> {
	

	@Override
	public GenomicRegion apply(String s) {
		return GenomicRegion.parse(s);
	}

	@Override
	public Class<GenomicRegion> getParsedType() {
		return GenomicRegion.class;
	}
}