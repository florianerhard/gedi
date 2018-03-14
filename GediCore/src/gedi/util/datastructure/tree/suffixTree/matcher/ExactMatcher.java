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
