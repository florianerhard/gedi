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

import gedi.util.datastructure.collections.intcollections.IntIterator;
import gedi.util.datastructure.tree.suffixTree.tree.SuffixTree;

public interface Traverser extends IntIterator {

	public static final int UP = 1;
	public static final int DOWN = 0;
	
	/**
	 * Gets the direction ({@link #UP} or {@link #DOWN}) of the recent {@link #next()} or {@link #nextInt()}. 
	 * @return direction
	 */
	public int getDirection();
	/**
	 * Gets the previously returned value (i.e. the node which has been visited before the current node). 
	 * @return the previous node
	 */
	public int getPrevious();
	
	public SuffixTree getSuffixTree();
	
}
