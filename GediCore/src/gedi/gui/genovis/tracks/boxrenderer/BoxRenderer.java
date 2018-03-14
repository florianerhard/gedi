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
package gedi.gui.genovis.tracks.boxrenderer;

import gedi.core.data.annotation.ColorProvider;
import gedi.core.data.annotation.NameProvider;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MissingInformationIntronInformation;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.PaintUtils;
import gedi.util.gui.PixelBasepairMapper;
import gedi.util.nashorn.JSFunction;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

import javax.script.ScriptException;

public class BoxRenderer<D> {
	
	public static final Stroke SOLID = new BasicStroke();
	public static final Stroke SELECTION = new BasicStroke(3.0f);
    public static final Stroke  DOTTED = new BasicStroke(1.0f,
            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {1.0f, 3.0f}, 0f);
    public static final Stroke DASHED = new BasicStroke(1.0f,
            BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL, 1.0f, new float[] {5.0f}, 0f);

	
    
	public Function<ReferenceGenomicRegion<D>,String> stringer = (rgr)->{
		if (rgr.getData() instanceof NameProvider) return ((NameProvider)rgr.getData()).getName();
		else return null;
	};
	public Function<ReferenceGenomicRegion<D>,Paint> background = (rgr)->{
		if (rgr.getData() instanceof ColorProvider)
			return ((ColorProvider)rgr.getData()).getColor();
		return Color.lightGray;
	};
	public Function<ReferenceGenomicRegion<D>,Paint> foreground = (rgr)->Color.black;
	public Function<ReferenceGenomicRegion<D>,Paint> border = (rgr)->Color.darkGray;
	public Function<ReferenceGenomicRegion<D>,Font> font = (rgr)->null;
	public Function<ReferenceGenomicRegion<D>,Stroke> borderStroke = (rgr)->SOLID;
	public Function<ReferenceGenomicRegion<D>,Paint> intronPaint = (rgr)->Color.gray;
	public Function<ReferenceGenomicRegion<D>,Stroke> intronStroke = (rgr)->DOTTED;
	public Function<ReferenceGenomicRegion<D>,Stroke> missingInformationStroke = (rgr)->DOTTED;
	public Function<ReferenceGenomicRegion<D>,Double> height = (rgr)->15.0;
	
	public boolean forceLabel = false;
	
	public void setForceLabel(boolean forceLabel) {
		this.forceLabel = forceLabel;
	}
	

	public void setStringer(String js) throws ScriptException {
		this.stringer = new JSFunction<>(true, js);
	}
	public void setForeground(String js) throws ScriptException {
		this.foreground = new JSFunction<>(true, js);
	}
	
	public void setBorder(String js) throws ScriptException {
		this.border = new JSFunction<>(true, js);
	}
	
	public void setFont(String js) throws ScriptException {
		this.font = new JSFunction<>(true, js);
	}
	
	public void setBorderStroke(String js) throws ScriptException {
		this.borderStroke= new JSFunction<>(true, js);
	}
	
	public void setIntronPaint(String js) throws ScriptException {
		this.intronPaint= new JSFunction<>(true, js);
	}
	
	public void setIntronStroke(String js) throws ScriptException {
		this.intronStroke = new JSFunction<>(true, js);
	}
	
	public void setMissingInformationStroke(String js) throws ScriptException {
		this.missingInformationStroke = new JSFunction<>(true, js);
	}
	
	public void setHeight(String js) throws ScriptException {
		this.height= new JSFunction<>(true, js);
	}
	
	public void setBackground(String js) throws ScriptException {
		this.background = new JSFunction<>(true, js);
	}
	
	
	
	public void setBackground(Function<D, Paint> background) {
		this.background = (r)->background.apply(r.getData());
	}
	
	public void setBackground(Color c) {
		this.background = (r)->c;
	}
	
	public void setForeground(Function<D, Paint> foreground) {
		this.foreground = (r)->foreground.apply(r.getData());
	}
	
	public void setBorder(Function<D, Paint> border) {
		this.border = (r)->border.apply(r.getData());
	}
	
	public void setBorder() {
		this.border = null;
	}
	
	public void setBorder(Color col, float size) {
		BasicStroke stroke = new BasicStroke(size);
		
		this.borderStroke = (r)->stroke;
		this.border = (r)->col;
	}

	public void setIntronPaint(Function<D, Paint> intronPaint) {
		this.intronPaint = (r)->intronPaint.apply(r.getData());
	}
	public void setIntronStroke(Function<D, Stroke> intronStroke) {
		this.intronStroke = (r)->intronStroke.apply(r.getData());
	}
	
	public void setIntronColor(Color col) {
		this.intronPaint = (r)->col;
	}
	public void setIntronSize(float h) {
		BasicStroke re = new BasicStroke(h);
		this.intronStroke = (r)->re;
	}
	
	public void setMissingInformationStroke(
			Function<D, Stroke> missingInformationStroke) {
		this.missingInformationStroke = (r)->missingInformationStroke.apply(r.getData());
	}

	public void setHeight(ToDoubleFunction<D> height) {
		this.height = (r)->height.applyAsDouble(r.getData());
	}
	
	public void setHeight(double h) {
		this.height = (r)->h;
	}
	
	public void setFont(Function<D, Font> font) {
		this.font = (r)->font.apply(r.getData());
	}
	
