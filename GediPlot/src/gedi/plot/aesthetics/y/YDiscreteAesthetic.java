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

package gedi.plot.aesthetics.y;

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

public class YDiscreteAesthetic extends YAesthetic {

	public YDiscreteAesthetic(DataColumn<?> col) {
		super(col, new DiscreteContinuousScale(
				ctx->new RangeMap<>(col.getFactorValue(0).getLevels()),
				ctx->DoubleInvertibleTransformer.iscale(ctx.currentFacet.getArea().getMaxY(), ctx.currentFacet.getArea().getMinY())
				),null);
	}
	
	
	public YDiscreteAesthetic(DataColumn<?> col, ToDoubleFunction<Factor> weights) {
		super(col, new DiscreteContinuousScale(
				ctx->new RangeMap<>(col.getFactorValue(0).getLevels(),weights),
				ctx->DoubleInvertibleTransformer.iscale(ctx.currentFacet.getArea().getMaxY(), ctx.currentFacet.getArea().getMinY())
				),null);
	}

}
