package gedi.util.datastructure.tree.suffixTree.tree.traversal;

import com.sun.org.apache.xerces.internal.util.IntStack;

import gedi.util.datastructure.tree.suffixTree.tree.Localization;
import gedi.util.datastructure.tree.suffixTree.tree.SuffixTree;

public class LeavesTraverser extends AbstractTraverser {

	
	/**
	 * Invariant: top element is a leaf and all others are not tested!
	 */
	private IntStack stack = new IntStack();
	
	public LeavesTraverser(SuffixTree tree, Localization l) {
		super(tree,tree.getChildNode(l));
		int startNode = tree.getChildNode(l);
		
		int[] children = tree.getChildNodes(startNode);
		stack.push(startNode);
		while (children.length>0) {
			stack.pop();
			for (int c : children)
				stack.push(c);
			children = tree.getChildNodes(stack.peek());
		}
	}
	
	public LeavesTraverser(SuffixTree tree, int startNode) {
		super(tree,startNode);
		
		int[] children = tree.getChildNodes(startNode);
		stack.push(startNode);
		while (children.length>0) {
			stack.pop();
			for (int c : children)
				stack.push(c);
			children = tree.getChildNodes(stack.peek());
		}
	}

	@Override
	protected boolean advance(int[] nodes, int prevDirection, int prevIndex, int curIndex,
			int nextIndex) {
		
		int size = stack.size();
		
		nodes[nextIndex] = size==0 ? -1 : stack.pop();
		
		if (size>1) {
			int[] children = tree.getChildNodes(stack.peek());
			while (children.length>0) {
				stack.pop();
				for (int c : children)
					stack.push(c);
				children = tree.getChildNodes(stack.peek());
			}
		}
		return true;
	}

}
