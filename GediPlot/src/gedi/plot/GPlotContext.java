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
