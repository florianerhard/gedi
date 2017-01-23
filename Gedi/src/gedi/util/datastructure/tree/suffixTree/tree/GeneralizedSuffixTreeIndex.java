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
