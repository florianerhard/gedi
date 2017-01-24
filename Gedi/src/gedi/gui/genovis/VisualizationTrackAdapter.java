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


import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.util.ArrayUtils;
import gedi.util.PaintUtils;
import gedi.util.StringUtils;
import gedi.util.dynamic.DynamicObject;
import gedi.util.gui.Dimension2DDouble;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;


public abstract class VisualizationTrackAdapter<D,P> implements VisualizationTrack<D,P> {

	protected double minHeight = 20;
	protected double prefHeight = 80;
	protected double maxHeight = Double.POSITIVE_INFINITY;
	protected double minPixelPerBasePair = 0;
	protected double maxPixelPerBasePair = Double.POSITIVE_INFINITY;
	protected double leftMarginWidth = 0;
	protected DynamicObject styles = DynamicObject.getEmpty();

	protected boolean smart = false;
	protected boolean hidden = false;
	
	private HashMap<ReferenceSequence,MutableReferenceGenomicRegion<D>> data = new HashMap<ReferenceSequence, MutableReferenceGenomicRegion<D>>();
	private HashMap<ReferenceSequence,MutableReferenceGenomicRegion<Void>> current = new HashMap<ReferenceSequence, MutableReferenceGenomicRegion<Void>>();
	
	private HashMap<VisualizationTrackPickInfo.TrackEventType,ArrayList<Consumer<VisualizationTrackPickInfo<P>>>> listener = new HashMap<VisualizationTrackPickInfo.TrackEventType, ArrayList<Consumer<VisualizationTrackPickInfo<P>>>>();
	
	
	protected Rectangle2D bounds; // set by layouter
	protected GenoVisViewer viewer;
	
	private String id = getClass().getSimpleName()+":"+StringUtils.getRandomId();
	
	protected TrackRenderContext<D> context = new TrackRenderContext<D>(); 
	
	private Class<D> dataClass;
	
	protected Strand fixedStrand;
	private boolean renderLabels = true;
	private boolean renderLegend = false;
	
	private Paint background;
	private boolean autohide = true;

	
	public VisualizationTrackAdapter(Class<D> dataClass) {
		this.dataClass = dataClass;
		addListener(t->{
			if (getLabelRect().contains(t.getPixelX(),t.getPixelY())) {
					setRenderLegend(!isRenderLegend());
					getViewer().repaint(true);
			}
		}, 
				VisualizationTrackPickInfo.TrackEventType.Clicked
				);
	}
	
	protected boolean isEmptyData(D data) {
		return false;
	}
	
	@Override
	public boolean isDataEmpty() {
		for (MutableReferenceGenomicRegion<D> r : this.data.values())
			if (!isEmptyData(r.getData()))
				return false;
		return true;
	}
	
	@Override
	public GenoVisViewer getViewer() {
		return viewer;
	}
	
	public void setBackground(Paint background) {
		this.background = background;
	}
	
	protected MutableReferenceGenomicRegion<D> getData(ReferenceSequence ref) {
		return data.get(ref);
	}
	
	protected void setData(ReferenceSequence ref, MutableReferenceGenomicRegion<D> data) {
		this.data.put(ref,data);
	}
	
	public MutableReferenceGenomicRegion<Void> getCurrent(ReferenceSequence ref) {
		return current.get(ref);
	}
	
	public void setRenderLabels(boolean renderLabels) {
		this.renderLabels = renderLabels;
	}
	
	@Override
	public void setAutoHide(boolean autohide) {
		this.autohide = autohide;
	}
	@Override
	public boolean isAutoHide() {
		return autohide;
	}
	
	@Override
	public void setSmartLayout(boolean smart) {
		if (smart && !this.smart && viewer!=null)
			viewer.addSmartTrack(this);
		else if (!smart && this.smart && viewer!=null)
			viewer.removeSmartTrack(this);
		this.smart = smart;
	}
	
	@Override
	public GenomicRegion[] getSmartRegions() {
		return null;
	}
	
	
	@Override
	public void setStrand(Strand strand) {
		this.fixedStrand = strand;
	}
	
	@Override
	public void setHidden(boolean hidden) {
		if (this.hidden!=hidden) {
			this.hidden = hidden;
			if (viewer!=null)
				viewer.relayout();
		}
	}
	
	@Override
	public boolean isHidden() {
		return hidden;
	}
	
	public Paint getBackground() {
		return background==null?PaintUtils.parseColor(getStyles().get("background-color").asString("WHITE")):background;
	}
	
	public void setRenderLegend(boolean renderLegend) {
		this.renderLegend = renderLegend;
	}
	
