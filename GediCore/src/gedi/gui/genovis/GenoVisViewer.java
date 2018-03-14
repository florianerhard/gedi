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

import gedi.core.reference.LazyGenome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.util.gui.PixelBasepairMapper;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

public interface GenoVisViewer {

	void relayout();
	void repaint();
	void repaint(boolean clearBuffer);
	void reload();
	
	PixelBasepairMapper getLocationMapper();
	LazyGenome getGenome();
	
	default void refreshLocation() {
		setLocation(getReference(), getRegion());
	}
	
	default void setLocation(ReferenceSequence reference, GenomicRegion region) {
		setLocation(new ReferenceSequence[] {reference}, new GenomicRegion[] {region});
	}
	
	default void setLocation(ReferenceSequence reference, GenomicRegion region, boolean async) {
		setLocation(new ReferenceSequence[] {reference}, new GenomicRegion[] {region},async);
	}
	
	default void setLocation(ReferenceSequence[] reference, GenomicRegion[] region) {
		setLocation(reference,region, true);
	}
	
	void setLocation(ReferenceSequence[] reference, GenomicRegion[] region, boolean async);
	ReferenceSequence[] getReference();
	GenomicRegion[] getRegion();
	double getScreenWidth();
	double getLeftMarginWidth();
	
	void addTrack(VisualizationTrack<?,?> track);
	void insertTrack(VisualizationTrack<?,?> track, VisualizationTrack<?,?> after);
	List<VisualizationTrack<?, ?>> getTracks();
	default VisualizationTrack<?, ?> getTrack(String id) {
		for (VisualizationTrack<?, ?> t : getTracks())
			if (t.getId().equals(id))
				return t;
		return null;
	}
	
	
	void addSmartTrack(VisualizationTrack<?,?> track);
	void removeSmartTrack(VisualizationTrack<?,?> track);
	
	BufferedImage getImage();
	
	default void writePng(String path) throws IOException {
		ImageIO.write(getImage(), "png", new File(path));
	}
	
	/**
	 * In the coordinates system of this!
	 * @param x
	 * @param y
	 * @return
	 */
	<D> VisualizationTrackPickInfo<D> pick(double x, double y, VisualizationTrackPickInfo<D> re);
	
	<P> void addListener(Consumer<VisualizationTrackPickInfo<P>> l, VisualizationTrackPickInfo.TrackEventType...catchType);
	void addReloadListener(Consumer<GenoVisViewer> l);
	void addPrepaintListener(Consumer<GenoVisViewer> l);
	void showToolTip(String text);
	
}
