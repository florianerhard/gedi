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
