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
import gedi.plot.primitives.Glyph;
import gedi.util.gui.Dimension2DDouble;

public class GlyphRenderable implements GPlotRenderable {

	private Glyph glyph;
	private Paint color = Color.black;
	private double factor = 0.5;
	private boolean absolute = false;
	
	public GlyphRenderable(Glyph glyph) {
		this.glyph = glyph;
	}

	public GlyphRenderable color(Paint color) {
		this.color = color;
		return this;
	}
	
	public GlyphRenderable factor(double factor) {
		this.factor = factor;
		return this;
	}
	
	public GlyphRenderable absoluteSize() {
		absolute = true;
		return this;
	}
	
	public GlyphRenderable relativeSize() {
		absolute = false;
		return this;
	}
	
	@Override
	public Dimension2D measureMinSize(GPlotContext context) {
		return new Dimension2DDouble();
	}

	@Override
	public void render(GPlotContext context, Rectangle2D area) {
		context.g2.setPaint(color);
		if (absolute)
			glyph.render(context.g2, area.getCenterX(), area.getCenterY(), sizeToPixel(factor, context));
		else
			glyph.render(context.g2, area.getCenterX(), area.getCenterY(), factor*Math.min(area.getWidth(), area.getHeight()));
	}
	
}