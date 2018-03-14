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


import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.util.dynamic.DynamicObject;

public interface GenomicRegionDataSink<FROM> extends GenomicRegionDataMapper<FROM,Void>  {
	
	
	void accept(ReferenceSequence reference, GenomicRegion region, PixelLocationMapping pixelMapping, FROM data);
	void acceptMeta(DynamicObject meta);
	
	default Void map(ReferenceSequence reference, GenomicRegion region, PixelLocationMapping pixelMapping, FROM from) {
		accept(reference,region,pixelMapping,from);
		return null;
	}
	
	default DynamicObject mapMeta(DynamicObject meta) {
		acceptMeta(meta);
		return null;
	}
	
	default boolean hasSideEffect() {
		return true;
	}
	
	
}
