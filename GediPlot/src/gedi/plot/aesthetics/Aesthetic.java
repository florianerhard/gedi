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
package gedi.plot.aesthetics;

import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;

import gedi.plot.GPlotContext;
import gedi.plot.GPlotRenderable;
import gedi.plot.renderables.legend.Legend;
import gedi.plot.scale.AestheticScale;
import gedi.util.datastructure.dataframe.DataColumn;

public class Aesthetic<E,P> {

	private AestheticScale<E,P> scale;
	protected DataColumn<?> col;
	protected P preprocess;
	protected Legend<E,P> legend;
	
	public Aesthetic(DataColumn<?> col, P scalePreprocessed, AestheticScale<E,P> scale, Legend<E,P> legend) {
		this.scale = scale;
		this.preprocess = scalePreprocessed;
		this.col = col;
		this.legend = legend;
		if (legend!=null)
			legend.setAesthetic(this);
	}
	
	public P getPreprocess() {
		return preprocess;
	}
	
	public void setPreprocess(P preprocess) {
		this.preprocess = preprocess;
	}

	public E transform(int row) {
		return scale.scale(preprocess, col, row);
	}
	
	public DataColumn<?> getColumn() {
		return col;
	}

	public Legend<E, P> getLegend() {
		return legend;
	}
}
