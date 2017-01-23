package gedi.jdbc;

import gedi.core.region.ArrayGenomicRegion;

public class IdGenomicRegion extends ArrayGenomicRegion {

	private long id;
	
	
	public IdGenomicRegion(int[] region, long id) {
		super(region);
		this.id = id;
	}
	
	public long getId() {
		return id;
	}
	
}
