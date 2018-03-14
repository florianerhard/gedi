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
import gedi.util.dynamic.DynamicObject;

public interface GenomicRegionDataSource<TO> extends GenomicRegionDataMapper<Void,TO>  {
	
	
	TO get(ReferenceSequence reference, GenomicRegion region, PixelLocationMapping pixelMapping);
	default DynamicObject getMeta() {
		return DynamicObject.getEmpty();
	}

	default TO map(ReferenceSequence reference, GenomicRegion region,PixelLocationMapping pixelMapping,  Void from) {
		return get(reference,region,pixelMapping);
	}
	
	default DynamicObject mapMeta(DynamicObject meta) {
		return getMeta();
	}
	
	
	
}
