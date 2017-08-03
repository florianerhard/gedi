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

package gedi.core.data.mapper;


import java.util.function.Consumer;

import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;

public interface GenomicRegionDataMapper<FROM,TO> {
	
	default void setJob(GenomicRegionDataMappingJob<FROM, TO> job){}
	default void setInput(int index, GenomicRegionDataMapper<?, FROM> input){}
	
	TO map(ReferenceSequence reference, GenomicRegion region, PixelLocationMapping pixelMapping, FROM data);
	
	default <T> void applyForAll(Class<T> cls, Consumer<T> consumer){}
	default boolean hasSideEffect() {
		return false;
	}
	
}