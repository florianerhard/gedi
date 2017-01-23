package gedi.util.datastructure.tree.suffixTree.tree.index;

import gedi.util.datastructure.tree.suffixTree.tree.SuffixTree;
import gedi.util.datastructure.tree.suffixTree.tree.traversal.DfsDownAndUpTraverser;
import gedi.util.datastructure.tree.suffixTree.tree.traversal.Traverser;

public class LeavesCountIndexer extends AbstractIntIndexer {

	@Override
	protected void createNew_internal(SuffixTree tree, int[] index) {
		DfsDownAndUpTraverser t = new DfsDownAndUpTraverser(tree,tree.getRoot().getNode());
		int prev = -2;
		while (t.hasNext()) {
			int node = t.nextInt();
			int direction = t.getDirection();
			if (node==prev && direction==Traverser.UP)
				index[t.getPrevious()]=1;
			if (node>=0 && direction==Traverser.UP)
				index[node] += index[t.getPrevious()];
			if (direction==Traverser.DOWN)
				prev = t.getPrevious();
		}
	}

	@Override
	public String name() {
		return "IndexLeavesCount";
	}

}
