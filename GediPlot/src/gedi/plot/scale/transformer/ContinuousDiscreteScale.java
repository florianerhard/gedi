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

import java.awt.Color;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import gedi.plot.GPlot;
import gedi.plot.GPlotContext;
import gedi.plot.GPlotUtils;
import gedi.plot.scale.DoubleAestheticScale;
import gedi.plot.scale.DoubleScalingPreprocessed;
import gedi.plot.scale.GPlotTicks;
import gedi.plot.scale.PaintScalingPreprocessed;
import gedi.plot.scale.transformer.ContinuousContinuousScale.ValueSpace;
import gedi.util.PaintUtils;
import gedi.util.datastructure.dataframe.DataColumn;
import gedi.util.gui.ColorPalettes;
import gedi.util.math.stat.binning.RealBinning;
import gedi.util.math.stat.factor.Factor;
import gedi.util.mutable.MutableInteger;
import gedi.util.mutable.MutableMonad;

/**
 * Reads the column as doubles, (pre) maps them first to indices (i.e. 0-n) and then (post) to the final values.
 * 
 * For the pre mapping, it does one of two things:
 * 
 * 1. If there are not too much (could be the number of desired ticks...) distinct values, create a Factor from these distinct values directly;
 * 2. Otherwise, use the nice ticks procedure to compute the breaks for bins and create a binning factor
 * 
 * 
 * 
 * @author flo
 *
 */
public class ContinuousDiscreteScale<O> implements ToDiscreteScale<O> {

	private Function<GPlotContext,DoubleFunction<Factor>> pre;
	private Function<GPlotContext,DiscreteInvertibleTransformer<O>> post;

	public ContinuousDiscreteScale(Function<GPlotContext,DoubleFunction<Factor>> pre, Function<GPlotContext,DiscreteInvertibleTransformer<O>> post) {
		this.pre = pre;
		this.post = post;
	}
	
	public O transform(GPlotContext ctx, double value) {
		return post.apply(ctx).transform(pre.apply(ctx).apply(value).getIndex());
	}
	
	@Override
	public O transform(GPlotContext ctx, DataColumn<?> col, int row) {
		return transform(ctx,col.getDoubleValue(row));
	}
	
	public static <O> ContinuousDiscreteScale<O> automatic(DataColumn<?> col, IntFunction<Function<GPlotContext,DiscreteInvertibleTransformer<O>>> post) {
		if  (col.size()==0) return new ContinuousDiscreteScale<>(ctx->(v)->null, post.apply(0));
		if (GPlotUtils.isContinuousColumn(col)) 
			return bin(col, GPlotUtils.inferScaling(col),post);
		return direct(col,post);
	}
	
	
	public static <O> ContinuousDiscreteScale<O> direct(DataColumn<?> col,IntFunction<Function<GPlotContext,DiscreteInvertibleTransformer<O>>> post) {
		Factor levels = col.getFactorValue(0);
		DoubleFunction<Factor> pre = v->levels.get(v+"");
		return new ContinuousDiscreteScale<>(ctx->pre, post.apply(levels.getLevels().length));
	}
	
	public static <O> ContinuousDiscreteScale<O> bin(DataColumn<?> col,DoubleInvertibleTransformer scaling, IntFunction<Function<GPlotContext,DiscreteInvertibleTransformer<O>>> postCreate) {
		ContinuousContinuousScale ccscale = ContinuousContinuousScale.compute((ctx)->scaling, col, (ctx)->DoubleInvertibleTransformer.identity);
		
		MutableInteger nticks = new MutableInteger(-1);
		MutableMonad<DoubleFunction<Factor>> preRe = new MutableMonad<>();
		MutableMonad<DiscreteInvertibleTransformer<O>> postRe = new MutableMonad<>();
		Consumer<GPlotContext> creator = ctx->{
				if (nticks.N==-1) {
					GPlotTicks ticks = ccscale.getTicks(ctx);
					nticks.N = ticks.count();
					
					if (ticks.count()<2) {
						Factor levels = col.getFactorValue(0);
						DoubleFunction<Factor> pre = v->levels.get(v+"");
						preRe.Item = pre;
						postRe.Item = postCreate.apply(levels.getLevels().length).apply(ctx);
					}
					else  {
						double[] off = new double[ticks.count()+1];
						off[0] = ccscale.transform(ctx,ticks.pre(0)-(ticks.pre(1)-ticks.pre(0))/2, ValueSpace.Pre, ValueSpace.Result);
						for (int i=1; i<ticks.count(); i++) 
							off[i] = ccscale.transform(ctx,ticks.pre(i-1)+(ticks.pre(i)-ticks.pre(i-1))/2, ValueSpace.Pre, ValueSpace.Result);
						off[off.length-1] = ccscale.transform(ctx,ticks.pre(ticks.count()-1)+(ticks.pre(ticks.count()-1)-ticks.pre(ticks.count()-2))/2, ValueSpace.Pre, ValueSpace.Result);
						
						preRe.Item = new RealBinning(off);
						postRe.Item = postCreate.apply(ticks.count()).apply(ctx);
					}
				}
		};

		Function<GPlotContext,DoubleFunction<Factor>> pre = ctx->{
			creator.accept(ctx);
			return preRe.Item;
		};

		Function<GPlotContext,DiscreteInvertibleTransformer<O>> post = ctx->{
			creator.accept(ctx);
			return postRe.Item;
		};
		
		return new ContinuousDiscreteScale<>(pre, post);
	}
	
	
}
