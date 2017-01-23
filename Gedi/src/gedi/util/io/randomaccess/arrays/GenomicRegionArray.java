package gedi.util.io.randomaccess.arrays;

import gedi.core.region.ArrayGenomicRegion;
import gedi.util.io.randomaccess.serialization.BinarySerializableArray;

public class GenomicRegionArray extends BinarySerializableArray<ArrayGenomicRegion> {

	public GenomicRegionArray() {
		this(new ArrayGenomicRegion[0]);
	}
	
	public GenomicRegionArray(ArrayGenomicRegion[] a) {
		super(ArrayGenomicRegion.class);
		setArray(a);
	}

}
