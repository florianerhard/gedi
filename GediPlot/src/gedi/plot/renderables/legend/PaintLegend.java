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

import gedi.plot.GPlot;
import gedi.plot.GPlotContext;
import gedi.plot.GPlotSubPlot;
import gedi.plot.GPlotRenderable;
import gedi.plot.GPlotUtils;
import gedi.plot.aesthetics.DoubleAesthetic;
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
import gedi.util.FunctorUtils;
import gedi.util.datastructure.dataframe.DoubleDataColumn;
import gedi.util.datastructure.dataframe.FactorDataColumn;
import gedi.util.datastructure.dataframe.IntegerDataColumn;
import gedi.util.functions.EI;

public class PaintLegend extends AbstractLegend<Paint,PaintScalingPreprocessed> {

	
	@Override
	protected void createLayout(GPlotContext context, GPlotLayoutVBox layout, double[] margin) {
		if (aes.getPreprocess().isDiscrete())
			super.createLayout(context, layout, margin);
		else {
			
			DoubleAesthetic scaes = aes.getPreprocess().getContinuousAes();
			double height = 5*(context.legends.fieldSize+context.legends.legendDistance);
			double width = context.legends.fieldSize;
			
			GPlotLayoutHBox hbox = new GPlotLayoutHBox();
			layout.add(hbox).margin(new double[] {context.legends.legendDistance,context.legends.legendDistance,context.legends.legendDistance,context.legends.legendDistance});
			
			GPlotSubPlot sub = new GPlotSubPlot(null,null);
			ContinuousContinuousScale ccscale = new ContinuousContinuousScale(FunctorUtils.constantFunction(GPlotUtils.inferScaling(scaes.getColumn())), scaes.getPreprocess().getMin(), scaes.getPreprocess().getMax(), ctx->DoubleInvertibleTransformer.iscale(ctx.currentFacet.getArea().getMaxY(), ctx.currentFacet.getArea().getMinY()));
					
			hbox.add(new GPlotBaseRenderable(width, height, false) {
				@Override
				public void render(GPlotContext context, Rectangle2D area) {
					DoubleScalingPreprocessed areaPreprocess = new DoubleScalingPreprocessed(area.getMaxY(), area.getMinY());
					Rectangle2D slice = new Rectangle2D.Double();
					for (double y=area.getMinY(); y<area.getMaxY(); y++) {
						double val = scaes.getScale().iscaleAsDouble(scaes.getPreprocess(), 1/(1-2*DoubleAesthetic.margin)*(DoubleAestheticScale.linlin.scaleAsDouble(areaPreprocess, y)-DoubleAesthetic.margin));
						context.g2.setPaint(aes.getPreprocess().getContinuous().apply(scaes.transform(val)));
						slice.setRect(area.getMinX(), y, area.getWidth(), 1);
						context.g2.fill(slice);
					}
					context.g2.setPaint(context.gplot.getTheme("ticks.color"));
					context.g2.draw(area);
				}
				
			},0,new double[] {0,0,0,0},sub,true);
			
			double[] pre = scaes.getTicks();
			String[] labels = EI.wrap(pre).map(d->scaes.getScale().formatPreTransformed(d, scaes.getTickDigits())).toArray(String.class);
			
			hbox.add(new GPlotScale((ctx)->new GPlotTicks(labels, pre, d->ccscale.transform(ctx, d, ValueSpace.Pre, ValueSpace.Result)))
					.right()
					.ticks(context.gplot.getTheme("ticks.color"), context.gplot.getThemeDouble("ticks.width"), context.gplot.getThemeDouble("ticks.size"), context.gplot.getTheme("ticks.type"))
					.labels(context.gplot.getTheme("ticklabels.color"), context.gplot.getTheme("ticklabels.font"),context.gplot.getThemeBoolean("ticklabels.bold"), context.gplot.getThemeDouble("ticklabels.size"), context.gplot.getThemeDouble("ticklabels.distance"))
					
					,0,new double[] {0,0,0,0},sub,false
			);
		}
	}
	
	
	
	@Override
	protected int getDiscreteCount(GPlotContext context) {
		return aes.getPreprocess().getCount();
	}
	

	@Override
	protected GPlotRenderable getDiscreteRenderable(GPlotContext context, int index) {
		Paint p = aes.getPreprocess().getPaint(new FactorDataColumn(null, aes.getColumn().getFactorValue(0).getLevels()), index);
		return new GlyphRenderable(Glyph.DOT).color(p);
	}
	
	@Override
	protected String getDiscreteLabel(GPlotContext context, int index) {
		return aes.getColumn().getFactorValue(0).getNames()[index];
	}

	
}
