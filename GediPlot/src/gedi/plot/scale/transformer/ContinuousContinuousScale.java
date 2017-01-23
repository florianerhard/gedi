package gedi.plot.scale.transformer;

import java.util.function.Function;
import java.util.function.Supplier;

import gedi.plot.GPlotContext;
import gedi.plot.scale.DoubleScalingPreprocessed;
import gedi.plot.scale.GPlotTicks;
import gedi.util.PaintUtils;
import gedi.util.datastructure.dataframe.DataColumn;
import gedi.util.functions.EI;
import gedi.util.mutable.MutableDouble;
import gedi.util.mutable.MutableInteger;

/**
 * Reads the input column as doubles, performs the following steps:
 * 
 * 1. Initial transformation (pre) of all values (transformed values are not stored) Typical are identity or log transformation
 * 2. Affine scaling of all initial values between 0 and 1 (by using the pre transformed min and max of the column)
 * 3. Another transformation (post) to bring the unit value to the desired range and scale (e.g. to pixel space for x and y, to size, or identity for alpha)
 * 4. Add a margin in post space (bottom and top addition of 5% of the total result space (from min to max of column)
 * 
 * pre and post transformations are expected to be strictly monotonous and invertible functions.
 * 
 * Thus, there are 5 spaces: The data space (before pre), the pre space, the unit space, the post space and the result space.
 * 
 * @author flo
 *
 */
public class ContinuousContinuousScale implements ToContinuousScale {
	
	private Function<GPlotContext,DoubleInvertibleTransformer> pre;
	private double min; 
	private double max;
	private double margin;
	private Function<GPlotContext,DoubleInvertibleTransformer> post;
	
	private Function<GPlotContext,DoubleInvertibleTransformer>[] steps;
	
	
	public ContinuousContinuousScale(Function<GPlotContext,DoubleInvertibleTransformer> pre, double min, double max, Function<GPlotContext,DoubleInvertibleTransformer> post) {
		this(pre,min,max,post,0.05);
	}
	public ContinuousContinuousScale(Function<GPlotContext,DoubleInvertibleTransformer> pre, double min, double max, Function<GPlotContext,DoubleInvertibleTransformer> post, double margin) {
		this.pre = pre;
		this.min = min;
		this.max = max;
		this.post = post;
		this.margin = margin;
		
		steps = new Function[4];
		steps[0] = pre;
		steps[1] = (ctx)->DoubleInvertibleTransformer.scale(pre.apply(ctx).transform(ContinuousContinuousScale.this.min), pre.apply(ctx).transform(ContinuousContinuousScale.this.max));
		steps[2] = post;
		steps[3] = (ctx)->DoubleInvertibleTransformer.margin(post.apply(ctx).transform(0), post.apply(ctx).transform(1),margin);
	}
	
	public ContinuousContinuousScale post(Function<GPlotContext,DoubleInvertibleTransformer> post) {
		return new ContinuousContinuousScale(pre, min, max, post);
	}

	
	public ContinuousContinuousScale margin(double mar) {
		return new ContinuousContinuousScale(pre, min, max, post);
	}
	public ContinuousContinuousScale nomargin() {
		return margin(0);
	}



	public enum ValueSpace {
		Data, Pre, Unit, Post, Result
	}
	
	@Override
	public GPlotTicks getTicks(GPlotContext ctx) {
		DoubleInvertibleTransformer pre = this.pre.apply(ctx);
		MutableInteger digits = new MutableInteger(); 
		double[] prespace = PaintUtils.findNiceTicks(pre.transform(min), pre.transform(max), ctx.gplot.getThemeInt("plot.ticks"),digits);
		String[] names = EI.wrap(prespace).map(d->pre.formatTransformed(d, digits.N)).toArray(String.class);
		return new GPlotTicks(names, prespace, d->transform(ctx,d,ValueSpace.Pre,ValueSpace.Result));
	}
	
	@Override
	public double transform(GPlotContext ctx, DataColumn<?> col, int row) {
		return transform(ctx, col.getDoubleValue(row));
	}
	
	@Override
	public double transformToUnit(GPlotContext ctx, DataColumn<?> col, int row) {
		return transformToUnit(ctx, col.getDoubleValue(row));
	}

	public double transform(GPlotContext ctx, double value, ValueSpace in, ValueSpace out) {
		if (in==out) return value;
		
		if (out.ordinal()<in.ordinal()) 
			for (int i=in.ordinal()-1; i!=out.ordinal()-1; i--) 
				value = steps[i].apply(ctx).inverse(value);
		else
			for (int i=in.ordinal(); i!=out.ordinal(); i++) 
				value = steps[i].apply(ctx).transform(value);
		
		return value;
	}
	
	public double transform(GPlotContext ctx, double value) {
		return transform(ctx,value,ValueSpace.Data, ValueSpace.Result);
	}
	
	public double transformToUnit(GPlotContext ctx, double value) {
		return transform(ctx,value,ValueSpace.Data, ValueSpace.Unit);
	}
	
	public double inverse(GPlotContext ctx, double value) {
		return transform(ctx,value,ValueSpace.Result, ValueSpace.Data);
	}
	
	
	public static ContinuousContinuousScale compute(Function<GPlotContext,DoubleInvertibleTransformer> pre, DataColumn<?> col, Function<GPlotContext,DoubleInvertibleTransformer> post) {
		double min = Double.NaN;
		double max = Double.NaN;
		for (int r=0; r<col.size(); r++) {
			double v = col.getDoubleValue(r);
			if (!Double.isNaN(v) && !Double.isInfinite(v)) {
				if(Double.isNaN(min)) {
					min = max = v;
				} else {
					min = Math.min(min, v);
					max = Math.max(max, v);
				}
			}
		}
		if (Double.isNaN(min))
			return new ContinuousContinuousScale(pre,0, 1,post);
		return new ContinuousContinuousScale(pre,min, max,post);
	}

	public Function<GPlotContext, DoubleInvertibleTransformer> getPre() {
		return pre;
	}


	
}
