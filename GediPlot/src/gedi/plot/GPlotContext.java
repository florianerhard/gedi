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
package gedi.plot;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import gedi.plot.renderables.legend.GPlotLegends;


public class GPlotContext {

	public Rectangle2D fullArea;
	public Graphics2D g2;
	public GPlot gplot;
	public GPlotLegends legends;
	public GPlotSubPlot currentFacet;

	public GPlotContext(Rectangle2D fullArea, Graphics2D g2) {
		this.fullArea = fullArea;
		this.g2 = g2;
	}

	
}
