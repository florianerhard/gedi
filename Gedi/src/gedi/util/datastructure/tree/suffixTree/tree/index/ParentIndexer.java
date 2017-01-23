package gedi.util.datastructure.tree.suffixTree.tree.index;

import gedi.util.datastructure.tree.suffixTree.tree.SuffixTree;
import gedi.util.datastructure.tree.suffixTree.tree.traversal.DfsDownAndUpTraverser;
import gedi.util.datastructure.tree.suffixTree.tree.traversal.Traverser;

public class ParentIndexer extends AbstractIntIndexer {

	@Override
	protected void createNew_internal(SuffixTree tree, int[] index) {
		index[0] = -1;
		DfsDownAndUpTraverser t = new DfsDownAndUpTraverser(tree,tree.getRoot().getNode());
		while (t.hasNext()) {
			int node = t.nextInt();
			if (t.getDirection()==Traverser.UP)
				index[t.getPrevious()]=node;
		}
	}

	@Override
	public String name() {
		return "IndexParent";
	}

}
