/**
 * 
 *    Copyright 2017 Florian Erhard
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */

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
