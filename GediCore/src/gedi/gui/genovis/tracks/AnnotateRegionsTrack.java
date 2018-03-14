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

import gedi.core.data.annotation.NameAnnotation;
import gedi.core.data.annotation.NameProvider;
import gedi.core.data.annotation.ScoreNameAnnotation;
import gedi.core.data.annotation.ScoreProvider;
import gedi.core.data.mapper.GenomicRegionDataMapping;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.gui.genovis.SwingGenoVisViewer;
import gedi.gui.genovis.VisualizationTrackPickInfo;
import gedi.gui.genovis.VisualizationTrackPickInfo.TrackEventType;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.io.text.tsv.formats.Bed;
import gedi.util.nashorn.JSConsumer;
import gedi.util.nashorn.JSFunction;
import gedi.util.nashorn.JSPredicate;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.script.ScriptException;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.filechooser.FileFilter;


@GenomicRegionDataMapping(fromType=IntervalTree.class)
public class AnnotateRegionsTrack extends PackRegionTrack<NameProvider> {

	private MemoryIntervalTreeStorage<? extends NameProvider> storage;

	private BiFunction<String,Double,NameProvider> factory;
	private BiConsumer<String,NameProvider> changer;
	
	private boolean locationChangeAllowed = true;
	private Consumer<ReferenceGenomicRegion<NameProvider>> clickAction;
	
	@Override
	public boolean isAutoHide() {
		return false;
	}
	
	public void setLocationChangeAllowed(boolean locationChangeAllowed) {
		this.locationChangeAllowed = locationChangeAllowed;
	}
	
	public boolean isLocationChangeAllowed() {
		return locationChangeAllowed;
	}
	
	public void setClickAction(String js) throws ScriptException {
		this.clickAction = new JSConsumer<>(true, js);
	}
	

