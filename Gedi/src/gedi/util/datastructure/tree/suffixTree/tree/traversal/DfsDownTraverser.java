package gedi.util.datastructure.tree.suffixTree.tree.traversal;

import com.sun.org.apache.xerces.internal.util.IntStack;

import gedi.util.datastructure.tree.suffixTree.tree.SuffixTree;

public class DfsDownTraverser extends AbstractTraverser {

	private IntStack stack = new IntStack();
	private IntStack parent = new IntStack();
	
	public DfsDownTraverser(SuffixTree tree, int start) {
		super(tree,start);
	}

	@Override
	protected boolean advance(int[] nodes, int prevDirection, int prevIndex, int curIndex,
			int nextIndex) {
		int[] children = tree.getChildNodes(nodes[curIndex]);
		for (int c : children) {
			stack.push(c);
			parent.push(nodes[curIndex]);
		}
		
		if (stack.size()==0) {
			nodes[nextIndex]=-1;
			return true;
		}
		
		nodes[nextIndex] = stack.pop();
		nodes[curIndex] = parent.pop();
		return true;
	}

}
