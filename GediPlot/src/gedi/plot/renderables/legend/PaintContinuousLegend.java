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
import java.util.function.DoubleFunction;

import gedi.plot.GPlot;
import gedi.plot.GPlotContext;
import gedi.plot.GPlotSubPlot;
import gedi.plot.GPlotRenderable;
import gedi.plot.GPlotUtils;
import gedi.plot.aesthetics.DoubleAesthetic;
import gedi.plot.aesthetics.paint.PaintContinuousAesthetic;
import gedi.plot.layout.GPlotLayoutHBox;
import gedi.plot.layout.GPlotLayoutVBox;
import gedi.plot.primitives.Glyph;
import gedi.plot.renderables.GPlotBaseRenderable;
import gedi.plot.renderables.GPlotScale;
import gedi.plot.renderables.GlyphRenderable;
import gedi.plot.scale.DoubleAestheticScale;
import gedi.plot.scale.DoubleScalingPreprocessed;
import gedi.plot.scale.GPlotTicks;
import gedi.plot.scale.PaintScalingPreprocessed;
import gedi.plot.scale.transformer.ContinuousContinuousScale;
import gedi.plot.scale.transformer.DoubleInvertibleTransformer;
import gedi.plot.scale.transformer.ContinuousContinuousScale.ValueSpace;
import gedi.util.datastructure.dataframe.DoubleDataColumn;
import gedi.util.datastructure.dataframe.FactorDataColumn;
import gedi.util.datastructure.dataframe.IntegerDataColumn;
import gedi.util.functions.EI;

public class PaintContinuousLegend extends AbstractLegend2<PaintContinuousAesthetic> {

	
	@Override
	protected void createLayout(GPlotContext context, GPlotLayoutVBox layout, double[] margin) {
			ContinuousContinuousScale scaes = aes.getContiScale();
			ContinuousContinuousScale areat = scaes.post(ctx->DoubleInvertibleTransformer.iscale(ctx.currentFacet.getArea().getMaxY(),ctx.currentFacet.getArea().getMinY()));
			
			
			double height = 5*(context.legends.fieldSize+context.legends.legendDistance);
			double width = context.legends.fieldSize;
			
			GPlotLayoutHBox hbox = new GPlotLayoutHBox();
			layout.add(hbox).margin(new double[] {context.legends.legendDistance,context.legends.legendDistance,context.legends.legendDistance,context.legends.legendDistance});
			
			GPlotSubPlot sub = new GPlotSubPlot(null,scaes.getPre());
					
			hbox.add(new GPlotBaseRenderable(width, height, false) {
				@Override
				public void render(GPlotContext context, Rectangle2D area) {
					Rectangle2D slice = new Rectangle2D.Double();
					DoubleFunction<Color> colorizer = aes.getToColor().apply(context);
					for (double y=area.getMinY(); y<area.getMaxY(); y++) {
						double v = areat.transform(context, y, ValueSpace.Result, ValueSpace.Unit);
						v = scaes.transform(context, v, ValueSpace.Unit, ValueSpace.Result);
						context.g2.setPaint(colorizer.apply(v));
						slice.setRect(area.getMinX(), y, area.getWidth(), 1);
						context.g2.fill(slice);
					}
					context.g2.setPaint(context.gplot.getTheme("ticks.color"));
					context.g2.draw(area);
				}
				
			},0,new double[] {0,0,0,0},sub,true);
			
			hbox.add(new GPlotScale(ctx->areat.getTicks(ctx))
					.right()
					.ticks(context.gplot.getTheme("ticks.color"), context.gplot.getThemeDouble("ticks.width"), context.gplot.getThemeDouble("ticks.size"), context.gplot.getTheme("ticks.type"))
					.labels(context.gplot.getTheme("ticklabels.color"), context.gplot.getTheme("ticklabels.font"),context.gplot.getThemeBoolean("ticklabels.bold"), context.gplot.getThemeDouble("ticklabels.size"), context.gplot.getThemeDouble("ticklabels.distance"))
					,0,new double[] {0,0,0,0},sub,false);
	}
	

	
}
