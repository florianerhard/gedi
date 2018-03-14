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
package gedi.gui.genovis.tracks;

import gedi.gui.genovis.GenoVisViewer;
import gedi.gui.genovis.SwingGenoVisViewer;
import gedi.util.MathUtils;
import gedi.util.mutable.MutableDouble;

public class ScaleLimitLinker  {

	private MutableDouble fixedMin = new MutableDouble(Double.NaN);
	private MutableDouble fixedMax = new MutableDouble(Double.NaN);
	
	
	private MutableDouble min = new MutableDouble(Double.NaN);
	private MutableDouble max = new MutableDouble(Double.NaN);
	
	private boolean hasRegisteredListener = false;
	
	
	public void updateMinMax(NumericTrack<?> t, double min, double max) {
		if (!hasRegisteredListener) {
			t.getViewer().addPrepaintListener(v->{
				this.min.N = Double.NaN;
				this.max.N = Double.NaN;
			});
			hasRegisteredListener = true;
		}
		
		double tmin = MathUtils.saveMin(min,this.min.N);
		double tmax = MathUtils.saveMax(max,this.max.N);
//		boolean refresh = (Double.isNaN(fixedMin) && tmin!=this.min) || (Double.isNaN(fixedMax) && tmax!=this.max);
		this.min.N = tmin;
		this.max.N = tmax;
//		if (refresh)
//			t.getViewer().repaint();
	}

	public MutableDouble computeCurrentMin(Void data) {
		return Double.isNaN(fixedMin.N)?this.min:fixedMin;
	}

	public MutableDouble computeCurrentMax(Void data) {
		return Double.isNaN(fixedMax.N)?this.max:fixedMax;
	}
	
}
