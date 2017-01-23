package gedi.plot.scale.transformer;

import gedi.plot.GPlotContext;
import gedi.util.datastructure.dataframe.DataColumn;

public interface ToDiscreteScale<O> {

	O transform(GPlotContext ctx, DataColumn<?> col, int row);
	
}
