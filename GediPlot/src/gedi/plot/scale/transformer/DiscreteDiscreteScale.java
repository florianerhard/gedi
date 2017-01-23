package gedi.plot.scale.transformer;

import java.util.function.Function;
import java.util.function.ToDoubleFunction;

import gedi.plot.GPlotContext;
import gedi.util.datastructure.dataframe.DataColumn;
import gedi.util.math.stat.factor.Factor;

public class DiscreteDiscreteScale<O> implements ToDiscreteScale<O> {

	private Function<Factor,O> post;

	
	public DiscreteDiscreteScale(Function<Factor,O> post) {
		this.post = post;
	}

	public O transform(Factor f) {
		return post.apply(f);
	}
	
	@Override
	public O transform(GPlotContext ctx, DataColumn<?> col, int row) {
		return transform(col.getFactorValue(row));
	}
}
