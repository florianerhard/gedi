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
