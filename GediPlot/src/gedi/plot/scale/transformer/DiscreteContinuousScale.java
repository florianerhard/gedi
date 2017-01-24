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

package gedi.plot.scale.transformer;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

import gedi.plot.GPlotContext;
import gedi.plot.scale.GPlotTicks;
import gedi.util.datastructure.dataframe.DataColumn;
import gedi.util.functions.EI;
import gedi.util.math.stat.factor.Factor;

/**
 * 
 * Reads the column as factors and produces double by:
 * 
 * Transform to bring the indices to the desired range and scale (e.g. to pixel space for x and y, to size)
 * 
 * There are three spaces: The discrete input space, the unit space (covered by ranges, one for each input object) and the post space.
 *
 * @author flo
 *
 */
public class DiscreteContinuousScale implements ToContinuousScale {

	private Function<GPlotContext,RangeMap<Factor>> pre;
	private Function<GPlotContext,DoubleInvertibleTransformer> post;

	
	public DiscreteContinuousScale(Function<GPlotContext,RangeMap<Factor>> pre, Function<GPlotContext,DoubleInvertibleTransformer> post) {
		this.pre = pre;
		this.post = post;
	}
	
	public Range inputToUnit(GPlotContext ctx, Factor f) {
		return pre.apply(ctx).getRange(f);
	}
	
	public Factor unitToInput(GPlotContext ctx, double val) {
		return pre.apply(ctx).getObject(val);
	}

	public double unitToPost(GPlotContext ctx, double val) {
		return post.apply(ctx).transform(val);
	}
	
	public double postToUnit(GPlotContext ctx, double val) {
		return post.apply(ctx).inverse(val);
	}

	public double transform(GPlotContext ctx, Factor f) {
		return post.apply(ctx).transform(pre.apply(ctx).getRange(f).getCenter());
	}
	
	@Override
	public GPlotTicks getTicks(GPlotContext ctx) {
		RangeMap<Factor> pre = this.pre.apply(ctx);
		DoubleInvertibleTransformer post = this.post.apply(ctx);
		
		double[] preVal = EI.seq(0, pre.count()).mapToDouble(index->pre.getRange(index).getCenter()).toDoubleArray();
		String[] names = EI.seq(0, pre.count()).map(index->pre.getObject(index).name()).toArray(String.class);
		
		return new GPlotTicks(names, preVal, d->post.transform(d));
	}
	
	@Override
	public double transform(GPlotContext ctx, DataColumn<?> col, int row) {
		return transform(ctx,col.getFactorValue(row));
	}
	
	@Override
	public double transformToUnit(GPlotContext ctx, DataColumn<?> col, int row) {
		return pre.apply(ctx).getRange(col.getFactorValue(row)).getCenter();
	}
	
}
