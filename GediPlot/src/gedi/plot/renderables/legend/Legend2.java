package gedi.plot.renderables.legend;

import gedi.plot.GPlotRenderable;
import gedi.plot.aesthetics.Aesthetic;
import gedi.plot.aesthetics.Aesthetic2;

public interface Legend2<AES extends Aesthetic2> extends GPlotRenderable {

	void setAesthetic(AES aesthetic);

}
