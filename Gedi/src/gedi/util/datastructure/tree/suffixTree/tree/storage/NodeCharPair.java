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
