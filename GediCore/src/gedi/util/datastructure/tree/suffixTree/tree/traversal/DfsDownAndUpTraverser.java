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

import java.util.Stack;

import com.sun.org.apache.xerces.internal.util.IntStack;

import gedi.util.datastructure.tree.suffixTree.tree.SuffixTree;

public class DfsDownAndUpTraverser extends AbstractTraverser {

	private Stack<ParentToChildren> stack = new Stack<ParentToChildren>();
	private boolean skip = false;
	
	public DfsDownAndUpTraverser(SuffixTree tree, int start) {
		super(tree,start);
	}

	/**
	 * Call after a DOWN step (after UP: no effect); will not traverse the subtree rooted here and will continue with UP 
	 */
	public void skipCurrentSubtree() {
		skip = true;
	}
	
	public double estimateProgress() {
		return estimateProgress(1E-4);
	}
	public double estimateProgress(double acc) {
		double re = 0;
		double incr = 1;
		for (int i=0; incr>acc && i<stack.size(); i++) {
			incr*=1.0/stack.get(i).children.length;
			re+=(stack.get(i).childPointer-1)*incr;
		}
		return re;
	}
	
	@Override
	protected boolean advance(int[] nodes, int prevDirection, int prevIndex, int curIndex,
			int nextIndex) {
		
		if (prevDirection==DOWN) {
			int[] children = tree.getChildNodes(nodes[curIndex]);
			stack.push(new ParentToChildren(nodes[curIndex],children));
		}
		
		
		if (stack.size()==0) {
			nodes[nextIndex]=-1;
			return true;
		}
		
		ParentToChildren top = stack.peek();
		
		if (prevDirection==DOWN && skip)
			top.skip();
		skip = false;
		
		if (top.hasNext()) {
			nodes[nextIndex] = top.next();
			nodes[curIndex] = top.parent;
			return true;
		}
		else {
			stack.pop();
			nodes[nextIndex] = stack.peek().parent;
			nodes[curIndex] = top.parent;
			return false;
		}
		
	}

	private static class ParentToChildren {
		int parent;
		int[] children;
		int childPointer = 0;
		public ParentToChildren(int parent, int[] children) {
			this.parent = parent;
			this.children = children;
		}
		int next() {
			return children[childPointer++];
		}
		int current() {
			return children[childPointer];
		}
		boolean hasNext() {
			return childPointer<children.length;
		}
		void skip() {
			childPointer = children.length;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(parent);
			sb.append("\t[");
			sb.append(childPointer==0?"*"+children[0]+"*":children[0]);
			for (int i=1; i<children.length; i++) {
				sb.append(",");
				sb.append(childPointer==i?"*"+children[i]+"*":children[i]);
			}
			sb.append("]");
			return sb.toString();
		}
	}
	
}
