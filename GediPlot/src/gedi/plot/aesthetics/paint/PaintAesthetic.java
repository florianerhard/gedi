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
package gedi.plot.aesthetics.paint;

import java.awt.Paint;

import gedi.plot.GPlotContext;
import gedi.plot.aesthetics.Aesthetic2;
import gedi.plot.aesthetics.x.XAesthetic;
import gedi.plot.aesthetics.x.XContinuousAesthetic;
import gedi.plot.aesthetics.x.XDiscreteAesthetic;
import gedi.plot.renderables.legend.Legend2;
import gedi.plot.scale.transformer.ToContinuousScale;
import gedi.plot.scale.transformer.ToDiscreteScale;
import gedi.util.datastructure.dataframe.DataColumn;

public class PaintAesthetic extends Aesthetic2<ToDiscreteScale<Paint>> {

	public PaintAesthetic(DataColumn<?> col, ToDiscreteScale<Paint> scale, Legend2<? extends PaintAesthetic> legend) {
		super(col, scale, legend);
	}

	public static PaintAesthetic infer(DataColumn<?> column) {
		return column.isDouble()?new PaintContinuousAesthetic(column):null;
	}

	public Paint transform(GPlotContext context, int r) {
		return getScale().transform(context, getColumn(), r);
	}

}
