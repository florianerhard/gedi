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
package gedi.plot.primitives;

import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import gedi.plot.GPlot;
import gedi.plot.GPlotContext;
import gedi.plot.GPlotRenderable;
import gedi.plot.aesthetics.Aesthetic;
import gedi.plot.aesthetics.Aesthetic2;
import gedi.util.functions.ExtendedIterator;
import gedi.util.gui.Dimension2DDouble;

public interface GPrimitive extends GPlotRenderable {

	
	void init(GPlot gp);
	
	ExtendedIterator<Aesthetic<?, ?>> getAesthetics();
	ExtendedIterator<Aesthetic2<?>> getAesthetics2();
	
	
	default Dimension2D measureMinSize(GPlotContext context) {
		return new Dimension2DDouble();
	}
	
	
	
}
