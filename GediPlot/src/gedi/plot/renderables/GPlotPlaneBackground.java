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
import java.awt.Paint;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;

import gedi.plot.GPlotContext;
import gedi.plot.GPlotRenderable;
import gedi.plot.aesthetics.DoubleAesthetic;
import gedi.plot.primitives.LineType;
import gedi.plot.scale.DoubleScalingPreprocessed;
import gedi.plot.scale.GPlotTicks;
import gedi.util.PaintUtils;
import gedi.util.datastructure.dataframe.DoubleDataColumn;
import gedi.util.gui.Dimension2DDouble;

public class GPlotPlaneBackground implements GPlotRenderable {

	private Paint color = Color.gray;
	private Paint tickColor = Color.white;
	private double tickWidth = 2;
	private LineType tickType = LineType.SOLID;
	private Paint subTickColor = Color.white;
	private double subTickWidth = 1;
	private LineType subTickType = LineType.SOLID;
	
	
	public GPlotPlaneBackground color(Paint color) {
		this.color = color;
		return this;
	}
	
	public GPlotPlaneBackground ticks(Color tickColor, double tickWidth, LineType tickType) {
		this.tickColor = tickColor;
		this.tickWidth = tickWidth;
		this.tickType = tickType;
		return this;
	}
	
	public GPlotPlaneBackground subTicks(Color subTickColor, double subTickWidth, LineType subTickType) {
		this.subTickColor = subTickColor;
		this.subTickWidth = subTickWidth;
		this.subTickType = subTickType;
		return this;
	}
	
	@Override
	public Dimension2D measureMinSize(GPlotContext context) {
		return new Dimension2DDouble();
	}

	@Override
	public void render(GPlotContext context, Rectangle2D area) {
		context.g2.setPaint(color);
		context.g2.fill(area);
		
		GPlotTicks xticks = context.gplot.x().getScale().getTicks(context);
		if (xticks.count()>0) {
			context.g2.setPaint(subTickColor);
			for (int r=0; r<=xticks.count(); r++) {
				double xco = xticks.subtransformed(r);
				if (xco>area.getMinX() && xco<area.getMaxX())
					subTickType.renderSegment(context.g2, xco, area.getMinY(), xco, area.getMaxY(), 0, subTickWidth);
			}
			
			context.g2.setPaint(tickColor);
			for (int r=0; r<xticks.count(); r++) {
				double xco = xticks.transformed(r);
				if (xco>area.getMinX() && xco<area.getMaxX())
					tickType.renderSegment(context.g2, xco, area.getMinY(), xco, area.getMaxY(), 0, tickWidth);
			}

		}
		
		GPlotTicks yticks = context.gplot.y().getScale().getTicks(context);
		if (yticks.count()>0) {
			context.g2.setPaint(subTickColor);
			for (int r=0; r<=yticks.count(); r++) {
				double yco = yticks.subtransformed(r);
				if (yco>area.getMinY() && yco<area.getMaxY())
					subTickType.renderSegment(context.g2, area.getMinX(), yco, area.getMaxX(), yco, 0, subTickWidth);
			}
			
			context.g2.setPaint(tickColor);
			for (int r=0; r<yticks.count(); r++) {
				double yco = yticks.transformed(r);
				if (yco>area.getMinY() && yco<area.getMaxY())
					tickType.renderSegment(context.g2, area.getMinX(), yco, area.getMaxX(), yco, 0, subTickWidth);
			}

		}
//		
//		if (nticks>0) {
//			DoubleAesthetic x = context.gplot.x();
//			DoubleAesthetic y = context.gplot.y();
//			DoubleScalingPreprocessed ppx = x.getPreprocess();
//			DoubleScalingPreprocessed ppy = y.getPreprocess();
//			DoubleDataColumn xCol = new DoubleDataColumn(null, PaintUtils.findNiceTicks(x.getScale().pretransform(ppx.getMin()), x.getScale().pretransform(ppx.getMax()), nticks));
//			DoubleDataColumn yCol = new DoubleDataColumn(null, PaintUtils.findNiceTicks(y.getScale().pretransform(ppy.getMin()), y.getScale().pretransform(ppy.getMax()), nticks));
//			
//			DoubleDataColumn sxCol = tosub(xCol);
//			DoubleDataColumn syCol = tosub(yCol);
//			
//			xCol.transform(v->x.getScale().ipretransform(v));
//			yCol.transform(v->y.getScale().ipretransform(v));
//			sxCol.transform(v->x.getScale().ipretransform(v));
//			syCol.transform(v->y.getScale().ipretransform(v));
//			
//			context.g2.setPaint(subTickColor);
//			for (int r=0; r<sxCol.size(); r++) {
//				double xco = scaleX(x.transform(sxCol,r),area);
//				if (xco>area.getMinX() && xco<area.getMaxX())
//					subTickType.renderSegment(context.g2, xco, area.getMinY(), xco, area.getMaxY(), 0, subTickWidth);
//			}
//			
//			for (int r=0; r<syCol.size(); r++) {
//				double yco = scaleY(y.transform(syCol,r),area);
//				if (yco>area.getMinY() && yco<area.getMaxY())
//					subTickType.renderSegment(context.g2, area.getMinX(), yco, area.getMaxX(), yco, 0, subTickWidth);
//			}
//
//			
//			context.g2.setPaint(tickColor);
//			for (int r=0; r<xCol.size(); r++) {
//				double xco = scaleX(x.transform(xCol,r),area);
//				if (xco>area.getMinX() && xco<area.getMaxX())
//					tickType.renderSegment(context.g2, xco, area.getMinY(), xco, area.getMaxY(), 0, tickWidth);
//			}
//			
//			for (int r=0; r<yCol.size(); r++) {
//				double yco = scaleY(y.transform(yCol,r),area);
//				if (yco>area.getMinY() && yco<area.getMaxY())
//					tickType.renderSegment(context.g2, area.getMinX(), yco, area.getMaxX(), yco, 0, tickWidth);
//			}
//			
//			
//			
//		}
		
	}


	private DoubleDataColumn tosub(DoubleDataColumn c) {
		if (c.size()<=1) return new DoubleDataColumn(null, new double[0]);
		
		double[] re = new double[c.size()+1];
		re[0] = c.getDoubleValue(0)-(c.getDoubleValue(1)-c.getDoubleValue(0))*0.5; 
		for (int i=1; i<re.length-1; i++)
			re[i] = (c.getDoubleValue(i-1)+c.getDoubleValue(i))*0.5;
		
		re[re.length-1] = c.getDoubleValue(re.length-2)+(c.getDoubleValue(re.length-2)-c.getDoubleValue(re.length-3))*0.5; 
		return new DoubleDataColumn(null, re);
	}

	

}
