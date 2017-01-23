package gedi.plot.scale.transformer;

import gedi.plot.GPlotContext;
import gedi.plot.scale.GPlotTicks;
import gedi.util.PaintUtils;
import gedi.util.datastructure.dataframe.DataColumn;

public interface ToContinuousScale {

	double transform(GPlotContext ctx, DataColumn<?> col, int row);
	double transformToUnit(GPlotContext ctx, DataColumn<?> col, int row);
	
	/**
	 * In the space that is supposed to be presented to the user (i.e. pre space)
	 * @param ctx
	 * @return
	 */
	GPlotTicks getTicks(GPlotContext ctx);
	
}
