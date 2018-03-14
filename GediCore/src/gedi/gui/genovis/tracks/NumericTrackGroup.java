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
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.gui.genovis.GenoVisViewer;
import gedi.gui.genovis.HasSubtracks;
import gedi.gui.genovis.VisualizationTrackPickInfo;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

@GenomicRegionDataMapping(fromType=Void.class)
@SuppressWarnings("rawtypes")
public class NumericTrackGroup extends NumericTrack<Void> implements HasSubtracks {

	private ArrayList<NumericTrack> tracks = new ArrayList<NumericTrack>();
	private ScaleLimitLinker limitLinker = new ScaleLimitLinker();
	
	public NumericTrackGroup() {
		super(Void.class);
	}
	
	@Override
	public boolean isEmptyData(Void data) {
		return false;
	}
	
	public ArrayList<NumericTrack> getSubTracks() {
		return tracks;
	}
	
	@Override
	protected double getLogbase() {
		return tracks.isEmpty()?super.getLogbase():tracks.get(0).getLogbase();
	}
	
	public void setLogScale(double base) {
		for (NumericTrack sub : tracks)
			sub.setLogScale(base);
		super.setLogScale(base);
	}
	
	
	public ScaleLimitLinker getLimitLinker() {
		return limitLinker;
	}
	
	@Override
	public void setBounds(Rectangle2D bounds) {
		super.setBounds(bounds);
		for (NumericTrack t : tracks)
			t.setBounds(bounds);
	}
	
	@Override
	public void setGenoVis(GenoVisViewer viewer) {
		super.setGenoVis(viewer);
		for (NumericTrack t : tracks)
			t.setGenoVis(viewer);
		
	}

	@Override
	public void setView(ReferenceSequence[] reference, GenomicRegion[] region) {
		super.setView(reference, region);
		for (NumericTrack t : tracks)
			t.setView(reference, region);
	}

	@Override
	public boolean isUptodate() {
		for (NumericTrack t : tracks)
			if (!t.isUptodate())
				return false;
		return super.isUptodate();
	}
	
	@Override
	public boolean isVisible() {
		for (NumericTrack t : tracks)
			if (t.isVisible()) return true;
		return false;
	}
	
	
	public void add(NumericTrack track) {
		if (track.group!=null) throw new RuntimeException("Cannot add to multiple groups!");
		if (track.limitLinker!=null) throw new IllegalArgumentException("Cannot set limit linker when in a group!");
		track.group = this;
		tracks.add(track);
	}
	
	
	@Override
	public void pick(VisualizationTrackPickInfo<Double> info) {
		for (NumericTrack tr : tracks)
			if (!tr.isHidden() && !tr.isDataEmpty() && tr.isVisible()) {
				tr.pick(info);
				return;
			}
	}
	
	@Override
	public void prepaint(Graphics2D g2) {
		for (NumericTrack tr : tracks)
			tr.prepaint(g2);
		super.prepaint(g2);
	}
	
	@Override
	public void renderReference(ReferenceSequence reference) {
		for (NumericTrack tr : tracks)
			if (!tr.isHidden() && !tr.isDataEmpty() && tr.isVisible()) {
				tr.renderReference(reference);
			}
	}
	
	@Override
	public gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<Void> renderEnd(
			gedi.gui.genovis.VisualizationTrackAdapter.TrackRenderContext<Void> context) {
		for (NumericTrack tr : tracks)
			tr.renderEnd(context);
		
		return super.renderEnd(context);
	}
	

	@Override
	protected double computeCurrentMin(Void data) {
		return limitLinker.computeCurrentMin(data).N;
	}

	@Override
	protected double computeCurrentMax(Void data) {
		return limitLinker.computeCurrentMax(data).N;
	}
	
	
}
