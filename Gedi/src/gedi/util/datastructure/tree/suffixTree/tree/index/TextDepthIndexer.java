package gedi.util.datastructure.tree.suffixTree.tree.index;

import gedi.util.datastructure.tree.suffixTree.tree.SuffixTree;
import gedi.util.datastructure.tree.suffixTree.tree.traversal.DfsDownTraverser;
import gedi.util.datastructure.tree.suffixTree.tree.traversal.Traverser;

public class TextDepthIndexer extends AbstractIntIndexer {

	@Override
	public String name() {
		return "IndexTextDepth";
	}

	@Override
	protected void createNew_internal(SuffixTree tree, int[] index) {
		DfsDownTraverser t = new DfsDownTraverser(tree,tree.getRoot().getNode());
		while (t.hasNext()) {
			int node = t.nextInt();
			if (node>0 && t.getDirection()==Traverser.DOWN)
				index[node] = index[t.getPrevious()]+tree.getSubSequence(t.getPrevious(), node).length();
		}
	}

}
