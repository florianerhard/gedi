package gedi.plot.aesthetics.paint;

import java.awt.Paint;

import gedi.plot.GPlotContext;
import gedi.plot.aesthetics.Aesthetic2;
import gedi.plot.aesthetics.x.XAesthetic;
import gedi.plot.aesthetics.x.XContinuousAesthetic;
import gedi.plot.aesthetics.x.XDiscreteAesthetic;
import gedi.plot.renderables.legend.Legend2;
import gedi.plot.scale.transformer.ToContinuousScale;
import gedi.plot.scale.transformer.ToDiscreteScale;
import gedi.util.datastructure.dataframe.DataColumn;

public class PaintAesthetic extends Aesthetic2<ToDiscreteScale<Paint>> {

	public PaintAesthetic(DataColumn<?> col, ToDiscreteScale<Paint> scale, Legend2<? extends PaintAesthetic> legend) {
		super(col, scale, legend);
	}

	public static PaintAesthetic infer(DataColumn<?> column) {
		return column.isDouble()?new PaintContinuousAesthetic(column):null;
	}

	public Paint transform(GPlotContext context, int r) {
		return getScale().transform(context, getColumn(), r);
	}

}
