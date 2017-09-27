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
import gedi.core.region.MissingInformationIntronInformation;
import gedi.util.PaintUtils;
import gedi.util.functions.TriFunction;
import gedi.util.gui.PixelBasepairMapper;

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

public class BoxRenderer<D> {
	
	public static final Stroke SOLID = new BasicStroke();
	public static final Stroke SELECTION = new BasicStroke(3.0f);
    public static final Stroke  DOTTED = new BasicStroke(1.0f,
            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {1.0f, 3.0f}, 0f);
    public static final Stroke DASHED = new BasicStroke(1.0f,
            BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL, 1.0f, new float[] {5.0f}, 0f);

	
    
	public TriFunction<ReferenceSequence,GenomicRegion,D,String> stringer = (ref,reg,d)->{
		if (d instanceof NameProvider) return ((NameProvider)d).getName();
		else return null;
	};
	public TriFunction<ReferenceSequence,GenomicRegion,D,Paint> background = (ref,reg,c)->{
		if (c instanceof ColorProvider)
			return ((ColorProvider)c).getColor();
		return Color.lightGray;
	};
	public TriFunction<ReferenceSequence,GenomicRegion,D,Paint> foreground = (ref,reg,c)->Color.black;
	public TriFunction<ReferenceSequence,GenomicRegion,D,Paint> border = (ref,reg,c)->Color.darkGray;
	public TriFunction<ReferenceSequence,GenomicRegion,D,Font> font = (ref,reg,c)->null;
	public TriFunction<ReferenceSequence,GenomicRegion,D,Stroke> borderStroke = (ref,reg,c)->SOLID;
	public TriFunction<ReferenceSequence,GenomicRegion,D,Paint> intronPaint = (ref,reg,c)->Color.gray;
	public TriFunction<ReferenceSequence,GenomicRegion,D,Stroke> intronStroke = (ref,reg,c)->DOTTED;
	public TriFunction<ReferenceSequence,GenomicRegion,D,Stroke> missingInformationStroke = (ref,reg,c)->DOTTED;
	public TriFunction<ReferenceSequence,GenomicRegion,D,Double> height = (ref,reg,c)->15.0;
	
	public boolean forceLabel = false;
	
	public void setForceLabel(boolean forceLabel) {
		this.forceLabel = forceLabel;
	}
	
	public TriFunction<ReferenceSequence, GenomicRegion, D, Paint> getBackground() {
		return background;
	}

	public TriFunction<ReferenceSequence, GenomicRegion, D, Paint> getForeground() {
		return foreground;
	}

	public TriFunction<ReferenceSequence, GenomicRegion, D, Paint> getBorder() {
		return border;
	}

	public TriFunction<ReferenceSequence, GenomicRegion, D, Font> getFont() {
		return font;
	}

	public TriFunction<ReferenceSequence, GenomicRegion, D, Stroke> getBorderStroke() {
		return borderStroke;
	}

	public TriFunction<ReferenceSequence, GenomicRegion, D, Paint> getIntronPaint() {
		return intronPaint;
	}

	public TriFunction<ReferenceSequence, GenomicRegion, D, Stroke> getIntronStroke() {
		return intronStroke;
	}

	public TriFunction<ReferenceSequence, GenomicRegion, D, Stroke> getMissingInformationStroke() {
		return missingInformationStroke;
	}

	public TriFunction<ReferenceSequence, GenomicRegion, D, Double> getHeight() {
		return height;
	}

	public void setStringer(Function<D, String> stringer) {
		this.stringer = (ref,reg,c)->stringer.apply(c);
	}
	
	public void setStringer() {
		this.stringer = null;
	}
	
	public void setBackground(Function<D, Paint> background) {
		this.background = (ref,reg,c)->background.apply(c);
	}
	
	public void setBackground(Color background) {
		this.background = (ref,reg,c)->background;
	}
	
	public void setForeground(Function<D, Paint> foreground) {
		this.foreground = (ref,reg,c)->foreground.apply(c);
	}
	
	public void setBorder(Function<D, Paint> border) {
		this.border = (ref,reg,c)->border.apply(c);
	}
	
	public void setBorder() {
		this.border = null;
	}
	
	public void setBorder(Color col, float size) {
		BasicStroke stroke = new BasicStroke(size);
		
		this.borderStroke = (ref,reg,c)->stroke;
		this.border = (ref,reg,c)->col;
	}

	public void setIntronPaint(Function<D, Paint> intronPaint) {
		this.intronPaint = (ref,reg,c)->intronPaint.apply(c);
	}
	public void setIntronStroke(Function<D, Stroke> intronStroke) {
		this.intronStroke = (ref,reg,c)->intronStroke.apply(c);
	}
	
	public void setIntronColor(Color col) {
		this.intronPaint = (ref,reg,c)->col;
	}
	public void setIntronSize(float h) {
		BasicStroke re = new BasicStroke(h);
		this.intronStroke = (ref,reg,c)->re;
	}
	
