package gedi.plot.layout;

import java.awt.Paint;

import gedi.plot.GPlotRenderable;

public class GPlotLayoutVBox extends GPlotLayoutTable {



	
	private GPlotLayoutTableRow last;

	public GPlotLayoutVBox add(GPlotRenderable ren) {
		last = row().col(ren);
		return this;
	}

	public GPlotLayoutVBox margin(double[] margin) {
		last.margin(margin);
		return this;
	}
	
	public GPlotLayoutVBox inner(double[] inner) {
		last.inner(inner);
		return this;
	}
	
	public GPlotLayoutVBox weight(double weight) {
		last.weight(weight);
		return this;
	}
	
	public GPlotLayoutVBox background(Paint background) {
		last.background(background);
		return this;
	}
	
}
