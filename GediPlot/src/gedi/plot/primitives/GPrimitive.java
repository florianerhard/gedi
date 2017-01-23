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
