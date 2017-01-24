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

package gedi.plot.layout;

import gedi.plot.GPlotSubPlot;
import gedi.plot.GPlotRenderable;

public class GPlotLayoutHBox extends GPlotLayoutTable {

	private GPlotLayoutTableRow row;

	public GPlotLayoutHBox() {
		row = row();
	}
	
	
	public GPlotLayoutHBox add(GPlotRenderable ren) {
		return add(ren,0,new double[] {0,0,0,0});
	}
	public GPlotLayoutHBox add(GPlotRenderable ren, double weight) {
		return add(ren,weight,new double[] {0,0,0,0});
	}
	public GPlotLayoutHBox add(GPlotRenderable ren, double[] margin) {
		return add(ren,0,margin);
	}
	public GPlotLayoutHBox add(GPlotRenderable ren, double weight, double[] margin) {
		row.col(ren).weight(weight).margin(margin);
		return this;
	}
	public GPlotLayoutHBox add(GPlotRenderable ren, double weight, double[] margin, GPlotSubPlot subplot, boolean determinesSubplot) {
		GPlotLayoutTableRow r = row.col(ren).weight(weight).margin(margin);
		if (subplot!=null) {
			if (determinesSubplot)
				r.spm(subplot);
			else
				r.sp(subplot);
		}
		return this;
	}
	
	
}
