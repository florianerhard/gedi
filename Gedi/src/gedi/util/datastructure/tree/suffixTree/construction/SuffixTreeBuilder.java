package gedi.util.datastructure.tree.suffixTree.construction;

import gedi.util.datastructure.tree.suffixTree.tree.SuffixTree;

public interface SuffixTreeBuilder {

	public <T extends SuffixTree> T build(T suffixTree, CharSequence...input) ;
	
}
