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
package gedi.plot.layout;

import java.awt.Paint;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import gedi.plot.GPlotContext;
import gedi.plot.GPlotSubPlot;
import gedi.plot.GPlotRenderable;
import gedi.plot.GPlotUtils;
import gedi.plot.primitives.GPrimitive;
import gedi.plot.scale.transformer.DoubleInvertibleTransformer;
import gedi.util.gui.Dimension2DDouble;

public class GPlotLayoutStacking implements GPlotLayout {

	private Paint debug;
	
	private ArrayList<GPlotRenderable> elements = new ArrayList<>();
	private ArrayList<double[]> margin = new ArrayList<>();
	
	
	public GPlotLayoutStacking debug(Paint debug) {
		this.debug = debug;
		return this;
	}
	
	public GPlotLayoutStacking layer(GPlotRenderable ren) {
		return layer(ren,new double[] {0,0,0,0});
	}
	
	public GPlotLayoutStacking layer(GPlotRenderable... ren) {
		for (GPlotRenderable r : ren)
			layer(r,new double[] {0,0,0,0});
		return this;
	}
	
	public GPlotLayoutStacking layer(Iterable<? extends GPlotRenderable> ren) {
		for (GPlotRenderable r : ren)
			layer(r,new double[] {0,0,0,0});
		return this;
	}
	
	public GPlotLayoutStacking layer(GPlotRenderable ren, double[] margin) {
		this.elements.add(ren);
		this.margin.add(margin);
		return this;
	}
	
	@Override
	public Dimension2D measureMinSize(GPlotContext context) {
		Dimension2D re = null;
		for (GPlotRenderable r : elements) {
			Dimension2D n = r.measureMinSize(context);
			if (n!=null) {
				if (re==null) re = n;
				else re.setSize(Math.max(re.getWidth(), n.getWidth()), Math.max(re.getHeight(), n.getHeight()));
			}
		}
		return re;
	}

	@Override
	public void render(GPlotContext context, Rectangle2D area) {
		Rectangle2D sub = new Rectangle2D.Double();
		for (int i=0; i<elements.size(); i++) {
			GPlotRenderable ren = elements.get(i);
			double[] mar = margin.get(i);
			
			sub.setRect(area);
			addMargin(sub,mar);
			if (ren.keepAspectRatio())
				correctAspect(sub,ren.measureMinSize(context));
			
			ren.render(context, sub);
			
			if (debug!=null) {
				context.g2.setPaint(debug);
				context.g2.draw(sub);
			}
		}
	}

			
	
}
