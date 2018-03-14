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
package gedi.util.datastructure.tree.suffixTree.tree.storage;

public class NodeCharPair {

	public int Node;
	public char FirstChar;
	
	public NodeCharPair(int node, char firstChar) {
		Node = node;
		FirstChar = firstChar;
	}

	@Override
	public String toString() {
		return "("+Node+","+FirstChar+")";
	}
	
	@Override
	public int hashCode() {
		return (Node<<12)|FirstChar;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof NodeCharPair))
			return false;
		NodeCharPair o = (NodeCharPair) obj;
		return o.Node==Node && o.FirstChar==FirstChar;
	}
}
