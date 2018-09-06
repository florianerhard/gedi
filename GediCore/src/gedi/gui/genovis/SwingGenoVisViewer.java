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

import gedi.core.data.annotation.CompositeReferenceSequenceLengthProvider;
import gedi.core.data.annotation.CompositeReferenceSequencesProvider;
import gedi.core.data.annotation.ReferenceSequenceLengthProvider;
import gedi.core.data.annotation.ReferenceSequencesProvider;
import gedi.core.data.mapper.GenomicRegionDataMappingJob;
import gedi.core.reference.LazyGenome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.util.FileUtils;
import gedi.util.functions.EI;
import gedi.util.gui.PixelBasepairMapper;
import gedi.util.job.PetriNet;
import gedi.util.job.Transition;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import javax.swing.ToolTipManager;

import org.freehep.graphicsbase.util.export.ExportDialog;
import org.freehep.graphicsbase.util.export.ExportFileType;
import org.freehep.graphicsio.DummyGraphics2D;

@SuppressWarnings({ "rawtypes", "serial", "unchecked", "unused" })
public class SwingGenoVisViewer extends JPanel implements GenoVisViewer, Scrollable {

	private static final Logger log = Logger.getLogger( SwingGenoVisViewer.class.getName() );
	

	private static final double EMPTY_HEIGHT = 1;
	private ArrayList<VisualizationTrack<?,?>> tracks = new ArrayList<VisualizationTrack<?,?>>();
	private TracksDataManager dataManager;
	private PixelBasepairMapper xMapper = new PixelBasepairMapper();
	
	private ReferenceSequence[] reference;
	private GenomicRegion[] region;
	private double leftMarginWidth;
	private LazyGenome genome;
	
	private BufferedImage renderBuffer;
	private boolean renderBufferDirty = true;
	
	private ArrayList<Consumer<GenoVisViewer>> prepaintListener = new ArrayList<Consumer<GenoVisViewer>>();
	private ArrayList<Consumer<GenoVisViewer>> reloadListener = new ArrayList<Consumer<GenoVisViewer>>();
	
	private HashMap<VisualizationTrackPickInfo.TrackEventType,ArrayList<Consumer<VisualizationTrackPickInfo>>> mouseListener = new HashMap<VisualizationTrackPickInfo.TrackEventType, ArrayList<Consumer<VisualizationTrackPickInfo>>>();
	private boolean screenshotMode = false;
	
	
	public SwingGenoVisViewer(PetriNet dataPipeline) {
		this(new TracksDataManager(dataPipeline),false);
	}
	
	/**
	 * Screenshot mode: set hysteresis to 0, disable reload after resize, all setlocation calls are made synchronous and waitForReadyToRender is called after setLocation
	 * @param dataPipeline
	 * @param screenshotMode
	 */
	public SwingGenoVisViewer(PetriNet dataPipeline, boolean screenshotMode) {
		this(new TracksDataManager(dataPipeline),screenshotMode);
	}
	
	
	ComponentAdapter reloadCompListener = new ComponentAdapter() {
		@Override
		public void componentResized(ComponentEvent e) {
			if (region!=null && reference!=null) {
				reload();
			}
		}
	};
	public void setScreenshotMode(boolean screenshotMode) {
		boolean wasscreen = this.screenshotMode;
		this.screenshotMode = screenshotMode;
		
		dataManager.setHysteresis(screenshotMode?0:200);

		if (screenshotMode && !wasscreen) removeComponentListener(reloadCompListener);
		else if (!screenshotMode && wasscreen) addComponentListener(reloadCompListener);
	}
	
