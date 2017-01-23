package gedi.util.datastructure.tree.suffixTree.tree.index;

import java.util.Arrays;

import gedi.util.datastructure.tree.suffixTree.tree.SuffixTree;
import gedi.util.datastructure.tree.suffixTree.tree.traversal.DfsDownAndUpTraverser;
import gedi.util.datastructure.tree.suffixTree.tree.traversal.Traverser;

public class FirstOccurrenceIndexer extends AbstractIntIndexer {

	private LeafSuffixPosIndexer leaf = new LeafSuffixPosIndexer();
	
	@Override
	protected void createNew_internal(SuffixTree tree, int[] index) {
		int[] leaf = this.leaf.get(tree);
		
		Arrays.fill(index, Integer.MAX_VALUE);
		DfsDownAndUpTraverser t = new DfsDownAndUpTraverser(tree,tree.getRoot().getNode());
		while (t.hasNext()) {
			int node = t.nextInt();
			if (tree.getNumChildren(node)==0)
				index[node] = leaf[node];
			else if (t.getDirection()==Traverser.UP)
				index[node] = Math.min(index[node],index[t.getPrevious()]);
		}
	}

	@Override
	public String name() {
		return "IndexFirstOccurence";
	}

}
