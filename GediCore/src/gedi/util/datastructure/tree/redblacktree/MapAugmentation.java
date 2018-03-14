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
package gedi.util.datastructure.tree.redblacktree;


public interface MapAugmentation<K,A> {

	/**
	 * l and r (i.e. the children) are already augmented!
	 * Is called after rotations in a bottom up manner! 
	 * @param p
	 * @param l
	 * @param r
	 */
	A computeAugmentation(AugmentedTreeMap.Entry<K,?> n, A currentAugmentation, A leftAugmentation, A rightAugmentation);

	A init(K key);
	
}
