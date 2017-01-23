package gedi.plot.aesthetics;

import gedi.plot.renderables.legend.Legend2;
import gedi.util.datastructure.dataframe.DataColumn;

public class Aesthetic2<S> {

	protected DataColumn<?> col;
	protected S scale;
	protected Legend2 legend;
	
	public Aesthetic2(DataColumn<?> col, S scale, Legend2 legend) {
		this.scale = scale;
		this.col = col;
		this.legend = legend;
		if (legend!=null)
			legend.setAesthetic(this);
	}

	public S getScale() {
		return scale;
	}
	
	public DataColumn<?> getColumn() {
		return col;
	}

	public Legend2<?> getLegend() {
		return legend;
	}
}
