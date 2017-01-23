package gedi.core.data.table;

import java.util.function.Function;

import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.mutable.MutableLong;

public class TableIntervalIndex {

	private GenomicRegionStorage<Long> index;
	private Table<?> table;
	private String regionColumn;
	

	public TableIntervalIndex(GenomicRegionStorage<Long> index,
			Table<?> table, String regionColumn) {
		this.index = index;
		this.table = table;
		this.regionColumn = regionColumn;
	}
	
	

	public Table<?> getTable() {
		return table;
	}
	
	public String getRegionColumn() {
		return regionColumn;
	}
	
	public GenomicRegionStorage<Long> getIndex() {
		return index;
	}
	
	
	
}
