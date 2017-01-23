package gedi.plot.aesthetics.x;

import gedi.plot.aesthetics.Aesthetic2;
import gedi.plot.renderables.legend.Legend2;
import gedi.plot.scale.transformer.ContinuousContinuousScale;
import gedi.plot.scale.transformer.DoubleInvertibleTransformer;
import gedi.plot.scale.transformer.ToContinuousScale;
import gedi.util.datastructure.dataframe.DataColumn;

public class XContinuousAesthetic extends XAesthetic {

	public XContinuousAesthetic(DataColumn<?> col) {
		super(col, ContinuousContinuousScale.compute(ctx->ctx.currentFacet.getXScaling(ctx), col, ctx->DoubleInvertibleTransformer.iscale(ctx.currentFacet.getArea().getMinX(), ctx.currentFacet.getArea().getMaxX())),null);
	}

}