	public SwingGenoVisViewer(TracksDataManager dataManager, boolean screenshotMode) {
		this.dataManager = dataManager;
		this.screenshotMode = screenshotMode;
		
		if (screenshotMode)
			dataManager.setHysteresis(0);
		else 
			addComponentListener(reloadCompListener);
			// this reloads potential additional data, when the window has been made bigger (as the information may have been hidden due to setMinBpPerPixel or so)
			// this leads to deadlocks with waitUntil when using this without display to generate screenshots
			
		this.genome = inferGenome();
		
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode()==KeyEvent.VK_F1){
					System.out.println(getGenomicLocationAsString());
				}
				if (e.getKeyCode()==KeyEvent.VK_F5){
					ExportDialog export = new ExportDialog();
					export.showExportDialog(SwingGenoVisViewer.this, "Save view as ...", SwingGenoVisViewer.this, getGenomicLocationAsString().replace(':', '-')+".png");
//			        try {
//						ImageIO.write(getImage(),"png",new File(getGenomicLocationAsString().replace(':', '-')+".png"));
//					} catch (IOException e1) {
//						e1.printStackTrace();
//					}
				}
			}
		});
		
		
		MouseAdapter ma = new MouseAdapter() {
			private Point mouseIn = null;
			private Object overObject = null;
			private Object overTrack = null;
			
			@Override
			public void mousePressed(MouseEvent e) {
				VisualizationTrackPickInfo<Object> pi = pick(e.getX(), e.getY(), null);
				if (fire(pi, VisualizationTrackPickInfo.TrackEventType.Down))
					e.consume();
			}
			
			@Override
			public void mouseReleased(MouseEvent e) {
				VisualizationTrackPickInfo<Object> pi = pick(e.getX(), e.getY(), null);
				if (fire(pi, VisualizationTrackPickInfo.TrackEventType.Up))
					e.consume();
			}
			
			
			
			@Override
			public void mouseClicked(MouseEvent e) {
				VisualizationTrackPickInfo.TrackEventType type = VisualizationTrackPickInfo.TrackEventType.Clicked;
				if (e.getClickCount()>1)
					type = VisualizationTrackPickInfo.TrackEventType.DoubleClicked;
				if (e.getButton()==MouseEvent.BUTTON3)
					type = VisualizationTrackPickInfo.TrackEventType.RightClicked;
				VisualizationTrackPickInfo<Object> pi = pick(e.getX(), e.getY(), null);
				if (fire(pi,type ))
					e.consume();
			}
			
			@Override
			public void mouseMoved(MouseEvent e) {
				mouseIn = new Point(e.getX(), e.getY());
				VisualizationTrackPickInfo<Object> pi = pick(e.getX(), e.getY(), null);
				
				if (overObject!=null) {
					if (pi.getData()!=null) {
						if (!overObject.equals(pi.getData())) {
							if (fire(pi.substitute(overObject), VisualizationTrackPickInfo.TrackEventType.ExitedObject))
								e.consume();
							if (fire(pi, VisualizationTrackPickInfo.TrackEventType.EnteredObject))
								e.consume();
							if (fire(pi, VisualizationTrackPickInfo.TrackEventType.Moved))
								e.consume();
						} else {
							if (fire(pi, VisualizationTrackPickInfo.TrackEventType.Moved))
								e.consume();
						}
					} else {
						if (fire(pi.substitute(overObject), VisualizationTrackPickInfo.TrackEventType.ExitedObject))
							e.consume();
						if (fire(pi, VisualizationTrackPickInfo.TrackEventType.Moved))
							e.consume();
					}
				} else {
					if (pi.getData()!=null) {
						if (fire(pi, VisualizationTrackPickInfo.TrackEventType.EnteredObject))
							e.consume();
						if (fire(pi, VisualizationTrackPickInfo.TrackEventType.Moved))
							e.consume();
					} else {
						if (fire(pi, VisualizationTrackPickInfo.TrackEventType.Moved))
							e.consume();
					}
				}
				
				overObject = pi.getData();
				
				
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
				mouseIn = new Point(e.getX(), e.getY());
				VisualizationTrackPickInfo<Object> pi = pick(e.getX(), e.getY(), null);
				if (fire(pi, VisualizationTrackPickInfo.TrackEventType.Dragged))
					e.consume();
			}

			
		};
		addMouseListener(ma);
		addMouseMotionListener(ma);
		addMouseWheelListener(ma);
		
		GenoVisZoomAndPanListener inter = new GenoVisZoomAndPanListener(this);
		addMouseListener(inter);
		addMouseMotionListener(inter);
		addMouseWheelListener(inter);
		
		
		setFocusable(true);
		requestFocusInWindow();
	}
	
	
	public PetriNet getPetriNet() {
		return dataManager.getDataPipeline();
	}
	
	public void setPetriNet(PetriNet pn) {
		dataManager = new TracksDataManager(pn);

		tracks.clear();
		for (VisualizationTrack t : EI.wrap(pn.getTransitions()).map(t->((GenomicRegionDataMappingJob)t.getJob()).getMapper()).instanceOf(VisualizationTrack.class).loop())
			addTrack(t);
		relayout();
		reload();
	}
	
	/**
	 * Returns whether to cancel events
	 * @param x
	 * @param y
	 * @param type
	 * @return
	 */
	protected boolean fire(
			VisualizationTrackPickInfo<Object> pi, VisualizationTrackPickInfo.TrackEventType type) {
		
		List<Consumer<VisualizationTrackPickInfo>> ll = getListener(type);
		Function<VisualizationTrack, List<Consumer<VisualizationTrackPickInfo>>> t2ll = t->t.getListener(type);
		
		pi.setType(type);
		
		if (pi.getTrack()!=null) 
			for (Consumer<VisualizationTrackPickInfo> l : t2ll.apply(pi.getTrack())) {
				l.accept(pi);
				if (pi.isConsumed()) return true;
			}
		
		for (Consumer<VisualizationTrackPickInfo> l : ll) {
			l.accept(pi);
			if (pi.isConsumed()) return true;
		}
		
		return false;
	}
	
	@Override
	public void showToolTip(String text) {
		ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);
		setToolTipText(text);
	}

	public String getGenomicLocationAsString() {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<getReference().length; i++) {
			if (i>0) sb.append("-");
			sb.append(getReference()[0]+":"+getRegion()[0]);
		}
		return sb.toString();
	}
	
	
	private LazyGenome inferGenome() {
		CompositeReferenceSequenceLengthProvider lengths = new CompositeReferenceSequenceLengthProvider();
		CompositeReferenceSequencesProvider refs = new CompositeReferenceSequencesProvider();
		for (Transition t : dataManager.getDataPipeline().getTransitions()) {
			if (t.getJob() instanceof GenomicRegionDataMappingJob) {
				GenomicRegionDataMappingJob<?,?> j = (GenomicRegionDataMappingJob<?,?>) t.getJob();
				j.getMapper().applyForAll(ReferenceSequencesProvider.class, p->refs.add(p));
				j.getMapper().applyForAll(ReferenceSequenceLengthProvider.class, p->lengths.add(p));
			}
		}
		
		return new LazyGenome(refs,lengths);
	}
	
	public void setGenome(LazyGenome genome) {
		this.genome = genome;
	}
	
	@Override
	public void addReloadListener(Consumer<GenoVisViewer> l) {
		reloadListener.add(l);
	}
	@Override
	public void addPrepaintListener(Consumer<GenoVisViewer> l) {
		prepaintListener.add(l);
	}
	
	public LazyGenome getGenome() {
		return genome;
	}

	@Override
	public double getLeftMarginWidth() {
		return leftMarginWidth;
	}
	
	public TracksDataManager getDataManager() {
		return dataManager;
	}
	
	@Override
	public void setLocation(ReferenceSequence[] reference, GenomicRegion[] region, boolean async) {
		if (screenshotMode) async = false;
//		if (region.getStart()<0) region = region.intersect(new ArrayGenomicRegion(0,region.getEnd()));
		log.log(Level.FINE,"Set location "+EI.seq(0, reference.length).map(i->reference[i]+":"+region[i]).concat(";"));
		this.reference = reference;
		this.region = region;
		
		for (VisualizationTrack<?,?> t : tracks)
			t.setView(reference, region);
		renderBufferDirty = true;
		
		if (async) 
			reload();
		else {
			doLayout();
			Object lock = new Object();
			synchronized (lock) {
				if (smartLayoutTracks.size()>0)
					dataManager.setLocation(xMapper,reference, region,(c)->dataManager.setLocation(xMapper,reference, region,(cc)->{
						synchronized (lock) {
							lock.notify();
						}
					}));
				else
					dataManager.setLocation(xMapper,reference, region,(c)->{
						synchronized (lock) {
							lock.notify();
						}
					});
				
				try {
					lock.wait();
				} catch (InterruptedException e) {
				}
				for (Consumer<GenoVisViewer> l : reloadListener)
					l.accept(this);	
			}
			
		}
		
		if (screenshotMode) 
			waitForReadyToRender(true);
	}
	
	
	@Override
	public void reload() {
		doLayout();
		
		if (smartLayoutTracks.size()>0)
			dataManager.setLocation(xMapper,reference, region,(cc)->dataManager.setLocation(xMapper,reference, region,(c)->relayout()));
		else
			dataManager.setLocation(xMapper,reference, region,(c)->relayout());
		repaint();
		for (Consumer<GenoVisViewer> l : reloadListener)
			l.accept(this);
	}

	private ArrayList<VisualizationTrack<?,?>> smartLayoutTracks = new ArrayList<VisualizationTrack<?,?>>();
	private boolean needlayout;
	private void updateMapper(double maxLeft) {
		if (region!=null) {
			GenomicRegion[] reg = region;
			
			if (smartLayoutTracks.size()>0) {
				GenomicRegion[] smart = null;
				for (VisualizationTrack<?,?> t : smartLayoutTracks) {
					GenomicRegion[] tsmart = t.getSmartRegions();
					if (tsmart!=null) {
						if (smart==null) smart = tsmart;
						else for (int i=0; i<tsmart.length; i++)
							smart[i] = smart[i].union(tsmart[i]);
					}
				}
				if (smart!=null) {
					reg = smart;
				}
			}

			xMapper.setIntronFixed(reference,false,reg,5,getWidth()-maxLeft);
		}
	}
	
	public void addSmartTrack(VisualizationTrack<?,?> t) {
		smartLayoutTracks.add(t);
	}
	public void removeSmartTrack(VisualizationTrack<?,?> t) {
		smartLayoutTracks.remove(t);
	}
	

	public void addTrack(VisualizationTrack<?,?> track) {
		tracks.add(track);
		track.setGenoVis(this);
		relayout();
	}
	
	
	public void insertTrack(VisualizationTrack<?,?> track, VisualizationTrack<?,?> after) {
		tracks.add(tracks.indexOf(after)+1,track);
		invalidate();
		validate();
	}
	
	public ArrayList<VisualizationTrack<?, ?>> getTracks() {
		ArrayList<VisualizationTrack<?, ?>> re = new ArrayList<VisualizationTrack<?,?>>();
		Stack<VisualizationTrack<?, ?>> dfs = new Stack<VisualizationTrack<?,?>>();
		for (int i=tracks.size()-1; i>=0; i--) 
			dfs.add(tracks.get(i));
		
		while (!dfs.isEmpty()) {
			VisualizationTrack<?, ?> t = dfs.pop();
			re.add(t);
			if (t instanceof HasSubtracks) {
				List<? extends VisualizationTrack> subs = ((HasSubtracks)t).getSubTracks();
				for (int i=subs.size()-1; i>=0; i--)
					dfs.add(subs.get(i));
			}
		}
		return re;
	}
	
	
	@Override
	public void doLayout() {
		super.doLayout();
		renderBufferDirty = true;
		// determine left region
		leftMarginWidth = 0;
		for (VisualizationTrack<?,?> t : tracks) {
			if (isToDraw(t))
				leftMarginWidth = Math.max(leftMarginWidth,t.getLeftMarginWidth());
		}
		
		updateMapper(leftMarginWidth);
		
		// set bounds for all tracks
		double y = 0;
		for (VisualizationTrack<?,?> t : tracks) {
			if (t.isHidden()) continue;
			
			double h = (t.isVisible() && !(t.isAutoHide() && t.isDataEmpty()))?t.getPrefHeight():EMPTY_HEIGHT;
			t.setBounds(new Rectangle2D.Double(leftMarginWidth,y,Math.max(0, getWidth()-leftMarginWidth),h));
			y+=h;
		}
		
		setPreferredSize(new Dimension(getWidth(),(int)y));
	}
	
	
	private boolean isToDraw(VisualizationTrack<?, ?> t) {
		return !t.isHidden() && !(t.isAutoHide() && t.isDataEmpty()) && t.isVisible() ;
	}
	
	@Override
	public void repaint(boolean clearBuffer) {
		if (clearBuffer)
			renderBufferDirty = true;
		super.repaint();
	}
	

	@Override
	protected void paintComponent(Graphics g) {
		if (renderBufferDirty) {
			if (renderBuffer==null || getWidth()!=renderBuffer.getWidth() || getHeight()!=renderBuffer.getHeight())
				renderBuffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2 = renderBuffer.createGraphics();
			render(g2);
			g2.dispose();
			renderBufferDirty = false;
		}
		Graphics2D g2 = (Graphics2D) g;
		g2.drawImage(renderBuffer, 0, 0, null);
		
//		if (mouseIn!=null) {
//			g2.setColor(Color.gray);
//			g2.draw(new Line2D.Double(mouseIn.getX(),0,mouseIn.getX(),getHeight()));
//		}
		// need to always repaint for that... 
	}
	
	public void render(Graphics2D g2) {
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		
		Shape clipper = g2.getClip();
		Rectangle2D clip = g2.getClipBounds();
		
		
		g2.setPaint(Color.gray);
		g2.fill(getBounds());
		
		for (Consumer<GenoVisViewer> l : prepaintListener)
			l.accept(this);
		
		for (VisualizationTrack<?,?> t : tracks)
			if (t.isVisible() && !t.isHidden())
				t.prepaint(g2);
		
		for (VisualizationTrack<?,?> t : tracks) {
			if (t.isHidden()) continue;
			
			if (t.isAutoHide() && t.isDataEmpty())
				continue;
			
			if (t.isVisible()){
				Rectangle2D nc = t.getLeftMarginWidth()>0?t.getBoundsWithMargin():t.getBounds();
				if(clip!=null)
					Rectangle2D.intersect(clip, nc, nc);
				g2.setClip(nc);
				t.paint(g2);
			} else {
				g2.setPaint(Color.gray);
				g2.fill(t.getBounds());
			}
		}
		
		g2.setClip(clipper);
	}

	/**
	 * 
	 * @param filename
	 * @return the actual height that has been used to render the view
	 * @throws IOException
	 */
	public double renderToFile(String filename) throws IOException {
		double height = 0;
		for (VisualizationTrack<?,?> t : tracks) {
			if (isToDraw(t)) {
				while (!t.isUptodate())
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
					}
				height+=t.getPrefHeight();
			}
		}
		
		File file = new File(filename);
		for (ExportFileType type : ExportFileType.getExportFileTypes(FileUtils.getExtension(file))) {
			if (type.fileHasValidExtension(file)) {
				Properties p = new Properties();
				
				needlayout = true;
				while (needlayout) {
					height = 0;
					for (VisualizationTrack<?,?> t : tracks) {
						if (isToDraw(t)) {
							while (!t.isUptodate())
								try {
									Thread.sleep(10);
								} catch (InterruptedException e) {
								}
							height+=t.getPrefHeight();
						}
					}
					setSize(getWidth(), (int)height);
					needlayout = false;
					type.exportToFile(file, this, getParent(), p, "Gedi");
					doLayout();
				}
			}
		}
		return height;
	}
	
	
	public double waitForReadyToRender(boolean setPreferredHeight) {
		double height = 0;
		for (VisualizationTrack<?,?> t : tracks) {
			if (isToDraw(t)) {
				while (!t.isUptodate())
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
					}
				height+=t.getPrefHeight();
			}
		}
		
		DummyGraphics2D g = new DummyGraphics2D(getSize(), false);
		
		needlayout = true;
		while (needlayout) {
			height = 0;
			for (VisualizationTrack<?,?> t : tracks) {
				if (isToDraw(t)) {
					while (!t.isUptodate())
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
						}
					height+=t.getPrefHeight();
				}
			}
			setSize(getWidth(), (int)height);
			needlayout = false;
			render(g);
			doLayout();
		}
		if (setPreferredHeight)
			setPreferredSize(new Dimension(getWidth(),(int)Math.ceil(height)));
		return height;
	}
	
	
	@Override
	public void print(Graphics g) {
		render((Graphics2D) g);
	}
	
	public BufferedImage getImage() {
		double height = 0;
		for (VisualizationTrack<?,?> t : tracks) {
			if (isToDraw(t)) {
				while (!t.isUptodate())
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
					}
				height+=t.getPrefHeight();
			}
		}
		
		needlayout = true;
		BufferedImage re = null;
		while (needlayout) {
			height = 0;
			for (VisualizationTrack<?,?> t : tracks) {
				if (isToDraw(t)) {
					while (!t.isUptodate())
						try {
							Thread.sleep(10);
						} catch (InterruptedException e) {
						}
					height=Math.max(height, t.getBounds().getMaxY());
				}
//				System.out.println("dont draw "+t+" "+t.isUptodate()+" "+t.isHidden()+" "+t.isDataEmpty()+" "+t.isVisible()+" "+t.getBounds());
			}
			
			re = new BufferedImage(getWidth(), (int) height, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2 = re.createGraphics();
			needlayout = false;
			paintComponent(g2);
			doLayout();
			setSize(getPreferredSize());
			g2.dispose();
		}
		
		return re;
	}

	@Override
	public void relayout() {
		needlayout = true;
		renderBufferDirty = true;
		revalidate();
		repaint();
	}

	@Override
	public PixelBasepairMapper getLocationMapper() {
		return xMapper;
	}
	
	@Override
	public ReferenceSequence[] getReference() {
		return reference;
	}
	
	@Override
	public GenomicRegion[] getRegion() {
		return region;
	}
	
	@Override
	public double getScreenWidth() {
		return getWidth();
	}

	@Override
	public <D> VisualizationTrackPickInfo<D> pick(double x, double y, VisualizationTrackPickInfo<D> re) {
		if (re==null) re = new VisualizationTrackPickInfo<D>();
		re.setup(x, y, getLocationMapper().pixelToBp(x-getLeftMarginWidth()), getLocationMapper().pixelToReferenceSequence(x-getLeftMarginWidth()));
		for (VisualizationTrack t : tracks) {
			if (isToDraw(t)){
				if (t.getBoundsWithMargin().contains(x, y)) {
					re.setTrack(t);
					t.pick(re);
					break;
				}
			}
		}
		
		return re;
	}

	@Override
	public <P> void addListener(Consumer<VisualizationTrackPickInfo<P>> l, VisualizationTrackPickInfo.TrackEventType...catchType) {
		for (VisualizationTrackPickInfo.TrackEventType t : catchType)
			mouseListener.computeIfAbsent(t, tet->new ArrayList<>()).add((Consumer)l);
	}
	
	private List<Consumer<VisualizationTrackPickInfo>> getListener(VisualizationTrackPickInfo.TrackEventType type) {
		ArrayList<Consumer<VisualizationTrackPickInfo>> re = mouseListener.get(type);
		return re==null?Collections.emptyList():re;
	}

	@Override
	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect,
			int orientation, int direction) {
		return 50;
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect,
			int orientation, int direction) {
		return 50;
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		return true;
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {
		return false;
	}
	
	
}

