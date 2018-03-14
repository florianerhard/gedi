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
package gedi.util.datastructure.tree.redblacktree;

import gedi.util.datastructure.tree.redblacktree.AugmentedTreeMap.Entry;
import gedi.util.mutable.MutableInteger;

import java.util.Collection;
import java.util.Comparator;


/**
 * Rank 1 is the smallest!
 * @author erhard
 *
 * @param <C>
 */
public class OrderStatisticTree<C> extends AugmentedTreeSet<C, MutableInteger>  {

	public OrderStatisticTree() {
		super(new OrderStatisticTreeAugmentation<C>());
	}
	
	
	public OrderStatisticTree(Collection<? extends C> coll) {
		super(new OrderStatisticTreeAugmentation<C>());
		addAll(coll);
	}
	
	public OrderStatisticTree(Comparator<? super C> comparator) {
		super(comparator,new OrderStatisticTreeAugmentation<C>());
	}
	
	
	public OrderStatisticTree(Comparator<? super C> comparator, Collection<? extends C> coll) {
		super(comparator,new OrderStatisticTreeAugmentation<C>());
		addAll(coll);
	}
	
	public OrderStatisticTree(boolean multi) {
		super(multi,new OrderStatisticTreeAugmentation<C>());
	}
	
	
	public OrderStatisticTree(boolean multi,Collection<? extends C> coll) {
		super(multi,new OrderStatisticTreeAugmentation<C>());
		addAll(coll);
	}
	
	public OrderStatisticTree(boolean multi,Comparator<? super C> comparator) {
		super(multi,comparator,new OrderStatisticTreeAugmentation<C>());
	}
	
	
	public OrderStatisticTree(boolean multi,Comparator<? super C> comparator, Collection<? extends C> coll) {
		super(multi,comparator,new OrderStatisticTreeAugmentation<C>());
		addAll(coll);
	}
	
	public void check() {
		AugmentedTreeMap.Entry<C,?> node = ((AugmentedTreeMap<C,?,MutableInteger>)m).root;
		check(node);
	}
	
	private int check(Entry<C, ?> node) {
		int c = 1;
		if (node.left!=null)
			c += check(node.left);
		if (node.right!=null)
			c += check(node.right);
		if (c!=((MutableInteger)node.augmentation).N) {
			System.err.println(toTreeString());
			throw new RuntimeException("Inconsistent at "+node);
		}
		return c;
	}
	
	/**
	 * Between 1 and size() (inclusive)
	 * @param rank
	 * @return
	 */
	public C getRanked(int rank) {
		if (rank<=0 || rank>size()) throw new IllegalArgumentException("Rank must be between 1 and size!");
		
		AugmentedTreeMap.Entry<C,?> node = ((AugmentedTreeMap<C,?,MutableInteger>)m).root;
		
		int r = node.left==null?1:(((MutableInteger)node.left.augmentation).N+1);
		while (r!=rank) {
			if (rank<r) node = node.left;
			else {
				node = node.right; rank-=r;
			}
			r = node.left==null?1:(((MutableInteger)node.left.augmentation).N+1);
		}
		return node.key;
	}
	
	public int getRank(C item) {
		return getRank(((AugmentedTreeMap<C,?,MutableInteger>)m).getEntry(item));
	}
	
	public int getMinRank(C item) {
		return getRank(((AugmentedTreeMap<C,?,MutableInteger>)m).getMinEntry(item));
	}
	
	public int getMaxRank(C item) {
		return getRank(((AugmentedTreeMap<C,?,MutableInteger>)m).getMaxEntry(item));
	}
	
	public int getMeanRank(C item) {
		return (getRank(((AugmentedTreeMap<C,?,MutableInteger>)m).getMinEntry(item))+getRank(((AugmentedTreeMap<C,?,MutableInteger>)m).getMaxEntry(item)))/2;
	}
	
	private int getRank(AugmentedTreeMap.Entry<C,?> node) {
		if (node==null) throw new IllegalArgumentException("Not an element!");
		
		AugmentedTreeMap.Entry<C,?> root = ((AugmentedTreeMap<C,?,MutableInteger>)m).root;
		int r = node.left==null?1:(((MutableInteger)node.left.augmentation).N+1);
		for (AugmentedTreeMap.Entry<C,?> u=node; u!=root; u=u.parent) {
			if (u==u.parent.right)
				r+=u.parent.left==null?1:(((MutableInteger)u.parent.left.augmentation).N+1);
		}
		
		return r;
	}
	
	


	private static class OrderStatisticTreeAugmentation<C> implements MapAugmentation<C,MutableInteger> {

		@Override
		public MutableInteger computeAugmentation(AugmentedTreeMap.Entry<C,?> n, MutableInteger currentAugmentation, MutableInteger leftAugmentation,
				MutableInteger rightAugmentation) {
			currentAugmentation.N = 1;
			if (leftAugmentation!=null)
				currentAugmentation.N += leftAugmentation.N;
			if (rightAugmentation!=null)
				currentAugmentation.N += rightAugmentation.N;
			return currentAugmentation;
		}

		@Override
		public MutableInteger init(C key) {
			return new MutableInteger(1);
		}

	}



}
