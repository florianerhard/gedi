package gedi.util.datastructure.tree.suffixTree.tree.index;

import gedi.util.datastructure.tree.suffixTree.tree.SuffixTree;

public abstract class AbstractIndexer<T> implements Indexer<T> {

	protected abstract void createNew_internal(SuffixTree tree, T index);
	protected abstract T createEmpty(int nodes);
		
	@Override
	public T createNew(SuffixTree tree) {
		T re = createEmpty(tree.getStorage().getMaxNode()+1);
		createNew_internal(tree, re);
		tree.setNodeAttribute(name(), re);
		return re;
	}
	
	@Override
	public T get(SuffixTree tree) {
		T re = (T) tree.getAttributes(name());
		if (re==null)
			return createNew(tree);
		else
			return re;
	}

	@Override
	public boolean has(SuffixTree tree) {
		return tree.getIntAttributes(name())!=null;
	}
	
	@Override
	public int hashCode() {
		return name().hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof IntIndexer && ((IntIndexer)obj).name().equals(name());
	}


}
