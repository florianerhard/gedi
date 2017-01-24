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

package gedi.gui.genovis;

import gedi.core.data.mapper.DisablingGenomicRegionDataMapper;
import gedi.core.data.mapper.GenomicRegionDataSink;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.GenomicRegion;
import gedi.util.dynamic.DynamicObject;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.function.Consumer;


public interface VisualizationTrack<D,P> extends GenomicRegionDataSink<D>, DisablingGenomicRegionDataMapper<D, Void> {

	double getMinHeight();
	double getPrefHeight();
	double getMaxHeight();
	
	double getLeftMarginWidth();
	
	DynamicObject getStyles();
	void setStyles(DynamicObject meta);
	
	Rectangle2D getBounds();
	Rectangle2D getBoundsWithMargin();

	void setBounds(Rectangle2D bounds);
	void setGenoVis(GenoVisViewer viewer);
	void setView(ReferenceSequence[] reference, GenomicRegion[] region);
	boolean isUptodate();
	
	Dimension getPreferredLegendBounds();
	void paintLegend(Graphics2D g2, Rectangle2D dest, int columns, int rows);
	/**
	 * Either columns or rows must be smaller than 1
	 * @param g2
	 * @param columns
	 * @param rows
	 * @return
	 */
	Dimension2D measureLegend(Graphics2D g2, int columns, int rows);
	void prepaint(Graphics2D g2);
	void paint(Graphics2D g2);
	
	String getId();
	
	GenoVisViewer getViewer();
	
	boolean isVisible();
	void setHidden(boolean hidden);
	boolean isHidden();
	
	GenomicRegion[] getSmartRegions();
	void setSmartLayout(boolean smart);
	
	boolean isDataEmpty();
	boolean isAutoHide();
	void setAutoHide(boolean autohide);
	
	// set fixed strand, e.g. when a filter is set for its genomicregionstorage
	void setStrand(Strand strand);
	
	void pick(VisualizationTrackPickInfo<P> info);
	
	void addListener(Consumer<VisualizationTrackPickInfo<P>> l, VisualizationTrackPickInfo.TrackEventType...catchType);
	List<Consumer<VisualizationTrackPickInfo<P>>> getListener(VisualizationTrackPickInfo.TrackEventType type);
	
	
}
