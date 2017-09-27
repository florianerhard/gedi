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

import gedi.util.sequence.Alphabet;

public interface GeneralizedSuffixTreeIndex {

	int getSequenceIndexForGeneralizedPosition(int pos);
	
	CharSequence getSequenceForGeneralizedPosition(int pos);
	
	int getSequencePositionForGeneralizedPosition(int pos);

	int getNumSequences();

	/**
	 * Without superroot or separator chars!
	 * @return
	 */
	Alphabet getAlphabet();
	
}
