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
