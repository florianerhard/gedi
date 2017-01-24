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

package gedi.gui.genovis.tracks.selection;

import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.gui.genovis.VisualizationTrack;
import gedi.gui.genovis.style.StyleObject;
import gedi.gui.genovis.style.StylePalette;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;

public class RegionSelectionModel {

	public enum RegionSelectionMode {
		None(0), Single(1), Multiple(Integer.MAX_VALUE);
		
		private int maxSelected;
		private RegionSelectionMode(int maxSelected) {
			this.maxSelected = maxSelected;
		}
	}
	
	private LinkedHashMap<ImmutableReferenceGenomicRegion<? extends VisualizationTrack>,StyleObject> selection = new LinkedHashMap<ImmutableReferenceGenomicRegion<? extends VisualizationTrack>, StyleObject>();
	private int maxSelected = Integer.MAX_VALUE;
	private StylePalette stylePalette = new StylePalette();
	
	private ArrayList<RegionSelectionListener> listeners = new ArrayList<RegionSelectionListener>();
	
	public void setStylePalette(StylePalette stylePalette) {
		this.stylePalette = stylePalette;
	}
	
	
	public void setSelectionMode(RegionSelectionMode mode) {
		setMaxSelected(mode.maxSelected);
	}
	
	public void setMaxSelected(int maxSelected) {
		this.maxSelected = maxSelected;
		removeIfNecessary(0);
	}
	
	private void removeIfNecessary(int add) {
		if (selection.size()+add>maxSelected) {
			Iterator<ImmutableReferenceGenomicRegion<? extends VisualizationTrack>> it = selection.keySet().iterator();
			while (it.hasNext() && selection.size()>=maxSelected) {
				ImmutableReferenceGenomicRegion<? extends VisualizationTrack> rgr = it.next();
				it.remove();
				fire(new RegionSelectionEvent(false, rgr));
			}
		}		
	}

	public void toggle(ImmutableReferenceGenomicRegion<? extends VisualizationTrack> rgr) {
		if (rgr==null || rgr.getReference()==null || rgr.getRegion()==null) return;
		if (selection.containsKey(rgr))
			deselect(rgr);
		else
			select(rgr);
	}
	
	public void select(ImmutableReferenceGenomicRegion<? extends VisualizationTrack> rgr) {
		if (rgr==null || rgr.getReference()==null || rgr.getRegion()==null) return;
		if (selection.containsKey(rgr)) return;
		removeIfNecessary(1);
		selection.put(rgr, stylePalette.get(selection.size()));
		fire(new RegionSelectionEvent(true, rgr));
	}
	
	public void deselect(ImmutableReferenceGenomicRegion<? extends VisualizationTrack> rgr) {
		if (rgr==null || rgr.getReference()==null || rgr.getRegion()==null) return;
		if (selection.remove(rgr)!=null)
			fire(new RegionSelectionEvent(false, rgr));
	}
	
	public boolean isSelected(ImmutableReferenceGenomicRegion<?> rgr) {
		return selection.containsKey(rgr);
	}
	
	public StyleObject getStyle(ImmutableReferenceGenomicRegion<?> rgr) {
		return selection.get(rgr);
	}
	
	
	public ImmutableReferenceGenomicRegion<?> getRecentSelection() {
		return EI.wrap(selection.keySet()).last();
	}
	
	public ExtendedIterator<ImmutableReferenceGenomicRegion<?>> ei() {
		return (ExtendedIterator)EI.wrap(selection.keySet());
	}
	
	public void addListener(RegionSelectionListener l) {
		listeners.add(l);
	}
	
	private void fire(RegionSelectionEvent e) {
		for (RegionSelectionListener m : listeners)
			m.regionSelection(e);
	}
	
	@FunctionalInterface
	public interface RegionSelectionListener {
		void regionSelection(RegionSelectionEvent e);
	}
	
	public class RegionSelectionEvent {
		private boolean added;
		private ImmutableReferenceGenomicRegion<? extends VisualizationTrack> rgr;
		public RegionSelectionEvent(
				boolean added,
				ImmutableReferenceGenomicRegion<? extends VisualizationTrack> rgr) {
			super();
			this.added = added;
			this.rgr = rgr;
		}
		public boolean isSelected() {
			return added;
		}
		public boolean isDeselected() {
			return !added;
		}
		public ImmutableReferenceGenomicRegion<? extends VisualizationTrack> getElement() {
			return rgr;
		}
		
		public RegionSelectionModel getModel() {
			return RegionSelectionModel.this;
		}
	}
	
}
