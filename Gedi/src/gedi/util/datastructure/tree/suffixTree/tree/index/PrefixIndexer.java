package gedi.util.datastructure.tree.suffixTree.tree.index;

import gedi.util.StringUtils;
import gedi.util.datastructure.tree.suffixTree.tree.SuffixTree;
import gedi.util.datastructure.tree.suffixTree.tree.traversal.DfsDownAndUpTraverser;
import gedi.util.datastructure.tree.suffixTree.tree.traversal.DfsDownTraverser;
import gedi.util.datastructure.tree.suffixTree.tree.traversal.Traverser;

public class PrefixIndexer extends AbstractIndexer<CharSequence[]> {

	@Override
	protected void createNew_internal(SuffixTree tree, CharSequence[] index) {
		DfsDownTraverser t = new DfsDownTraverser(tree,tree.getRoot().getNode());
		
		while (t.hasNext()) {
			int node = t.nextInt();
			
			if (t.getPrevious()>-1)
				index[node] = StringUtils.concatSequences(index[t.getPrevious()],tree.getSubSequence(t.getPrevious(),node));
			else
				index[node] = tree.getSubSequence(t.getPrevious(),node);
		}
	}

	@Override
	public String name() {
		return "Prefix";
	}

	@Override
	protected CharSequence[] createEmpty(int nodes) {
		return new CharSequence[nodes];
	}

}
