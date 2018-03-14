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
package gedi.plot.renderables.legend;

import java.awt.Color;
import java.awt.Paint;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import gedi.plot.GPlotContext;
import gedi.plot.GPlotRenderable;
import gedi.plot.primitives.LineType;
import gedi.plot.renderables.GPlotRenderableLabel;
import gedi.util.StringUtils;
import gedi.util.functions.EI;
import gedi.util.gui.Dimension2DDouble;
import gedi.util.mutable.MutableDouble;

public class GPlotLegends implements GPlotRenderable {


	GPlotRenderableLabel title = new GPlotRenderableLabel();
	Paint titleBackground;

	GPlotRenderableLabel label = new GPlotRenderableLabel();
	double legendDistance = 1;

	private int columns = 1;

	Paint fieldBackground = Color.white;
	
	
	Paint background = Color.white;
	LineType border = LineType.SOLID;
	double borderWidth = 0;
	Paint borderColor = Color.BLACK;
	double fieldSize = 1.5;
	

	public GPlotLegends title(Paint titleBackground, Paint titleColor, String titleFont, boolean titleBold, double titleSize) {
		this.title.label(titleFont, titleColor, titleFont, titleBold, titleSize);
		this.titleBackground = titleBackground;
		return this;
	}
	public GPlotLegends labels(Paint labelColor, String labelFont, boolean labelBold, double labelSize) {
		this.label.label(labelFont, labelColor, labelFont, labelBold, labelSize);
		return this;
	}
	
	public GPlotLegends style(Paint background, LineType border, double borderWidth, Paint borderColor) {
		this.background = background;
		this.border = border;
		this.borderWidth = borderWidth;
		this.borderColor = borderColor;
		return this;
	}

	public GPlotLegends field(Paint backgroundColor, double size) {
		this.fieldBackground = backgroundColor;
		this.fieldSize = size;
		return this;
	}

	public GPlotLegends distance(double legendDistance) {
		this.legendDistance = legendDistance;
		return this;
	}


	@Override
	public Dimension2D measureMinSize(GPlotContext context) {
		context.legends = this;
		
		ArrayList<Dimension2D> all = EI.wrap(context.gplot.getLayers())
				.demultiplex(l->l.getAesthetics()).map(a->a.getLegend())
				.removeNulls()
				.unique(false)
				.map(r->r.measureMinSize(context))
				.list();
		EI.wrap(context.gplot.getLayers())
				.demultiplex(l->l.getAesthetics2()).map(a->a.getLegend())
				.removeNulls()
				.unique(false)
				.map(r->r.measureMinSize(context))
				.toCollection(all);

		MutableDouble height = new MutableDouble();
		MutableDouble width = new MutableDouble();
		
		ArrayList<ArrayList<Dimension2D>> lay = getLayout(context, all);
		
		EI.wrap(lay).forEachRemaining(col->{
			double h = EI.wrap(col).mapToDouble(d->d.getHeight()).sum();
			double w = EI.wrap(col).mapToDouble(d->d.getWidth()).max();
			h += (col.size()-1)*sizeToPixelVertical(legendDistance, context);
			height.N = Math.max(height.N, h);
			width.N += w;
		});
		width.N += (lay.size()-1)*sizeToPixelHorizontal(legendDistance, context);
		context.legends = null;
		
		
		return new Dimension2DDouble(width.N,height.N);
	}

	private ArrayList<ArrayList<Dimension2D>> getLayout(GPlotContext context, ArrayList<Dimension2D> all) {
		
		ArrayList<ArrayList<Dimension2D>> re = new ArrayList<>();
		re.add(new ArrayList<Dimension2D>());
		
		double sum = EI.wrap(all).mapToDouble(d->d.getHeight()).sum();
		double part = sum/columns;
		double sofar = 0;

		for (int i=0; i<all.size(); i++) {
			double h = all.get(i).getHeight();
			if (sofar+h>part && re.size()<columns) {
				if ((part-sofar)/h<0.5) {
					re.add(new ArrayList<>());
					re.get(re.size()-1).add(all.get(i));
					sofar=h;
				} else { 
					re.get(re.size()-1).add(all.get(i));
					re.add(new ArrayList<>());
					sofar = 0;
				}
								
			} else {
				re.get(re.size()-1).add(all.get(i));
			}
		}
		
		return re;
	}


	@Override
	public void render(GPlotContext context, Rectangle2D area) {

		context.legends = this;
		
		ArrayList<? extends GPlotRenderable> rens = EI.wrap(context.gplot.getLayers())
				.demultiplex(l->l.getAesthetics())
				.map(a->a.getLegend())
				.removeNulls()
				.unique(false)
				.list();
		EI.wrap(context.gplot.getLayers())
			.demultiplex(l->l.getAesthetics2())
			.map(a->a.getLegend())
			.removeNulls()
			.unique(false)
			.toCollection((ArrayList)rens);
		
		ArrayList<Dimension2D> all = EI.wrap(rens)
				.map(r->r.measureMinSize(context))
				.list();

		ArrayList<ArrayList<Dimension2D>> lay = getLayout(context, all);
		if (lay.size()==0) return;
		
		double[] widths = new double[lay.size()];
		double[][] heights = new double[lay.size()][];
		
		int total = 0;
		for (int c=0; c<lay.size(); c++) {
			ArrayList<Dimension2D> col = lay.get(c);
			heights[c] = EI.wrap(col).mapToDouble(d->d.getHeight()).toDoubleArray();
			widths[c] = EI.wrap(col).mapToDouble(d->d.getWidth()).max();
			total += col.size();
		}
		if (total==0) return;
		
		for (int i=1; i<widths.length; i++)
			widths[i]+=widths[i-1]+sizeToPixelHorizontal(legendDistance, context);

		for (int i=0; i<heights.length; i++)
			for (int j=1; j<heights[i].length; j++)
				heights[i][j]+=heights[i][j-1]+sizeToPixelVertical(legendDistance, context);

		double w = widths[widths.length-1];
		double h = EI.wrap(heights).mapToDouble(a->a[a.length-1]).max();
		
		double fx = Math.min(1, area.getWidth()/w);
		double fy = Math.min(1, area.getHeight()/h);
		
		double sx = sizeToPixelHorizontal(legendDistance, context)*fx;
		double sy = sizeToPixelVertical(legendDistance, context)*fy;
		
		for (int i=0; i<widths.length; i++)
			widths[i]*=fx;

		for (int i=0; i<heights.length; i++)
			for (int j=0; j<heights[i].length; j++)
				heights[i][j]*=fy;
		
		int i=0;
		int j=0;
		Rectangle2D sub = new Rectangle2D.Double();
		for (int e=0; e<all.size(); e++) {
			GPlotRenderable r = rens.get(e);
			Dimension2D pref = all.get(e);
			
			double lw = i==0?0:widths[i-1]+sx;
			double lh = j==0?0:heights[i][j-1]+sy;
			sub.setRect(
					area.getX()+lw,
					area.getY()+lh,
					widths[i]-lw,
					Math.min(pref.getHeight(),heights[i][j]-lh)
					);
			
			context.g2.setPaint(background);
			context.g2.fill(sub);
			
			r.render(context, sub);
			
			context.g2.setPaint(borderColor);
			border.renderShape(context.g2, sub, borderWidth);
			
			if (++i>=heights.length) {
				i=0;
				j++;
			}
		}
		
		context.legends = null;
		
	}


}
