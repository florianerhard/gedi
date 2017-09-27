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

package gedi.util.datastructure.tree.suffixTree.tree;


public interface SuffixTreeStorage {

	
	public void initialize(char[] alphabet, CharSequence s);

	public int addLeaf(int parent, int start);

	public int split(Localization l);
	
	public String toString(CharSequence text);

	public void canonize(Localization localization);
	
	public CharSequence follow(Localization l, char c);
	
	public int lookup(Localization l, char c);

	public void createSuperRootAndRoot();

	public int[] getChildNodes(int node);
	
	public int getMaxNode();

	public CharSequence getSubSequence(int parent, int child);

	public int getNumChildren(int node);
}