	public void setMissingInformationStroke(
			Function<D, Stroke> missingInformationStroke) {
		this.missingInformationStroke = (ref,reg,c)->missingInformationStroke.apply(c);
	}

	public void setHeight(ToDoubleFunction<D> height) {
		this.height = (ref,reg,c)->height.applyAsDouble(c);
	}
	
	public void setHeight(double height) {
		this.height = (ref,reg,c)->height;
	}
	
	public void setFont(Function<D, Font> font) {
		this.font = (ref,reg,c)->font.apply(c);
	}
	
	public void setFont(String name, int size, boolean bold, boolean italic) {
		int style = Font.PLAIN;
		if (bold) style|=Font.BOLD;
		if (italic) style|=Font.ITALIC;
		
		Font f = new Font(name, style, size);
		this.font = (ref,reg,c)->f;
	}
	

	public TriFunction<ReferenceSequence, GenomicRegion, D, String> getStringer() {
		return stringer;
	}
	
	
	public void setStringer(
			TriFunction<ReferenceSequence, GenomicRegion, D, String> stringer) {
		this.stringer = stringer;
	}

	public void setBackground(
			TriFunction<ReferenceSequence, GenomicRegion, D, Paint> background) {
		this.background = background;
	}

	public void setForeground(
			TriFunction<ReferenceSequence, GenomicRegion, D, Paint> foreground) {
		this.foreground = foreground;
	}

	public void setBorder(
			TriFunction<ReferenceSequence, GenomicRegion, D, Paint> border) {
		this.border = border;
	}

	public void setFont(TriFunction<ReferenceSequence, GenomicRegion, D, Font> font) {
		this.font = font;
	}

	public void setBorderStroke(
			TriFunction<ReferenceSequence, GenomicRegion, D, Stroke> borderStroke) {
		this.borderStroke = borderStroke;
	}

	public void setIntronPaint(
			TriFunction<ReferenceSequence, GenomicRegion, D, Paint> intronPaint) {
		this.intronPaint = intronPaint;
	}

	public void setIntronStroke(
			TriFunction<ReferenceSequence, GenomicRegion, D, Stroke> intronStroke) {
		this.intronStroke = intronStroke;
	}

	public void setMissingInformationStroke(
			TriFunction<ReferenceSequence, GenomicRegion, D, Stroke> missingInformationStroke) {
		this.missingInformationStroke = missingInformationStroke;
	}

	public void setHeight(
			TriFunction<ReferenceSequence, GenomicRegion, D, Double> height) {
		this.height = height;
	}

	public GenomicRegion renderBox(Graphics2D g2, PixelBasepairMapper locationMapper,ReferenceSequence reference, Strand strand, GenomicRegion region, D d, double xOffset, double y, double h) {
		double y1 = y;
		double y2 = y1+h-1;
		double ym = y1+h/2;
		Stroke os = g2.getStroke();
		
		boolean rtl = locationMapper.is5to3() && strand==Strand.Minus;
		
		Paint border = this.border==null?null:this.border.apply(reference.toStrand(strand),region,d);
		Paint fg = this.foreground==null?null:this.foreground.apply(reference.toStrand(strand),region,d);
		Paint bg  = this.background==null?null:this.background.apply(reference.toStrand(strand),region,d);
		String label = this.stringer==null?null:this.stringer.apply(reference.toStrand(strand),region,d);
		Font font = this.font==null?null:this.font.apply(reference.toStrand(strand),region,d);
		
		// paint connecting lines
		if (this.intronPaint!=null && this.intronStroke!=null) {
			for (int i=0; i<region.getNumParts()-1; i++) {
				
				double x1 = rtl?locationMapper.bpToPixel(reference,region.getStart(i+1)):locationMapper.bpToPixel(reference,region.getEnd(i));
				double x2 = rtl?locationMapper.bpToPixel(reference,region.getEnd(i)):locationMapper.bpToPixel(reference,region.getStart(i+1));

				if (missingInformationStroke!=null && region instanceof MissingInformationIntronInformation
						&& ((MissingInformationIntronInformation)region).isMissingInformationIntron(i)) 
					g2.setStroke(missingInformationStroke.apply(reference.toStrand(strand),region,d));
				else
					g2.setStroke(intronStroke.apply(reference.toStrand(strand),region,d));
				
				g2.setPaint(intronPaint.apply(reference.toStrand(strand),region,d));

				g2.draw(new Line2D.Double(xOffset+x1, ym, xOffset+x2, ym));
			}
		}
		
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		// paint boxes
		boolean changed = false;
		GenomicRegion reg = new ArrayGenomicRegion();
		
		g2.setStroke(borderStroke.apply(reference.toStrand(strand),region,d));
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
		return height.apply(ref, reg, d);
	}

}