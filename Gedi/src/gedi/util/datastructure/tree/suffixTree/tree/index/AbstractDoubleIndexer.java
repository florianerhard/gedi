package gedi.util.datastructure.tree.suffixTree.tree.index;

public abstract class AbstractDoubleIndexer extends AbstractIndexer<double[]> {

	@Override
	protected double[] createEmpty(int nodes) {
		return new double[nodes];
	}

}
