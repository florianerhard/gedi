package gedi.util.datastructure.tree.suffixTree.tree.index;

import gedi.util.datastructure.tree.suffixTree.tree.SuffixTree;

public class LeafSuffixPosIndexer extends TextDepthIndexer {

	@Override
	protected void createNew_internal(SuffixTree tree, int[] index) {
		super.createNew_internal(tree, index);
		for (int i=0; i<index.length; i++)
			index[i] = tree.getText().length()-index[i];
	}
	
	
	@Override
	public String name() {
		return "IndexLeafSuffixPos";
	}
}
