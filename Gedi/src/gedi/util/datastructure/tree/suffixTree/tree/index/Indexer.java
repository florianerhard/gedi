package gedi.util.datastructure.tree.suffixTree.tree.index;

import gedi.util.datastructure.tree.suffixTree.tree.SuffixTree;

public interface Indexer<T> {
	
	public T createNew(SuffixTree tree);
	public T get(SuffixTree tree);
	public boolean has(SuffixTree tree);
	public String name();
	
}
