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

package gedi.plot.scale;

import java.util.function.DoubleUnaryOperator;

import gedi.plot.scale.transformer.DoubleInvertibleTransformer;
import gedi.util.datastructure.dataframe.DataColumn;

public class DoubleAestheticScale implements AestheticScale<Double,DoubleScalingPreprocessed> {

	private DoubleInvertibleTransformer pre;
	private DoubleInvertibleTransformer post;

	public DoubleAestheticScale(DoubleInvertibleTransformer pre, DoubleInvertibleTransformer post) {
		this.pre = pre;
		this.post = post;
	}
	
	public String formatPreTransformed(double val, int digits) {
		return pre.formatTransformed(val, digits);
	}
	
	public String formatPostTransformed(double val, int digits) {
		return post.formatTransformed(val, digits);
	}
	
	public double pretransform(double val) {
		return pre.transform(val);
	}
	public double posttransform(double val) {
		return post.transform(val);
	}
	public double ipretransform(double val) {
		return pre.inverse(val);
	}
	public double iposttransform(double val) {
		return post.inverse(val);
	}

	public double scaleAsDouble(DoubleScalingPreprocessed preprocess, DataColumn<?> col, int row) {
		return posttransform((pretransform(col.getDoubleValue(row))-pretransform(preprocess.getMin()))/(pretransform(preprocess.getMax())-pretransform(preprocess.getMin())));
	}
	
	public double scaleAsDouble(DoubleScalingPreprocessed preprocess, double value) {
		return posttransform((pretransform(value)-pretransform(preprocess.getMin()))/(pretransform(preprocess.getMax())-pretransform(preprocess.getMin())));
	}

	public double iscaleAsDouble(DoubleScalingPreprocessed preprocess, DataColumn<?> col, int row) {
		double val = iposttransform(col.getDoubleValue(row));
		val = val * (pretransform(preprocess.getMax())-pretransform(preprocess.getMin()));
		val = val + pretransform(preprocess.getMin());
		return ipretransform(val);
	}
	
	public double iscaleAsDouble(DoubleScalingPreprocessed preprocess, double value) {
		double val = iposttransform(value);
		val = val * (pretransform(preprocess.getMax())-pretransform(preprocess.getMin()));
		val = val + pretransform(preprocess.getMin());
		return ipretransform(val);
	}

	public Double scale(DoubleScalingPreprocessed preprocess, DataColumn<?> col, int row) {
		return scaleAsDouble(preprocess,col,row);
	}
	
	
	public static DoubleAestheticScale linlin = new DoubleAestheticScale(DoubleInvertibleTransformer.identity, DoubleInvertibleTransformer.identity);
	
	
}