	public AnnotateRegionsTrack(MemoryIntervalTreeStorage<? extends NameProvider> storage, Strand strand) {
		this.storage = storage;
		if (storage.getType()==NameAnnotation.class) {
			factory = (n,s)->new NameAnnotation(n);
			changer = (s,a)->((NameAnnotation) a).setName(s);
		} else if (storage.getType()==ScoreNameAnnotation.class) {
			factory = (n,s)->new ScoreNameAnnotation(n,s);
			changer = (s,a)->((ScoreNameAnnotation) a).setName(s);
		}
		else throw new RuntimeException(storage.getType()+" is not supported!");
		setStrand(strand);
		
		Consumer<VisualizationTrackPickInfo<ReferenceGenomicRegion<NameProvider>>> l = new Consumer<VisualizationTrackPickInfo<ReferenceGenomicRegion<NameProvider>>>() {
			
			MutableReferenceGenomicRegion<NameProvider> annot;
			boolean drag = false;
			int startbp;
			@Override
			public void accept(
					VisualizationTrackPickInfo<ReferenceGenomicRegion<NameProvider>> t) {
				
				if (t==null) return;
				ReferenceSequence ref = t.getReference().toStrand(fixedStrand);
				if (t.getType()==TrackEventType.Down && isLocationChangeAllowed()) {
					
					drag = false;
					if (isRight(t)) {
						annot = t.getData().toMutable().toStrand(fixedStrand);
						startbp = annot.getRegion().getStart();
						((SwingGenoVisViewer)viewer).setCursor(Cursor.getDefaultCursor());
					} else if (isLeft(t)) {
						annot = t.getData().toMutable().toStrand(fixedStrand);
						startbp = annot.getRegion().getStop();
						((SwingGenoVisViewer)viewer).setCursor(Cursor.getDefaultCursor());
					} else if (t.getData()!=null && t.getData().getRegion()!=null) {
						annot = t.getData().toMutable().toStrand(fixedStrand);
						drag = true;
						startbp = t.getData().induce(t.getBp());
					} else {
						annot = new MutableReferenceGenomicRegion<NameProvider>();
						annot.set(ref, new ArrayGenomicRegion(t.getBp(),t.getBp()),factory.apply("NEW",0.0));
						startbp = t.getBp();
					}
				}
				else if (t.getType()==TrackEventType.Clicked && clickAction!=null) {
					MutableReferenceGenomicRegion<NameProvider> pick = t.getData().toMutable().toStrand(fixedStrand);
					if (pick!=null)
						clickAction.accept(pick);
				}
				else if (t.getType()==TrackEventType.Dragged && annot!=null) {
					if (drag) {
						storage.remove(annot.getReference(), annot.getRegion());
						getData(t.getReference()).getData().remove(annot.getRegion());
						
						int bp = annot.getRegion().map(startbp);
						int diff = t.getBp()-bp;
						annot.setRegion(annot.getRegion().translate(diff));
						
						storage.add((ReferenceGenomicRegion) annot);
						getData(t.getReference()).getData().put(annot.getRegion(),annot.getData());
					}
					else if (ref.equals(annot.getReference())) {
						storage.remove(annot.getReference(), annot.getRegion());
						getData(t.getReference()).getData().remove(annot.getRegion());
						
						annot.setRegion(new ArrayGenomicRegion(Math.min(startbp,t.getBp()),Math.max(startbp,t.getBp())));
						if (!annot.getRegion().isEmpty()) {
							storage.add((ReferenceGenomicRegion)annot);
							getData(t.getReference()).getData().put(annot.getRegion(),annot.getData());
						}
					}
				}
				else if (t.getType()==TrackEventType.Moved && isLocationChangeAllowed()) {
					if (t.getData()!=null) {
						if (isLeft(t)) 
							((SwingGenoVisViewer)viewer).setCursor(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));						
						else if (isRight(t)) 
							((SwingGenoVisViewer)viewer).setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));						
						else
							((SwingGenoVisViewer)viewer).setCursor(Cursor.getDefaultCursor());
					}
					
				}
				else if (t.getType()==TrackEventType.DoubleClicked) {
					if (t.getData()!=null) {
						String name = JOptionPane.showInputDialog("Enter name", t.getData().getData().getName());
						if (name!=null)	changer.accept(name, t.getData().getData());
					}
				}
				else if (t.getType()==TrackEventType.RightClicked) {
					
					createPopup().show((SwingGenoVisViewer)viewer, (int)t.getPixelX(), (int)t.getPixelY());
				}
				else if (t.getType()==TrackEventType.Up) {
					annot = null;
				}
				
				
				viewer.relayout();
				viewer.repaint(true);
				t.consume();
			}
			
			private boolean isRight(
					VisualizationTrackPickInfo<ReferenceGenomicRegion<NameProvider>> t) {
				return t.getData()!=null && t.getData().getRegion()!=null && Math.abs(t.getPixelX()-viewer.getLeftMarginWidth()-viewer.getLocationMapper().bpToPixel(t.getReference(), t.getData().getRegion().getEnd()))<5;
			}
			private boolean isLeft(
					VisualizationTrackPickInfo<ReferenceGenomicRegion<NameProvider>> t) {
				return t.getData()!=null && t.getData().getRegion()!=null && Math.abs(t.getPixelX()-viewer.getLeftMarginWidth()-viewer.getLocationMapper().bpToPixel(t.getReference(), t.getData().getRegion().getStart()))<5;
			}
		};
		addListener(l, 
				VisualizationTrackPickInfo.TrackEventType.Down, 
				VisualizationTrackPickInfo.TrackEventType.Up, 
				VisualizationTrackPickInfo.TrackEventType.DoubleClicked, 
				VisualizationTrackPickInfo.TrackEventType.Dragged,
				VisualizationTrackPickInfo.TrackEventType.RightClicked,
				VisualizationTrackPickInfo.TrackEventType.Clicked,
				VisualizationTrackPickInfo.TrackEventType.Moved
				);
		
	}
	
	
	@Override
	public boolean isVisible() {
		return true;
	}
	
	@Override
	public TrackRenderContext<IntervalTree<GenomicRegion, NameProvider>> renderEnd(
			TrackRenderContext<IntervalTree<GenomicRegion, NameProvider>> context) {
		
		double h = context.<Double>get(MAX_HEIGHT)+20;
		context.putValue(MAX_HEIGHT, h);
		
		return super.renderEnd(context);
	}
	
	private String currentFile = null;
	
	private JPopupMenu createPopup() {
		JPopupMenu men = new JPopupMenu();
		
		men.add(createMenItem("Clear",storage.size()>0,()->{
			int re = JOptionPane.showConfirmDialog((Component)viewer, "Really clear all annotations?", "Clear", JOptionPane.WARNING_MESSAGE);
			if (re==JOptionPane.YES_OPTION) {
				storage.clear();
				viewer.reload();
			}
		}));
		men.addSeparator();
		men.add(createMenItem("Load...",true,()->{
			JFileChooser cho = new JFileChooser(System.getProperty("user.dir"));
			cho.setDialogTitle("Load "+getId());
			cho.setFileFilter(new FileFilter() {
				
				@Override
				public String getDescription() {
					return "BED files";
				}
				
				@Override
				public boolean accept(File f) {
					return f.getPath().toLowerCase().endsWith(".bed");
				}
			});
			if (cho.showOpenDialog((Component)viewer)==JFileChooser.APPROVE_OPTION) {
				try {
					Bed.iterateScoreNameEntries(cho.getSelectedFile().getPath()).map(r->new ImmutableReferenceGenomicRegion<NameProvider>(r.getReference(), r.getRegion(), factory.apply(r.getData().getName(),r.getData().getScore())))
						.forEachRemaining(r->storage.add((ReferenceGenomicRegion)r));
					viewer.reload();
					currentFile = cho.getSelectedFile().getPath();
				} catch (IOException e) {
					throw new RuntimeException("Could not load file "+cho.getSelectedFile());
				}
			}
		}));
		men.add(createMenItem("Save as...",true,()->{
			JFileChooser cho = new JFileChooser(System.getProperty("user.dir"));
			cho.setDialogTitle("Save "+getId());
			cho.setFileFilter(new FileFilter() {
				
				@Override
				public String getDescription() {
					return "BED files";
				}
				
				@Override
				public boolean accept(File f) {
					return f.getPath().toLowerCase().endsWith(".bed");
				}
			});
			if (cho.showSaveDialog((Component)viewer)==JFileChooser.APPROVE_OPTION) {
				try {
					currentFile = cho.getSelectedFile().getPath();
					Bed.save(currentFile,storage.ei().map(m->new ImmutableReferenceGenomicRegion<ScoreNameAnnotation>(m.getReference(), m.getRegion(), new ScoreNameAnnotation(m.getData().getName(),m.getData() instanceof ScoreProvider?((ScoreProvider)m.getData()).getScore():0))));
					JOptionPane.showMessageDialog((Component)viewer, "Saved as "+currentFile);
				} catch (IOException e) {
					throw new RuntimeException("Could not save to file "+cho.getSelectedFile(),e);
				}
			}
		}
		));
		men.add(createMenItem("Save",currentFile!=null,()->{
			try {
				Bed.save(currentFile,storage.ei().map(m->new ImmutableReferenceGenomicRegion<ScoreNameAnnotation>(m.getReference(), m.getRegion(), new ScoreNameAnnotation(m.getData().getName(),m.getData() instanceof ScoreProvider?((ScoreProvider)m.getData()).getScore():0))));
			} catch (Exception e) {
				throw new RuntimeException("Could not save to file "+currentFile,e);
			}
		}));
		
		return men;
	}


	private JMenuItem createMenItem(String label, boolean enabled, Runnable action) {
		JMenuItem re = new JMenuItem(label);
		re.setEnabled(enabled);
		re.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				action.run();
			}
		});
		return re;
	}
}
