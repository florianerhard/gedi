package gedi.util.datastructure.tree.suffixTree.matcher;


import gedi.util.datastructure.collections.intcollections.IntIterator;

public abstract class AbstractMatcher implements Matcher {

	
	protected IntIterator empty() {
		return new IntIterator.EmptyIntIterator();
	}
}