	public boolean isRenderLegend() {
		return renderLegend;
	}
			
	public void setMinPixelPerBasePair(double minPixelPerBasePair) {
		this.minPixelPerBasePair = minPixelPerBasePair;
	}
	
	public void setMaxPixelPerBasePair(double maxPixelPerBasePair) {
		this.maxPixelPerBasePair = maxPixelPerBasePair;
	}
	
	
	public void setMaxBasePairsPerPixel(double maxBasePairPerPixel) {
		this.minPixelPerBasePair = 1/maxBasePairPerPixel;
	}
	
	public void setMinBasePairsPerPixel(double minBasePairPerPixel) {
		this.maxPixelPerBasePair = 1/minBasePairPerPixel;
	}
	
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}
	
	@Override
	public DynamicObject getStyles() {
		return styles;
	}
	@Override
	public void setStyles(DynamicObject styles) {
		this.styles = styles;
	}
	
	@Override
	public double getPrefHeight() {
		return prefHeight;
	}
	
	@Override
	public Rectangle2D getBoundsWithMargin() {
		return new Rectangle2D.Double(getBounds().getX()-getLeftMarginWidth(),getBounds().getY(),getBounds().getWidth()+getLeftMarginWidth(),getBounds().getHeight());
	}

	protected Rectangle2D getLeftMargin() {
		return new Rectangle2D.Double(getBounds().getX()-getLeftMarginWidth(),getBounds().getY(),getLeftMarginWidth(),getBounds().getHeight());
	}

	
	@Override
	public double getLeftMarginWidth() {
		return leftMarginWidth;
	}

	@Override
	public void setBounds(Rectangle2D bounds) {
		if (!bounds.equals(this.bounds)) {
			this.bounds = bounds;
		}
	}
	
	@Override
	public Rectangle2D getBounds() {
		return bounds;
	}

	@Override
	public void addListener(Consumer<VisualizationTrackPickInfo<P>> l, VisualizationTrackPickInfo.TrackEventType...catchType) {
		for (VisualizationTrackPickInfo.TrackEventType t : catchType)
			listener.computeIfAbsent(t, tet->new ArrayList<>()).add(l);
	}
	
	@Override
	public String toString() {
		return id;
	}
	
	@Override
	public List<Consumer<VisualizationTrackPickInfo<P>>> getListener(VisualizationTrackPickInfo.TrackEventType type) {
		ArrayList<Consumer<VisualizationTrackPickInfo<P>>> re = listener.get(type);
		return re==null?Collections.emptyList():re;
	}
	
	@Override
	public void setGenoVis(
			GenoVisViewer viewer) {
		this.viewer = viewer;
		if (smart)
			viewer.addSmartTrack(this);
	}
	
	@Override
	public double getMinHeight() {
		return minHeight;
	}

	@Override
	public double getMaxHeight() {
		return maxHeight;
	}

	private boolean lastTimeVisible = false;
	
	@Override
	public void setView(ReferenceSequence[] reference, GenomicRegion[] region) {
		boolean visPrev = lastTimeVisible;
		
		if (reference.length!=region.length) throw new IllegalArgumentException();
		
		current.clear();
		for (int i=0; i<reference.length; i++) {
			current.put(reference[i], new MutableReferenceGenomicRegion<Void>().set(reference[i],region[i],(Void)null));
		}
		data.keySet().retainAll(current.keySet());
		
//		current.setReference(reference).setRegion(region);
		lastTimeVisible = isVisible();
		if (lastTimeVisible && !visPrev)
			viewer.relayout();
		viewer.repaint();
	}
	
	protected boolean isLocationSet() {
		return current.size()>0;
	}
	
	@Override
	public boolean isVisible() {
		if (getBounds()==null) return false;
		
		double bppp = this.viewer.getLocationMapper().getPixelsPerBasePair();
		boolean re = bppp>=minPixelPerBasePair && bppp<=maxPixelPerBasePair;
		return re;
	}
		
	
	@Override
	public boolean isDisabled() {
		return !isVisible() || isHidden();
	}

	@Override
	public void accept(ReferenceSequence reference, GenomicRegion region, PixelLocationMapping pixelMapping,
			D data) {
		if (current.containsKey(reference) && current.get(reference).getRegion().equals(region)) {
//		if (reference.equals(current.getReference()) && region.equals(current.getRegion())) 
			this.data.put(reference, new MutableReferenceGenomicRegion<D>().set(reference,region, data));
			if (autohide)
				viewer.relayout();
			else
				viewer.repaint();
		}
	}

