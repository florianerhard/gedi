package gedi.util.datastructure.tree.suffixTree.matcher;



import gedi.util.datastructure.collections.intcollections.IntIterator;
import gedi.util.datastructure.tree.suffixTree.SuffixTreeUtils;
import gedi.util.datastructure.tree.suffixTree.tree.Localization;
import gedi.util.datastructure.tree.suffixTree.tree.SuffixTree;
import gedi.util.datastructure.tree.suffixTree.tree.traversal.LeavesTraverser;

public class ExactMatcher extends AbstractMatcher {

	private CharSequence pattern;
	
	public ExactMatcher(CharSequence pattern) {
		this.pattern = pattern;
	}

	@Override
	public IntIterator matchIterator(SuffixTree tree) {
		
		Localization l = SuffixTreeUtils.read(tree,pattern);
		if (l==null) return empty();
		return new LeavesTraverser(tree,l);
	}

	public CharSequence getPattern() {
		return pattern;
	}


}
