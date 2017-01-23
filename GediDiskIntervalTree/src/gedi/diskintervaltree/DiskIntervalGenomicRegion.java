package gedi.diskintervaltree;

import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;

public class DiskIntervalGenomicRegion<T> extends ArrayGenomicRegion {

	private T data;
	
	public DiskIntervalGenomicRegion(int start, int end, T data) {
		super(start,end);
		this.data = data;
	}

	public DiskIntervalGenomicRegion(int[] coord, T data) {
		super(coord);
		this.data=data;
	}

	public T getData() {
		return data;
	}
	

}
