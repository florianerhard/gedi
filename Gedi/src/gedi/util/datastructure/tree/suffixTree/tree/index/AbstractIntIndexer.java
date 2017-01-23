package gedi.util.datastructure.tree.suffixTree.tree.index;

public abstract class AbstractIntIndexer extends AbstractIndexer<int[]> implements IntIndexer {

	@Override
	protected int[] createEmpty(int nodes) {
		return new int[nodes];
	}

//	protected abstract void createNew_internal(SuffixTree tree, int[] index);
//
//	@Override
//	public int[] createNew(SuffixTree tree) {
//		int[] re = new int[tree.getStorage().getMaxNode()+1];
//		createNew_internal(tree, re);
//		tree.setNodeAttribute(name(), re);
//		return re;
//	}
//	
//	@Override
//	public int[] get(SuffixTree tree) {
//		int[] re = tree.getIntAttributes(name());
//		if (re==null)
//			return createNew(tree);
//		else
//			return re;
//	}
//
//	@Override
//	public boolean has(SuffixTree tree) {
//		return tree.getIntAttributes(name())!=null;
//	}
//	
//	@Override
//	public int hashCode() {
//		return name().hashCode();
//	}
//	
//	@Override
//	public boolean equals(Object obj) {
//		return obj instanceof IntIndexer && ((IntIndexer)obj).name().equals(name());
//	}


}
