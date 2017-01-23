package gedi.plot.aesthetics.x;

import gedi.plot.GPlotContext;
import gedi.plot.aesthetics.Aesthetic2;
import gedi.plot.renderables.legend.Legend2;
import gedi.plot.scale.transformer.ToContinuousScale;
import gedi.util.datastructure.dataframe.DataColumn;

public class XAesthetic extends Aesthetic2<ToContinuousScale> {

	public XAesthetic(DataColumn<?> col, ToContinuousScale scale, Legend2<XAesthetic> legend) {
		super(col, scale, legend);
	}

	public static XAesthetic infer(DataColumn<?> column) {
		return column.isDouble()?new XContinuousAesthetic(column):new XDiscreteAesthetic(column);
	}

	public double transform(GPlotContext context, int r) {
		return getScale().transform(context, getColumn(), r);
	}

}
