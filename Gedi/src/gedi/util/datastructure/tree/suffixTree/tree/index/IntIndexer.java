package gedi.util.datastructure.tree.suffixTree.tree.index;

import gedi.util.datastructure.tree.suffixTree.tree.SuffixTree;

public interface IntIndexer {
	
	public int[] createNew(SuffixTree tree);
	public int[] get(SuffixTree tree);
	public boolean has(SuffixTree tree);
	public String name();
	
}
