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

package gedi.core.data.index;

import java.util.HashMap;
import java.util.function.Function;
import java.util.logging.Logger;

import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;

public class MemoryDataIndex<D,R> extends HashMap<D,ImmutableReferenceGenomicRegion<R>> implements DataIndex<D,R> {

	private static final Logger log = Logger.getLogger( MemoryDataIndex.class.getName() );
	
	public MemoryDataIndex(GenomicRegionStorage<R> storage, Function<R,D> mapper) {
		storage.iterateReferenceGenomicRegions().forEachRemaining(rgr->putWarn(mapper.apply(rgr.getData()),rgr));
	}
	
	
	private void putWarn(D data, ImmutableReferenceGenomicRegion<R> rgr) {
		if (containsKey(data))
			log.warning("Element "+data+" already present in index!");
		put(data,rgr);
	}
}