	public void setFont(String name, int size, boolean bold, boolean italic) {
		int style = Font.PLAIN;
		if (bold) style|=Font.BOLD;
		if (italic) style|=Font.ITALIC;
		
		Font f = new Font(name, style, size);
		this.font = (r)->f;
	}
	


	public GenomicRegion renderBox(Graphics2D g2, PixelBasepairMapper locationMapper,ReferenceSequence reference, Strand strand, GenomicRegion region, D d, double xOffset, double y, double h) {
		double y1 = y;
		double y2 = y1+h-1;
		double ym = y1+h/2;
		Stroke os = g2.getStroke();
		
		boolean rtl = locationMapper.is5to3() && strand==Strand.Minus;
		
		ImmutableReferenceGenomicRegion<D> rgr = new ImmutableReferenceGenomicRegion<>(reference.toStrand(strand),region,d);
		
		Paint border = this.border==null?null:this.border.apply(rgr);
		Paint fg = this.foreground==null?null:this.foreground.apply(rgr);
		Paint bg  = this.background==null?null:this.background.apply(rgr);
		String label = this.stringer==null?null:this.stringer.apply(rgr);
		Font font = this.font==null?null:this.font.apply(rgr);
		
		// paint connecting lines
		if (this.intronPaint!=null && this.intronStroke!=null) {
			for (int i=0; i<region.getNumParts()-1; i++) {
				
				double x1 = rtl?locationMapper.bpToPixel(reference,region.getStart(i+1)):locationMapper.bpToPixel(reference,region.getEnd(i));
				double x2 = rtl?locationMapper.bpToPixel(reference,region.getEnd(i)):locationMapper.bpToPixel(reference,region.getStart(i+1));

				if (missingInformationStroke!=null && region instanceof MissingInformationIntronInformation
						&& ((MissingInformationIntronInformation)region).isMissingInformationIntron(i)) 
					g2.setStroke(missingInformationStroke.apply(rgr));
				else
					g2.setStroke(intronStroke.apply(rgr));
				
				g2.setPaint(intronPaint.apply(rgr));

				g2.draw(new Line2D.Double(xOffset+x1, ym, xOffset+x2, ym));
			}
		}
		
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		// paint boxes
		boolean changed = false;
		GenomicRegion reg = new ArrayGenomicRegion();
		
		g2.setStroke(borderStroke.apply(rgr));
		for (int i=0; i<region.getNumParts(); i++) {
			
			
			Rectangle2D tile = getTile(reference, region.getStart(i), region.getEnd(i), locationMapper, xOffset, y1, y2-y1);
			min = Math.min(min,tile.getX()-xOffset);
			max = Math.max(max,tile.getMaxX()-xOffset);
			
			changed |= renderTile(g2,tile, border,bg,fg,font,label,region, i, d);
			
			reg = reg.union(new ArrayGenomicRegion(locationMapper.pixelToBp(tile.getMinX()),locationMapper.pixelToBp(tile.getMaxX())));
			
		}
		g2.setStroke(os);
		
		return changed?reg:region;
	}
	
	protected Rectangle2D getTile(ReferenceSequence reference, int start, int end, PixelBasepairMapper locationMapper, double xOffset, double y, double h) {
		boolean rtl = locationMapper.is5to3() && reference.getStrand()==Strand.Minus;
		double x1 = rtl?locationMapper.bpToPixel(reference,end):locationMapper.bpToPixel(reference,start);
		double x2 = rtl?locationMapper.bpToPixel(reference,start):locationMapper.bpToPixel(reference,end);
		Rectangle2D.Double tile = new Rectangle2D.Double(xOffset+x1,y,x2-x1,h);
		return tile;
	}

	protected boolean renderTile(Graphics2D g2, Rectangle2D tile, Paint border, Paint bg, Paint fg, Font font, String label, GenomicRegion region, int part, D d) {
		
		boolean changed = false;
		
		if (bg!=null) {
			g2.setPaint(bg);
			g2.fill(tile);
		}
		if (border!=null){
			g2.setPaint(border);
		}
		if (border!=null || bg!=null)
			g2.draw(tile);
		
		Font f = g2.getFont();
		if (font!=null)
			g2.setFont(font);
		
		if (fg!=null && label!=null) {
			g2.setPaint(fg);
			double fit = PaintUtils.getFitStringScale(label, g2, tile);
			if (fit>=1 || fit*g2.getFont().getSize()>=10) {
				PaintUtils.paintString(label, g2, tile, 0, 0);
			} else if (forceLabel) {
				g2.setPaint(bg);
				double l = tile.getMinX();
				double w = tile.getWidth();
				tile.setRect(tile.getMaxX()+1, tile.getY(), 5+PaintUtils.getFitStringWidth(label, g2, tile.getHeight()), tile.getHeight());
//				g2.setStroke(new BasicStroke(1));
//				PaintUtils.paintString(label, g2, tile, 0,0,0, 0, 0,Color.black);
				PaintUtils.paintString(label, g2, tile, 0,0);
				tile.setRect(l, tile.getY(), tile.getWidth()+w, tile.getHeight());
				changed = true;
			}
		}
		
		g2.setFont(f);
		return changed;
	}
	
	public double prefHeight(ReferenceSequence ref, GenomicRegion reg, D d) {
		return height.apply(new ImmutableReferenceGenomicRegion<D>(ref, reg, d));
	}

}