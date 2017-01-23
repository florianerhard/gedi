package gedi.plot.aesthetics.y;

import gedi.plot.aesthetics.Aesthetic2;
import gedi.plot.renderables.legend.Legend2;
import gedi.plot.scale.transformer.ContinuousContinuousScale;
import gedi.plot.scale.transformer.DoubleInvertibleTransformer;
import gedi.plot.scale.transformer.ToContinuousScale;
import gedi.util.datastructure.dataframe.DataColumn;

public class YContinuousAesthetic extends YAesthetic {

	public YContinuousAesthetic(DataColumn<?> col) {
		super(col, ContinuousContinuousScale.compute(
				ctx->ctx.currentFacet.getYScaling(ctx), 
				col, 
				ctx->DoubleInvertibleTransformer.iscale(ctx.currentFacet.getArea().getMaxY(), ctx.currentFacet.getArea().getMinY())),null);
	}

}
