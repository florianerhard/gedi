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
package gedi.gui.genovis.tracks;


import gedi.core.data.mapper.GenomicRegionDataMapping;
import gedi.gui.genovis.VisualizationTrackAdapter;
import gedi.gui.genovis.VisualizationTrackPickInfo;

import java.awt.Paint;


@GenomicRegionDataMapping(fromType=Void.class)
public class SpacerTrack extends VisualizationTrackAdapter<Void,Void> {


	public SpacerTrack() {
		this(null,5);
	}
	
	public SpacerTrack(Paint background, double height) {
		super(Void.class);
		this.minHeight = this.prefHeight = this.maxHeight = height;
		this.minPixelPerBasePair=0;
		this.maxPixelPerBasePair=Double.POSITIVE_INFINITY;
		
		setBackground(background);
	}
	
	@Override
	public void pick(VisualizationTrackPickInfo<Void> info) {
	}

	public void setHeight(double height) {
		this.minHeight = this.prefHeight = this.maxHeight = height;
		if (viewer!=null)
			this.viewer.relayout();
	}
	
	
	@Override
	protected gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<Void> renderLabel(
			gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<Void> context) {
		return context;
	}
	@Override
	protected TrackRenderContext<Void> renderTrack(TrackRenderContext<Void> context) {
		context.g2.setPaint(getBackground());
		context.g2.fill(getBoundsWithMargin());
		return context;
	}





	
}
