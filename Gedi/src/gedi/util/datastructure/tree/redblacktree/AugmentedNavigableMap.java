package gedi.util.datastructure.tree.redblacktree;

import java.util.NavigableMap;

public interface AugmentedNavigableMap<K,V,A> extends NavigableMap<K,V>{

	
	MapAugmentation<K,A> getAugmentation();

	AugmentedNavigableMap<K,V,A> subMap(K fromKey, boolean fromInclusive,
			K toKey,   boolean toInclusive);

	AugmentedNavigableMap<K,V,A> headMap(K toKey, boolean inclusive);

	AugmentedNavigableMap<K,V,A> tailMap(K fromKey, boolean inclusive);

	AugmentedNavigableMap<K,V,A> descendingMap();
}
