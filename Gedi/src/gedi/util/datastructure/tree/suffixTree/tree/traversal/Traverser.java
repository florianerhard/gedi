package gedi.util.datastructure.tree.suffixTree.tree.traversal;

import gedi.util.datastructure.collections.intcollections.IntIterator;
import gedi.util.datastructure.tree.suffixTree.tree.SuffixTree;

public interface Traverser extends IntIterator {

	public static final int UP = 1;
	public static final int DOWN = 0;
	
	/**
	 * Gets the direction ({@link #UP} or {@link #DOWN}) of the recent {@link #next()} or {@link #nextInt()}. 
	 * @return direction
	 */
	public int getDirection();
	/**
	 * Gets the previously returned value (i.e. the node which has been visited before the current node). 
	 * @return the previous node
	 */
	public int getPrevious();
	
	public SuffixTree getSuffixTree();
	
}
