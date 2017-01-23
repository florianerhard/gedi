package gedi.plot.scale;

import gedi.util.datastructure.dataframe.DataColumn;

@FunctionalInterface
public interface AestheticScale<T,P> {

	T scale(P preprocess, DataColumn<?> col, int row);
}
