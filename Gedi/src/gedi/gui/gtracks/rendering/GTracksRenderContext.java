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

package gedi.gui.gtracks.rendering;

import java.util.LinkedHashMap;

import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.gui.PixelBasepairMapper;

public class GTracksRenderContext {

	
	private LinkedHashMap<ReferenceSequence, GenomicRegion> regions = new LinkedHashMap<>();
	private PixelBasepairMapper xmapper;

	public GTracksRenderContext(ReferenceSequence[] references, GenomicRegion[] regions, PixelBasepairMapper xmapper) {
		for (int i=0; i<references.length; i++)
			this.regions.put(references[i],regions[i]);
		this.xmapper = xmapper;
	}
	
	public GTracksRenderContext(ReferenceSequence reference, GenomicRegion region, PixelBasepairMapper xmapper) {
		this.regions.put(reference,region);
		this.xmapper = xmapper;
	}
	
	
	public GenomicRegion getRegionToRender(ReferenceSequence reference) {
		return regions.get(reference);
	}

	public PixelBasepairMapper getLocationMapper() {
		return xmapper;
	}

}
