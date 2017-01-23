package gedi.util.datastructure.tree.suffixTree.tree.storage;

import gedi.util.datastructure.tree.suffixTree.tree.SuffixTreeStorage;

public abstract class AbstractSuffixTreeStorage implements SuffixTreeStorage {

	protected CharSequence t;
	protected int nextIndex;
	

	@Override
	public void initialize(char[] alphabet, CharSequence s) {
		nextIndex = 0;
		t=s;
	}
	
	@Override
	public int getMaxNode() {
		return nextIndex-1;
	}
	

}
