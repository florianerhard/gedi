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
