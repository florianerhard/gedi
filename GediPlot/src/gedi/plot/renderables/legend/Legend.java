package gedi.plot.renderables.legend;

import gedi.plot.GPlotRenderable;
import gedi.plot.aesthetics.Aesthetic;

public interface Legend<E,P> extends GPlotRenderable {

	void setAesthetic(Aesthetic<E, P> aesthetic);

}
