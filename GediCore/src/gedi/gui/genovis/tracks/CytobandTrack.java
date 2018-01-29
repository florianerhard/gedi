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

import gedi.core.data.annotation.ScoreNameAnnotation;
import gedi.core.data.mapper.GenomicRegionDataMapping;
import gedi.core.region.GenomicRegion;
import gedi.gui.genovis.tracks.boxrenderer.BoxRenderer;
import gedi.util.PaintUtils;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;

import java.awt.Color;


@GenomicRegionDataMapping(fromType=IntervalTree.class)
public class CytobandTrack extends PackRegionTrack<ScoreNameAnnotation> {


	public CytobandTrack() {
		this.minHeight = this.prefHeight = this.maxHeight = 15;
		this.hspace = 0;
		this.minPixelPerBasePair=0;
		this.maxPixelPerBasePair=1.0/5_000;
		simpleBasePairsPerPixel = 0;
		BoxRenderer<ScoreNameAnnotation> ren = (BoxRenderer<ScoreNameAnnotation>) this.boxRenderer;
		
		ren.stringer = (r)->r.getData().getName();
		ren.background = (r)->r.getData().getScore()>0.5?PaintUtils.parseColor("#CC6600"):PaintUtils.parseColor("#FFFFCC");
		ren.foreground = (r)->r.getData().getScore()>0.5?Color.WHITE:Color.BLACK;
		ren.border = ren.background;
	}
	
	
	@Override
	protected gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<IntervalTree<GenomicRegion, ScoreNameAnnotation>> renderLabel(
			gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<IntervalTree<GenomicRegion, ScoreNameAnnotation>> context) {
		return context;
	}
	
}
