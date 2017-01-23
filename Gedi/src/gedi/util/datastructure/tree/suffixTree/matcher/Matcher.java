package gedi.util.datastructure.tree.suffixTree.matcher;


import gedi.util.datastructure.collections.intcollections.IntIterator;
import gedi.util.datastructure.tree.suffixTree.tree.SuffixTree;

public interface Matcher {

	public IntIterator matchIterator(SuffixTree tree);
	
}
