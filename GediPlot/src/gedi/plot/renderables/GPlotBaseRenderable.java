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

import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;

import gedi.plot.GPlotContext;
import gedi.plot.GPlotRenderable;
import gedi.util.gui.Dimension2DDouble;

public class GPlotBaseRenderable implements GPlotRenderable {

	
	private double w,h;
	private boolean keepAspect;
	
	public GPlotBaseRenderable() {
	}

	public GPlotBaseRenderable(double w, double h, boolean keepAspect) {
		this.w = w;
		this.h = h;
		this.keepAspect = keepAspect;
	}
	
	@Override
	public boolean keepAspectRatio() {
		return keepAspect;
	}

	@Override
	public Dimension2D measureMinSize(GPlotContext context) {
		return new Dimension2DDouble(sizeToPixelHorizontal(w, context),sizeToPixelVertical(h, context));
	}

	@Override
	public void render(GPlotContext context, Rectangle2D area) {
	}
	
}
