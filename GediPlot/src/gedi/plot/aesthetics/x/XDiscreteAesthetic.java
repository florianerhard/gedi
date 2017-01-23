package gedi.plot.aesthetics.x;

import java.util.function.ToDoubleFunction;

import gedi.plot.aesthetics.Aesthetic2;
import gedi.plot.renderables.legend.Legend2;
import gedi.plot.scale.transformer.ContinuousContinuousScale;
import gedi.plot.scale.transformer.DiscreteContinuousScale;
import gedi.plot.scale.transformer.DoubleInvertibleTransformer;
import gedi.plot.scale.transformer.RangeMap;
import gedi.plot.scale.transformer.ToContinuousScale;
import gedi.util.datastructure.dataframe.DataColumn;
import gedi.util.math.stat.factor.Factor;

public class XDiscreteAesthetic extends XAesthetic {

	public XDiscreteAesthetic(DataColumn<?> col) {
		super(col, new DiscreteContinuousScale(
				ctx->new RangeMap<>(col.getFactorValue(0).getLevels()),
				ctx->DoubleInvertibleTransformer.iscale(ctx.currentFacet.getArea().getMinX(), ctx.currentFacet.getArea().getMaxX())
				),null);
	}
	
	
	public XDiscreteAesthetic(DataColumn<?> col, ToDoubleFunction<Factor> weights) {
		super(col, new DiscreteContinuousScale(
				ctx->new RangeMap<>(col.getFactorValue(0).getLevels(),weights),
				ctx->DoubleInvertibleTransformer.iscale(ctx.currentFacet.getArea().getMinX(), ctx.currentFacet.getArea().getMaxX())
				),null);
	}

}
