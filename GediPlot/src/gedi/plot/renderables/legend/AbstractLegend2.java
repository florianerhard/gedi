package gedi.plot.renderables.legend;

import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;

import gedi.plot.GPlotContext;
import gedi.plot.GPlotRenderable;
import gedi.plot.aesthetics.Aesthetic;
import gedi.plot.aesthetics.Aesthetic2;
import gedi.plot.layout.GPlotLayoutHBox;
import gedi.plot.layout.GPlotLayoutStacking;
import gedi.plot.layout.GPlotLayoutTable;
import gedi.plot.layout.GPlotLayoutTable.GPlotLayoutTableRow;
import gedi.plot.layout.GPlotLayoutVBox;
import gedi.plot.renderables.GPlotEmptyRenderable;
import gedi.plot.renderables.GPlotRenderableLabel;
import gedi.util.gui.Dimension2DDouble;

public abstract class AbstractLegend2<AES extends Aesthetic2<?>> implements Legend2<AES> {

	protected AES aes;
	
	@Override
	public void setAesthetic(AES aesthetic) {
		this.aes = aesthetic;
	}
	
	@Override
	public Dimension2D measureMinSize(GPlotContext context) {
			
		double[] halfMargin = {context.legends.legendDistance/2,context.legends.legendDistance/2,context.legends.legendDistance/2,context.legends.legendDistance/2};
		
		GPlotRenderableLabel title = context.legends.title;
		title.label(aes.getColumn().name());
		title.align(-1, 0);
		
		GPlotLayoutVBox layout = new GPlotLayoutVBox();
		layout.add(title).margin(halfMargin);
		
		createLayout(context,layout,halfMargin);
		
		Dimension2D re = layout.measureMinSize(context);
		return re;
	}
	
	public void render(GPlotContext context, Rectangle2D area) {
		double[] halfMargin = {context.legends.legendDistance/2,context.legends.legendDistance/2,context.legends.legendDistance/2,context.legends.legendDistance/2};
		
		GPlotRenderableLabel title = context.legends.title;
		title.label(aes.getColumn().name());
		title.align(-1, 0);
		
		GPlotLayoutVBox layout = new GPlotLayoutVBox();
		layout.add(title).margin(halfMargin).background(context.legends.titleBackground);
		
		createLayout(context,layout,halfMargin);
		
		layout.render(context, area);
	}

	protected void createLayout(GPlotContext context, GPlotLayoutVBox layout, double[] margin) {
		if (getDiscreteCount(context)==0) return;
		
		GPlotLayoutTable tab = new GPlotLayoutTable();
		tab.inner(new double[] {-1, Double.NaN });
		layout.add(tab);
		for (int i=0; i<getDiscreteCount(context); i++) {
			GPlotLayoutStacking stack = new GPlotLayoutStacking();
			stack.layer(new GPlotEmptyRenderable(context.legends.fieldSize, context.legends.fieldSize, context.legends.fieldBackground,true));
			stack.layer(getDiscreteRenderable(context,i));
			GPlotLayoutTableRow row = tab.row();
			row.col(stack).margin(margin);
			row.col(context.legends.label.label(getDiscreteLabel(context,i)).clone().align(-1, 0)).margin(margin);
		}
		
	}

	protected String getDiscreteLabel(GPlotContext context, int index) {
		return null;
	}

	/**
	 * Do not reuse the returned object!
	 * @param index
	 * @return
	 */
	protected GPlotRenderable getDiscreteRenderable(GPlotContext context, int index) {
		return null;
	}

	protected int getDiscreteCount(GPlotContext context) {
		return 0;
	}
	
}
