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

package gedi.plot.renderables;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;

import org.scilab.forge.jlatexmath.Box;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXFormula.TeXIconBuilder;
import org.scilab.forge.jlatexmath.TeXIcon;

import gedi.plot.GPlotContext;
import gedi.plot.GPlotRenderable;
import gedi.util.PaintUtils;
import gedi.util.gui.Dimension2DDouble;

public class GPlotRenderableLabel implements GPlotRenderable {

	private Paint color = Color.black;
	private String label = "";
	private String font = "Arial";
	private boolean bold = false;
	private double size = 1;

	private double rotation = 0;
	private double xalign;
	private double yalign;
	private TeXFormula tex;


	public GPlotRenderableLabel label(String label, Paint color, String font, boolean bold, double size) {
		label(label);
		this.color = color;
		this.font = font;
		this.bold = bold;
		this.size = size;
		return this;
	}

	public GPlotRenderableLabel label(String label) {
		if (label.startsWith("$") && label.endsWith("$"))
			return tex(label);
		this.label = label;
		return this;
	}

	public GPlotRenderableLabel tex(String label) {
		this.label = null;
		tex = new TeXFormula(label);
		return this;
	}

	public GPlotRenderableLabel align(double xalign, double yalign) {
		this.xalign = xalign;
		this.yalign = yalign;
		return this;
	}

	public GPlotRenderableLabel clone() {
		return new GPlotRenderableLabel().label(label, color, font, bold, size).align(xalign, yalign).rotationRadians(rotation);
	}


	/**
	 * In degrees!
	 * @param rotation
	 */
	public GPlotRenderableLabel rotation(double rotation) {
		this.rotation = rotation*Math.PI/180;
		return this;
	}

	public GPlotRenderableLabel rotationRadians(double rotation) {
		this.rotation = rotation;
		return this;
	}

	public GPlotRenderableLabel left() {
		return rotation(270);
	}

	public GPlotRenderableLabel bottom() {
		return rotation(0);
	}

	public GPlotRenderableLabel right() {
		return rotation(90);
	}

	public GPlotRenderableLabel top() {
		return rotation(180);
	}

	@Override
	public Dimension2D measureMinSize(GPlotContext context) {
		if (tex!=null) {
			TeXIcon icon = buildTex(context);
			Box box = icon.getBox();
			double size = icon.getTrueIconWidth()/box.getWidth();
			if (rotation==0)
				return new Dimension2DDouble(box.getWidth()*size, box.getHeight()*size);

			Dimension2DDouble re = new Dimension2DDouble(Math.abs(box.getWidth()*Math.cos(rotation)+box.getHeight()*Math.sin(rotation))*size, Math.abs(box.getHeight()*Math.cos(rotation)+box.getWidth()*Math.sin(rotation))*size);
			return re;
		}
		if (label==null) return new Dimension2DDouble();
		double height = sizeToPixelVertical(size, context);
		String font = this.font==null?context.g2.getFont().getFontName():this.font;
		if (size==0) height = context.g2.getFont().getSize();
		Font f = new Font(font, bold?Font.BOLD:0, (int) height);
		Rectangle2D bounds = context.g2.getFontMetrics(f).getStringBounds(label, context.g2);
		if (rotation==0)
			return new Dimension2DDouble(bounds.getWidth(), bounds.getHeight());

		Dimension2DDouble re = new Dimension2DDouble(Math.abs(bounds.getWidth()*Math.cos(rotation)+bounds.getHeight()*Math.sin(rotation)), Math.abs(bounds.getHeight()*Math.cos(rotation)+bounds.getWidth()*Math.sin(rotation)));
		return re;
	}

	private TeXIcon buildTex(GPlotContext context) {
		TeXIconBuilder bui = tex.new TeXIconBuilder();
		if (color instanceof Color)
			bui.setFGColor((Color)color);
		if (bold)
			bui.setStyle(TeXFormula.BOLD);
		bui.setSize((float)sizeToPixelVertical(size, context));
		bui.setStyle(TeXConstants.STYLE_DISPLAY);
		return bui.build();		
	}

	private void paintIcon(Graphics2D g2, TeXIcon icon,Rectangle2D area) {
		
		double w = area.getWidth();
		double h = area.getHeight();
		
//		if (rotation!=0) {
//			double w1 = w*Math.cos(rotation)+h*Math.sin(rotation);
//			h = h*Math.cos(rotation)+w*Math.sin(rotation);
//			w = w1;
//		}
		
		Box box = icon.getBox();
		double size = Math.min(h, icon.getTrueIconWidth()/box.getWidth());
		AffineTransform at = g2.getTransform();
		g2.scale(size, size); 
		g2.rotate(rotation, area.getCenterX()/size, area.getCenterY()/size);
		double xleft = area.getX()/size;
		double xright = (area.getX()+w) / size -box.getWidth();
		double xw = xalign*0.5+0.5;
		double ytop = area.getY()/size+box.getHeight();
		double ybottom = (area.getY()+h) / size ;
		double yw = yalign*0.5+0.5;
		box.draw(g2, (float)((1-xw)*xleft + xw*xright), (float)((1-yw)*ytop + yw*ybottom));
		
		g2.setTransform(at);
	}

	
	
	@Override
	public void render(GPlotContext context, Rectangle2D area) {
		Paint paint = this.color==null?Color.black:this.color;
		context.g2.setPaint(paint);

		if (tex!=null) {
			paintIcon(context.g2, buildTex(context), area);
			return;
		}

		if (label==null) return;


		double height = sizeToPixelVertical(size, context);
		String font = this.font==null?context.g2.getFont().getFontName():this.font;
		if (size==0) height = context.g2.getFont().getSize();
		Font f = new Font(font, bold?Font.BOLD:0, (int) height);

		Font o = context.g2.getFont();
		context.g2.setFont(f);
		
		PaintUtils.paintString(label, context.g2, area, rotation, (float)xalign, (float)yalign);
		context.g2.setFont(o);

	}



}
