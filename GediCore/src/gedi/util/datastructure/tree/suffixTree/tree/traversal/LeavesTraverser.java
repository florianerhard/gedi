/**
 * 
 *    Copyright 2017 Florian Erhard
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */
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
