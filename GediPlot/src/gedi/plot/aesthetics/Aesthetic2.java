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

import gedi.plot.renderables.legend.Legend2;
import gedi.util.datastructure.dataframe.DataColumn;

public class Aesthetic2<S> {

	protected DataColumn<?> col;
	protected S scale;
	protected Legend2 legend;
	
	public Aesthetic2(DataColumn<?> col, S scale, Legend2 legend) {
		this.scale = scale;
		this.col = col;
		this.legend = legend;
		if (legend!=null)
			legend.setAesthetic(this);
	}

	public S getScale() {
		return scale;
	}
	
	public DataColumn<?> getColumn() {
		return col;
	}

	public Legend2<?> getLegend() {
		return legend;
	}
}
