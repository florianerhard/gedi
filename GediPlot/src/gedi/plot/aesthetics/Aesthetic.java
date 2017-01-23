package gedi.plot.aesthetics;

import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;

import gedi.plot.GPlotContext;
import gedi.plot.GPlotRenderable;
import gedi.plot.renderables.legend.Legend;
import gedi.plot.scale.AestheticScale;
import gedi.util.datastructure.dataframe.DataColumn;

public class Aesthetic<E,P> {

	private AestheticScale<E,P> scale;
	protected DataColumn<?> col;
	protected P preprocess;
	protected Legend<E,P> legend;
	
	public Aesthetic(DataColumn<?> col, P scalePreprocessed, AestheticScale<E,P> scale, Legend<E,P> legend) {
		this.scale = scale;
		this.preprocess = scalePreprocessed;
		this.col = col;
		this.legend = legend;
		if (legend!=null)
			legend.setAesthetic(this);
	}
	
	public P getPreprocess() {
		return preprocess;
	}
	
	public void setPreprocess(P preprocess) {
		this.preprocess = preprocess;
	}

	public E transform(int row) {
		return scale.scale(preprocess, col, row);
	}
	
	public DataColumn<?> getColumn() {
		return col;
	}

	public Legend<E, P> getLegend() {
		return legend;
	}
}
