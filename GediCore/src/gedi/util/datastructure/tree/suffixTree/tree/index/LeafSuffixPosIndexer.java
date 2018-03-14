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
package gedi.util.datastructure.tree.suffixTree.tree.index;

import gedi.util.datastructure.tree.suffixTree.tree.SuffixTree;

public class LeafSuffixPosIndexer extends TextDepthIndexer {

	@Override
	protected void createNew_internal(SuffixTree tree, int[] index) {
		super.createNew_internal(tree, index);
		for (int i=0; i<index.length; i++)
			index[i] = tree.getText().length()-index[i];
	}
	
	
	@Override
	public String name() {
		return "IndexLeafSuffixPos";
	}
}
