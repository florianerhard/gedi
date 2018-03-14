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

import gedi.util.datastructure.tree.suffixTree.tree.SuffixTree;

public abstract class AbstractTraverser implements Traverser {
	
	protected SuffixTree tree;
	
	private int[] cache = new int[] {-1,-1,-1};
	private int current;
	private boolean nextChecked;
	private int direction;
	private int nextDirection;
	
	
	public AbstractTraverser(SuffixTree tree, int start) {
		this.tree = tree;
		cache[0] = start;
		nextDirection = DOWN;
		current = 2;
	}
	
	/**
	 * Puts the index of the next traversed node into nodes[nextIndex], or -1 if there is none.
	 * Returns true, if the direction of this step was down.
	 * @param nodes
	 * @param prevIndex
	 * @param curIndex
	 * @param nextIndex
	 * @return
	 */
	protected abstract boolean advance(int[] nodes, int prevDirection,int prevIndex, int curIndex, int nextIndex);
	
	@Override
	public SuffixTree getSuffixTree() {
		return tree;
	}
	
	@Override
	public int getDirection() {
		return direction;
	}

	@Override
	public int getPrevious() {
		return cache[(current+2)%3];
	}

	@Override
	public boolean hasNext() {
		if (!nextChecked)
			checkNext();
		return cache[(current+1)%3]>=0;
	}

	@Override
	public int nextInt() {
		if (!nextChecked)
			checkNext();
		direction = nextDirection;
		current = (current+1)%3;
		nextChecked = false;
		return cache[current];
	}

	

	private void checkNext() {
		boolean isDown = advance(cache,direction,(current+2)%3,current,(current+1)%3);
		nextDirection = isDown ? DOWN : UP;
		nextChecked = true;
	}

	

	@Override
	public Integer next() {
		return nextInt();
	}

	@Override
	public void remove() {}

}
