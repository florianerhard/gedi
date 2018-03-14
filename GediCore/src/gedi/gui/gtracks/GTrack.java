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
package gedi.gui.gtracks;

import gedi.core.data.mapper.DisablingGenomicRegionDataMapper;
import gedi.core.data.mapper.GenomicRegionDataMapper;
import gedi.gui.gtracks.rendering.GTracksRenderer;
import gedi.gui.gtracks.style.GTrackStyles;
import gedi.util.ReflectionUtils;
import gedi.util.dynamic.DynamicObject;

public interface GTrack<D,P> extends GenomicRegionDataMapper<D,GTracksRenderer>, DisablingGenomicRegionDataMapper<D, GTracksRenderer> {

	public enum HeightType {
		Fixed,// Will always render to a fixed height (e.g. sequence, packed tracks)
		Adaptive // Will always render into what is there
	}
	
	HeightType geHeightType();
	
	
	default void setStyles(DynamicObject styles) {
		try {
			ReflectionUtils.set(this, "style", new GTrackStyles(styles));
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
		}
	}
	
}
