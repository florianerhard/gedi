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

package gedi.plot;

import java.awt.geom.Rectangle2D;
import java.util.function.Function;

import gedi.plot.scale.transformer.DoubleInvertibleTransformer;

public class GPlotSubPlot {

	private Rectangle2D area;
	
	private Function<GPlotContext,DoubleInvertibleTransformer> xScaling;
	private Function<GPlotContext,DoubleInvertibleTransformer> yScaling;
	
	
	public GPlotSubPlot(Function<GPlotContext,DoubleInvertibleTransformer> xScaling,
			Function<GPlotContext,DoubleInvertibleTransformer> yScaling) {
		this.xScaling = xScaling;
		this.yScaling = yScaling;
	}
	public DoubleInvertibleTransformer getXScaling(GPlotContext ctx) {
		return xScaling.apply(ctx);
	}
	public DoubleInvertibleTransformer getYScaling(GPlotContext ctx) {
		return yScaling.apply(ctx);
	}
	public Rectangle2D getArea() {
		return area;
	}
	public void setArea(Rectangle2D area) {
		this.area = area;
	}
	
	
}
