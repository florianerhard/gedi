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