//	@Override
//	public D getData() {
//		boolean chrEqual = current.getReference().equals(data.getReference());
//		boolean regEqual = current.getRegion().equals(data.getRegion());
//		
//		if (chrEqual && regEqual)
//			return data.getData();
//		return null;
//	}
	
	public boolean isUptodate() {
		if (dataClass==Void.class) 
			return true;
					
		for (ReferenceSequence reference : current.keySet()) {
				if (!this.data.containsKey(reference)) return false;
				
				ImmutableReferenceGenomicRegion<D> data = this.data.get(reference).toImmutable();
				ImmutableReferenceGenomicRegion<Void> current = this.current.get(reference).toImmutable();
				
				boolean chrEqual = current.getReference().equals(data.getReference());
				boolean regEqual = current.getRegion().equals(data.getRegion());
				
				if (!chrEqual || data.getData()==null || !regEqual) return false;
				
		}
		return true;
	}
	
	public Dimension getPreferredLegendBounds() {
		int insets = 5;
		int legendSize = 12;
		
		int l = getStyles().asArray().length;
		double maxW = legendSize;
		FontMetrics fm = ((SwingGenoVisViewer)viewer).getFontMetrics(((SwingGenoVisViewer)viewer).getFont());
		for (int i=0; i<l; i++) {
			String name = getStyles().get("["+i+"].name").asString("<unknown>");
			maxW = Math.max(maxW, fm.stringWidth(name));
		}
		 
		return new Dimension((int) (legendSize*3+maxW+2*insets), (int) (15*l*1.5+legendSize+2*insets));
	}
	
	@Override
	public void paintLegend(Graphics2D g2, Rectangle2D dest, int columns, int rows) {
		
		
		g2.setStroke(new BasicStroke(1));
		
		int styles = getStyles().asArray().length;
		
		if (styles==0) return;
		
		double insets = Math.min(5,dest.getHeight()/2/(rows-1));
		
		if (rows>0) {
			rows = Math.min(rows, styles);
			columns = (styles+rows-1)/rows;
		}
		columns = Math.min(columns,styles);
		
		double size = (dest.getHeight()-insets*(rows-1))/rows;
		
		double[] columnWidth = new double[columns];
		for (int c=0; c<columns; c++) {
			double maxw = 0;
			for (int i=c; i<styles; i+=columns) {
				String name = getStyles().get("["+i+"].name").asString("<unknown>");
				double w = PaintUtils.getFitStringWidth(name, g2, size);
				w+=size*0.75; // box+space
				maxw = Math.max(maxw,w);
			}
			columnWidth[c]=maxw; // space
		}
		
		double f = dest.getWidth()/ArrayUtils.sum(columnWidth);
		ArrayUtils.mult(columnWidth, f);
		
		double x = dest.getX();
		for (int c=0; c<columns; c++) {
			
			for (int i=c; i<styles; i+=columns) {
				double y = dest.getY()+(i/columns)*size+(i/columns)*insets;
				g2.setPaint(PaintUtils.parseColor(getStyles().get("["+i+"].color").asString("#000000")));
				String name = getStyles().get("["+i+"].name").asString("<unknown>");
				Rectangle2D col = new Rectangle2D.Double(x+size/4,y+size/4,size/2,size/2);
				g2.fill(col);
				g2.setColor(Color.black);
				g2.draw(col);
				col.setRect(x+size, y, columnWidth[c]-size*0.75, size);
				PaintUtils.paintString(name, g2, col, -1, 0);
			}
			
			x+=columnWidth[c];
		}
		
	}

	@Override
	public Dimension2D measureLegend(Graphics2D g2, int columns, int rows) {
		int styles = getStyles().asArray().length;
		if (styles==0) return new Dimension2DDouble();
		double size = 20;
		
		int insets = 5;
		
		if (rows>0) {
			rows = Math.min(rows, styles);
			columns = (styles+rows-1)/rows;
		}
		columns = Math.min(columns,styles);
		
		double height = size*rows+insets*(rows-1);
		double width = 0;
		for (int c=0; c<columns; c++) {
			double maxw = 0;
			for (int i=c; i<styles; i+=columns) {
				String name = getStyles().get("["+i+"].name").asString("<unknown>");
				double w = PaintUtils.getFitStringWidth(name, g2, size);
				w+=size*0.75; // box+space
				maxw = Math.max(maxw,w);
			}
			width+=maxw+size; // space
		}
		
		return new Dimension2DDouble(width,height);
	}
	
	@Override
	public void prepaint(Graphics2D g2) {
		renderBegin(current,data,context.set(g2, null, null, null, null, null));
	}

	@Override
	public void paint(Graphics2D g2) {
		if (!isLocationSet())
			return;
		
		
		renderBackground(context);
		for (ReferenceSequence reference : current.keySet()) {
			
			renderReference(reference);
		}
		renderEnd(context.set(g2, null, null, null, null, null));
		
		renderMargin(context);
		
		if (renderLabels)
			renderLabel(context);
		
		if (renderLegend) {
			
			Dimension2D dim = measureLegend(context.g2, -1, 1);
			double x = getBounds().getMaxX()-dim.getWidth()-2;
			double y = getBounds().getY()+2;
			paintLegend(context.g2, new Rectangle2D.Double(x, y, dim.getWidth(), dim.getHeight()), -1, 1);
		}
		
	}

	
	public void renderReference(ReferenceSequence reference) {
		Graphics2D g2 = context.g2;
		GenomicRegion region = current.get(reference).getRegion();
		
		if (dataClass==Void.class) {
			renderTrack(context.set(g2, reference, fixedStrand,region,region, null));
			return;
		}
		if (!this.data.containsKey(reference)) return;
		
		ImmutableReferenceGenomicRegion<D> data = this.data.get(reference).toImmutable();
		ImmutableReferenceGenomicRegion<Void> current = this.current.get(reference).toImmutable();
		
		boolean chrEqual = current.getReference().equals(data.getReference());
		boolean regEqual = current.getRegion().equals(data.getRegion());
		
		if (!chrEqual || data.getData()==null) return;
		
		if (regEqual) {
			renderTrack(context.set(g2, data.getReference(), fixedStrand,data.getRegion(),data.getRegion(), data.getData()));
		}
		else {
			renderTrack(context.set(g2, current.getReference(), fixedStrand, current.getRegion(), data.getRegion(),data.getData()));
		}
		
	}

	protected TrackRenderContext<D> renderLabel(TrackRenderContext<D> context) {
		String name = this.getId();
		Rectangle2D rect = getLabelRect();
		double inmar = 2;
		
		context.g2.setPaint(Color.black);
		context.g2.fill(rect);
		
		context.g2.setPaint(Color.white);
		
		extend(rect,-inmar,-inmar,-inmar,-inmar);
		PaintUtils.paintString(name, context.g2, rect, 0, 0);
		return context;
	}
	
	private Rectangle2D getLabelRect() {
		Rectangle2D rect = context.g2.getFontMetrics().getStringBounds(getId(), context.g2);
		double mar = 4;
		double inmar = 2;
		rect.setRect(mar+getBounds().getX(),mar+getBounds().getY(), rect.getWidth(), rect.getHeight());
		
		extend(rect,inmar,inmar,inmar,inmar);
		return rect;
	}

	private void extend(Rectangle2D rect, double b, double l, double t, double r) {
		rect.setRect(rect.getX()-l, rect.getY()-t, rect.getWidth()+l+r, rect.getHeight()+t+b);		
	}

	protected TrackRenderContext<D> renderBegin(HashMap<ReferenceSequence, MutableReferenceGenomicRegion<Void>> current, HashMap<ReferenceSequence, MutableReferenceGenomicRegion<D>> data, TrackRenderContext<D> context) {
		return context;
	}
	public TrackRenderContext<D> renderEnd(TrackRenderContext<D> context) {
		return context;
	}
	protected TrackRenderContext<D> renderBackground(TrackRenderContext<D> context){
		return context;
	}
	protected TrackRenderContext<D> renderMargin(TrackRenderContext<D> context){
		return context;
	}
	protected abstract TrackRenderContext<D> renderTrack(TrackRenderContext<D> context);

	
	protected static class TrackRenderContext<D> {
		public Graphics2D g2;
		public ReferenceSequence reference;
		public Strand fixedStrand;
		public GenomicRegion regionToRender;
		public GenomicRegion regionOfData;
		public D data;
		
		private ArrayList values;
		
		public TrackRenderContext<D> set(Graphics2D g2, ReferenceSequence reference, Strand fixedStrand, GenomicRegion regionToRender, GenomicRegion regionOfData, D data) {
			this.g2 = g2;
			this.reference = reference;
			this.fixedStrand = fixedStrand;
			this.regionToRender = regionToRender;
			this.regionOfData = regionOfData;
			this.data = data;
			return this;
		}
		
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public void putValue(int id, Object value) {
			if (values==null) values = new ArrayList();
			while (values.size()<id+1) values.add(null);
			values.set(id, value);
		}
		
		public <T> T get(int id) {
			return (T) values.get(id);
		}

		public boolean contains(int id) {
			return values.size()>=id && values.get(id)!=null;
		}

		public boolean isReady() {
			return regionOfData==regionToRender;
		}
		
	}
	
	
}
