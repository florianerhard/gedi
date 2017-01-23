package gedi.plot.aesthetics.y;

import gedi.plot.GPlotContext;
import gedi.plot.aesthetics.Aesthetic2;
import gedi.plot.aesthetics.x.XAesthetic;
import gedi.plot.aesthetics.x.XContinuousAesthetic;
import gedi.plot.aesthetics.x.XDiscreteAesthetic;
import gedi.plot.renderables.legend.Legend2;
import gedi.plot.scale.transformer.ToContinuousScale;
import gedi.util.datastructure.dataframe.DataColumn;

public class YAesthetic extends Aesthetic2<ToContinuousScale> {

	public YAesthetic(DataColumn<?> col, ToContinuousScale scale, Legend2<YAesthetic> legend) {
		super(col, scale, legend);
	}

	
	public static YAesthetic infer(DataColumn<?> column) {
		return column.isDouble()?new YContinuousAesthetic(column):new YDiscreteAesthetic(column);
	}

	public double transform(GPlotContext context, int r) {
		return getScale().transform(context, getColumn(), r);
	}
}
