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


import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.util.FunctorUtils;
import gedi.util.MathUtils;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.mutable.MutableInteger;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Spliterator;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * If I implements {@link Comparable}, the sorting must be consistent with {@link IntervalComparator}!
 * @author erhard
 *
 * @param <I>
 * @param <D>
 */
public class IntervalTree<I extends Interval,D> extends AugmentedTreeMap<I, D,MutableInteger> implements Interval {


	private static final long serialVersionUID = 9064248848470532710L;
	
	private ReferenceSequence reference = null;

	public IntervalTree(ReferenceSequence reference) {
		super(new IntervalComparator(),new IntervalTreeAugmentation<I>());
		this.reference = reference;
	}

	public IntervalTree(Map<? extends I,? extends D> map, ReferenceSequence reference) {
		super(new IntervalComparator(),new IntervalTreeAugmentation<I>());
		putAll(map);
		this.reference = reference;
	}
	
	public ReferenceSequence getReference() {
		return reference;
	}


	public ExtendedIterator<ImmutableReferenceGenomicRegion<D>> ei() {
		return EI.wrap(entrySet()).map(e->new ImmutableReferenceGenomicRegion<D>(reference, e.getKey().asRegion(), e.getValue()));
	}
	
	public ExtendedIterator<I> keys(int start, int stop) {
		return EI.wrap(iterateIntervalsIntersecting(start, stop, i->true)).map(e->e.getKey());
	}

	
	public boolean add(I inter) {
		boolean re = containsKey(inter);
		put(inter,null);
		return re;
	}
	
	public boolean addAll(Collection<? extends I> inter) {
		boolean re = false;
		for (I i : inter)
			re|=add(i);
		return re;
	}

	public int countIntervalsSpanning(int p) {
		Stack<AugmentedTreeMap.Entry<I, ?>> stack = new Stack<AugmentedTreeMap.Entry<I,?>>();
		if (root!=null)
			stack.push(root);
		int re = 0;
		while (!stack.isEmpty()) {
			AugmentedTreeMap.Entry<I, ?> n = stack.pop();
			if (p<=getMaxStop(n)) {
				if (n.left!= null)
					stack.push(n.left);
				int a = n.getKey().getStart();
				int b = n.getKey().getStop();
				if (a<=p && p<=b)
					re++;
				if (p>=n.getKey().getStart() && n.right != null)
					stack.push(n.right);
			}
		}
		return re;
	}

	public <C extends Collection<? super I>> C getIntervalsSpanning(int p, C re) {
		Stack<AugmentedTreeMap.Entry<I, ?>> stack = new Stack<AugmentedTreeMap.Entry<I,?>>();
		if (root!=null)
			stack.push(root);

		while (!stack.isEmpty()) {
			AugmentedTreeMap.Entry<I, ?> n = stack.pop();
			if (p<=getMaxStop(n)) {
				if (n.left!= null)
					stack.push(n.left);
				int a = n.getKey().getStart();
				int b = n.getKey().getStop();
				if (a<=p && p<=b)
					re.add(n.getKey());
				if (p>=n.getKey().getStart() && n.right != null)
					stack.push(n.right);
			}
		}
		return re;
	}

	/**
	 * Diffs: minStartDiff=-1 and maxStartDiff=3: only intervals are returned which have a startposition in [start-1:start+3]
	 * They have however at least to intersect!
	 * Integer.MIN_VALUE and Integer.MAX_VALUE are considered as -Inf and Inf
	 * @param <C>
	 * @param start
	 * @param stop
	 * @param re
	 * @param startDiff
	 * @param stopDiff
	 * @return
	 */
	public <C extends Collection<? super I>> C getIntervals(int start, int stop, C re, int minStartDiff, int maxStartDiff, int minStopDiff, int maxStopDiff) {
		Stack<AugmentedTreeMap.Entry<I, ?>> stack = new Stack<AugmentedTreeMap.Entry<I,?>>();
		if (root!=null)
			stack.push(root);

		while (!stack.isEmpty()) {
			AugmentedTreeMap.Entry<I, ?> n = stack.pop();
			if (start<=getMaxStop(n)) {
				if (n.left!= null)
					stack.push(n.left);
				int a = n.getKey().getStart();
				int b = n.getKey().getStop();
				if (a<=stop && b>=start && 
						(minStartDiff==Integer.MIN_VALUE||a+minStartDiff<=start) && 
						(maxStartDiff==Integer.MAX_VALUE||a+maxStartDiff>=start) && 
						(minStopDiff==Integer.MIN_VALUE||b+minStopDiff<=stop) && 
						(maxStopDiff==Integer.MAX_VALUE||b+maxStopDiff>=stop)
						)
					re.add(n.getKey());
				if (stop>=n.getKey().getStart() && n.right != null)
					stack.push(n.right);
			}
		}
		return re;
	}


	public <C extends Collection<? super I>> C getIntervalsSpanning(int start, int stop, C re) {
		Stack<AugmentedTreeMap.Entry<I, ?>> stack = new Stack<AugmentedTreeMap.Entry<I,?>>();
		if (root!=null)
			stack.push(root);

		while (!stack.isEmpty()) {
			AugmentedTreeMap.Entry<I, ?> n = stack.pop();
			if (start<=getMaxStop(n)) {
				if (n.left!= null)
					stack.push(n.left);
				int a = n.getKey().getStart();
				int b = n.getKey().getStop();
				if (a<=start && b>=stop)
					re.add(n.getKey());
				if (stop>=n.getKey().getStart() && n.right != null)
					stack.push(n.right);
			}
		}
		return re;
	}

	public <C extends Collection<? super I>> C getIntervalsEqual(int start, int stop, C re) {
		Stack<AugmentedTreeMap.Entry<I, ?>> stack = new Stack<AugmentedTreeMap.Entry<I,?>>();
		if (root!=null)
			stack.push(root);

		while (!stack.isEmpty()) {
			AugmentedTreeMap.Entry<I, ?> n = stack.pop();
			if (start<=getMaxStop(n)) {
				if (n.left!= null)
					stack.push(n.left);
				int a = n.getKey().getStart();
				int b = n.getKey().getStop();
				if (a==start && b==stop)
					re.add(n.getKey());
				if (stop>=n.getKey().getStart() && n.right != null)
					stack.push(n.right);
			}
		}
		return re;
	}

	
	public boolean hasIntervalsIntersecting(int start, int stop) {
		Stack<AugmentedTreeMap.Entry<I, ?>> stack = new Stack<AugmentedTreeMap.Entry<I,?>>();
		if (root!=null)
			stack.push(root);

		while (!stack.isEmpty()) {
			AugmentedTreeMap.Entry<I, ?> n = stack.pop();
			if (start<=getMaxStop(n)) {
				if (n.left!= null)
					stack.push(n.left);
				int a = n.getKey().getStart();
				int b = n.getKey().getStop();
				if (a<=stop && b>=start)
					return true;
				if (stop>=n.getKey().getStart() && n.right != null)
					stack.push(n.right);
			}
		}
		return false;
	}

	public <C extends Collection<? super I>> C getIntervalsIntersecting(int start, int stop, C re) {
		Stack<AugmentedTreeMap.Entry<I, ?>> stack = new Stack<AugmentedTreeMap.Entry<I,?>>();
		if (root!=null)
			stack.push(root);

		while (!stack.isEmpty()) {
			AugmentedTreeMap.Entry<I, ?> n = stack.pop();
			if (start<=getMaxStop(n)) {
				if (n.left!= null)
					stack.push(n.left);
				int a = n.getKey().getStart();
				int b = n.getKey().getStop();
				if (a<=stop && b>=start)
					re.add(n.getKey());
				if (stop>=n.getKey().getStart() && n.right != null)
					stack.push(n.right);
			}
		}
		return re;
	}

	public <C extends Map<? super I,? super D>> C getIntervalsIntersecting(int start, int stop, C re) {
		Stack<AugmentedTreeMap.Entry<I, D>> stack = new Stack<AugmentedTreeMap.Entry<I,D>>();
		if (root!=null)
			stack.push(root);

		while (!stack.isEmpty()) {
			AugmentedTreeMap.Entry<I, D> n = stack.pop();
			if (start<=getMaxStop(n)) {
				if (n.left!= null)
					stack.push(n.left);
				int a = n.getKey().getStart();
				int b = n.getKey().getStop();
				if (a<=stop && b>=start)
					re.put(n.getKey(),n.getValue());
				if (stop>=n.getKey().getStart() && n.right != null)
					stack.push(n.right);
			}
		}
		return re;
	}


	//	public Spliterator<Entry<I,D>> iterateIntervalsIntersecting(int start, int stop) {
	//		Stack<AugmentedTreeMap.Entry<I, D>> stack = new Stack<AugmentedTreeMap.Entry<I,D>>();
	//		if (root!=null)
	//			stack.push(root);
	//		
	//		return new Spliterator<Entry<I,D>>() {
	//
	//			@Override
	//			public boolean tryAdvance(
	//					Consumer<? super gedi.util.datastructure.tree.redblacktree.AugmentedTreeMap.Entry<I, D>> action) {
	//				while (!stack.isEmpty()) {
	//					AugmentedTreeMap.Entry<I, D> n = stack.pop();
	//					if (start<=getMaxStop(n)) {
	//						if (n.left!= null)
	//							stack.push(n.left);
	//						int a = n.getKey().getStart();
	//						int b = n.getKey().getStop();
	//						if (stop>=n.getKey().getStart() && n.right != null)
	//							stack.push(n.right);
	//						if (a<=stop && b>=start) { 
	//							action.accept(n);
	//							return true;
	//						}
	//					}
	//				}
	//				return false;
	//			}
	//
	//			@Override
	//			public Spliterator<gedi.util.datastructure.tree.redblacktree.AugmentedTreeMap.Entry<I, D>> trySplit() {
	//				return null;
	//			}
	//
	//			@Override
	//			public long estimateSize() {
	//				return Long.MAX_VALUE;
	//			}
	//
	//			@Override
	//			public int characteristics() {
	//				return IMMUTABLE|DISTINCT|NONNULL|ORDERED|SORTED;
	//			}
	//			
	//		};
	//
	//		
	//	}
	//	
	//	public Spliterator<Entry<I,D>> iterateIntervalsIntersecting(GenomicRegion region) {
	//		Stack<AugmentedTreeMap.Entry<I, D>> stack = new Stack<AugmentedTreeMap.Entry<I,D>>();
	//		
	//		
	//		return new Spliterator<Entry<I,D>>() {
	//			
	//			int currentPart = -1;
	//			int leftConstraint;
	//			int start;
	//			int stop;
	//			
	//			@Override
	//			public boolean tryAdvance(
	//					Consumer<? super gedi.util.datastructure.tree.redblacktree.AugmentedTreeMap.Entry<I, D>> action) {
	//				while (!stack.isEmpty()) {
	//					AugmentedTreeMap.Entry<I, D> n = stack.pop();
	//					if (start<=getMaxStop(n)) {
	//						if (n.left!= null)
	//							stack.push(n.left);
	//						int a = n.getKey().getStart();
	//						int b = n.getKey().getStop();
	//						if (stop>=n.getKey().getStart() && n.right != null)
	//							stack.push(n.right);
	//						if (a<=stop && b>=start && a>=leftConstraint) { 
	//							action.accept(n);
	//							return true;
	//						}
	//					}
	//				}
	//				if (++currentPart<region.getNumParts()) {
	//					start = region.getStart(currentPart);
	//					stop = region.getStop(currentPart);
	//					leftConstraint = currentPart>0?region.getEnd(currentPart-1):Integer.MIN_VALUE;
	//					if (root!=null)
	//						stack.push(root);
	//				}
	//				return false;
	//			}
	//
	//			@Override
	//			public Spliterator<gedi.util.datastructure.tree.redblacktree.AugmentedTreeMap.Entry<I, D>> trySplit() {
	//				return null;
	//			}
	//
	//			@Override
	//			public long estimateSize() {
	//				return Long.MAX_VALUE;
	//			}
	//
	//			@Override
	//			public int characteristics() {
	//				return IMMUTABLE|DISTINCT|NONNULL|ORDERED|SORTED;
	//			}
	//			
	//		};
	//
	//		
	//	}

	public Spliterator<Map.Entry<I,D>> iterateIntervalsIntersecting(int start, int stop, Predicate<I> filter) {
		
		class Re implements Spliterator<Map.Entry<I,D>> {
			Stack<AugmentedTreeMap.Entry<I, D>> stack = new Stack<AugmentedTreeMap.Entry<I,D>>();
			Entry<I,D> current = root!=null&&start<=getMaxStop(root)?root:null;
			@Override
			public boolean tryAdvance(
					Consumer<? super Map.Entry<I, D>> action) {
				boolean done = false;
				while (!done) {
					if(current!=null) {
						stack.push(current);
						if (current.left!=null && start<=getMaxStop(current.left))
							current = current.left;
						else
							current = null;
						
					} else {
						if (!stack.isEmpty()) {
							Entry<I, D> re = stack.pop();
							
							if (stop>=re.getKey().getStart() && re.right != null)
								current = re.right;
							
							int a = re.getKey().getStart();
							int b = re.getKey().getStop();
							if (a<=stop && b>=start && filter.test(re.getKey())) {
								action.accept(re);
								return true;
							}
						}
						else
							done = true;
					}
				} 
				return false;
			}

			@Override
			public Spliterator<Map.Entry<I, D>> trySplit() {
				// fill stack
				while(current!=null) {
					stack.push(current);
					if (current.left==null || start<=getMaxStop(current.left))
						current = current.left; 
				}
				if (stack.size()<=3)return null;
				
				Re re = new Re();
				// split stack
				for (int i=stack.size()/2; i<stack.size(); i++)
					re.stack.push(stack.get(i));
				for (int i=0; i<re.stack.size(); i++)
					stack.pop();
				
				// bottom part of stack to this, top part to re
				Stack<Entry<I, D>> save = stack;
				stack = re.stack;
				re.stack=save;
				
				return re;
				
			}

			@Override
			public long estimateSize() {
				return Long.MAX_VALUE;
			}

			@Override
			public int characteristics() {
				return IMMUTABLE|DISTINCT|NONNULL|ORDERED|SORTED;
			}

		};

		return new Re();

	}

	public Spliterator<Map.Entry<I,D>> iterateIntervalsIntersecting(GenomicRegion region, Predicate<I> filter) {
		

		class Re implements Spliterator<Map.Entry<I,D>> {

			Stack<AugmentedTreeMap.Entry<I, D>> stack = new Stack<AugmentedTreeMap.Entry<I,D>>();
			Entry<I,D> current = null;
			int currentPart = -1;
			int leftConstraint;
			int start;
			int stop;

			@Override
			public boolean tryAdvance(
					Consumer<? super Map.Entry<I, D>> action) {
				boolean done = false;
				while (!done) {
					if(current!=null) {
						stack.push(current);
						if (current.left!=null && start<=getMaxStop(current.left))
							current = current.left;
						else
							current = null;
						
					} else {
						if (!stack.isEmpty()) {
							Entry<I, D> re = stack.pop();
							
							if (stop>=re.getKey().getStart() && re.right != null)
								current = re.right;
							
							int a = re.getKey().getStart();
							int b = re.getKey().getStop();
							if (a<=stop && b>=start && a>=leftConstraint && filter.test(re.getKey())) {
								action.accept(re);
								return true;
							}
						}
						else
							done = true;
					}
				} 
				if (++currentPart<region.getNumParts()) {
					start = region.getStart(currentPart);
					stop = region.getStop(currentPart);
					leftConstraint = currentPart>0?region.getEnd(currentPart-1):Integer.MIN_VALUE;
					current = root!=null&&start<=getMaxStop(root)?root:null;
					return tryAdvance(action);
				}
				return false;
			}

			@Override
			public Spliterator<Map.Entry<I, D>> trySplit() {
				// fill stack
				while(current!=null) {
					stack.push(current);
					if (current.left==null || start<=getMaxStop(current.left))
						current = current.left; 
				}
				if (stack.size()<=3)return null;
				
				Re re = new Re();
				re.currentPart = currentPart;
				re.start = start;
				re.stop = stop;
				re.leftConstraint = leftConstraint;
				
				// split stack
				for (int i=stack.size()/2; i<stack.size(); i++)
					re.stack.push(stack.get(i));
				for (int i=0; i<re.stack.size(); i++)
					stack.pop();
				
				// bottom part of stack to this, top part to re
				Stack<Entry<I, D>> save = stack;
				stack = re.stack;
				re.stack=save;
				
				return re;
			}

			@Override
			public long estimateSize() {
				return Long.MAX_VALUE;
			}

			@Override
			public int characteristics() {
				return IMMUTABLE|DISTINCT|NONNULL|ORDERED|SORTED;
			}

		};

		return new Re();

	}

	public void forEachIntervalsIntersecting(int start, int stop, Consumer<Entry<I,D>> consumer) {
		Stack<AugmentedTreeMap.Entry<I, D>> stack = new Stack<AugmentedTreeMap.Entry<I,D>>();
		if (root!=null)
			stack.push(root);

		while (!stack.isEmpty()) {
			AugmentedTreeMap.Entry<I, D> n = stack.pop();
			if (start<=getMaxStop(n)) {
				if (n.left!= null)
					stack.push(n.left);
				int a = n.getKey().getStart();
				int b = n.getKey().getStop();
				if (a<=stop && b>=start)
					consumer.accept(n);
				if (stop>=n.getKey().getStart() && n.right != null)
					stack.push(n.right);
			}
		}
	}

	public <C extends Collection<? super I>> C getIntervalsNeighbor(int start, int stop, C re) {
		int size = re.size();
		getIntervalsIntersecting(start, stop, re);
		if (size<re.size()) return re;

		ArrayList<I> lbuff = getIntervalsLeftNeighbor(start, stop,  new ArrayList<I>());
		if (lbuff.size()==0) return getIntervalsRightNeighbor(start, stop, re);
		
		ArrayList<I> rbuff = getIntervalsRightNeighbor(start, stop, new ArrayList<I>());
		if (rbuff.size()==0) return re;
		
		int ldist = start-lbuff.get(0).getStop();
		int rdist = rbuff.get(0).getStart()-stop;
		if (ldist<=rdist) 
			re.addAll(lbuff);
		if (ldist>=rdist) 
			re.addAll(rbuff);
		return re;
	}

	/**
	 * Gets the rightmost intervals (w.r.t. theirs stops) that are either left to start or all intersecting
	 * @param start
	 * @param stop
	 * @param re
	 * @return
	 */
	public <C extends Collection<? super I>> C getIntervalsLeftNeighbor(int start, int stop, C re) {
		int size = re.size();
		getIntervalsIntersecting(start, stop, re);
		if (size<re.size()) return re;



		Stack<AugmentedTreeMap.Entry<I, ?>> stack = new Stack<AugmentedTreeMap.Entry<I,?>>();
		if (root!=null)
			stack.push(root);

		int curr = Integer.MIN_VALUE;
		HashSet<AugmentedTreeMap.Entry<I, ?>> cand = new HashSet<AugmentedTreeMap.Entry<I,?>>();
		while (!stack.isEmpty()) {
			AugmentedTreeMap.Entry<I, ?> n = stack.pop();
			int b = n.key.getStop();
			if (b<start) {
				if (b>curr) cand.clear();
				if (b>=curr) {
					curr = b;
					cand.add(n);
				}
			}

			if (start<=getMaxStop(n)) {
				if (n.left!= null)
					stack.push(n.left);
				if (start>=n.getKey().getStart() && n.right != null)
					stack.push(n.right);
			} else {
				b = getMaxStop(n);
				if (b>curr) cand.clear();
				if (b>=curr) {
					curr = b;
					cand.add(n);
				}
			}
		}

		if (curr==Integer.MIN_VALUE) return re; // no interval left of start
		stack.addAll(cand);

		while (!stack.isEmpty()) {
			AugmentedTreeMap.Entry<I, ?> n = stack.pop();
			if (n.getKey().getStop()==curr)
				re.add(n.getKey());
			if (n.left!=null && getMaxStop(n.left)==curr)
				stack.push(n.left);
			if (n.right!=null && getMaxStop(n.right)==curr)
				stack.push(n.right);
		}
		return re;
	}

	/**
	 * Gets the leftmost intervals (w.r.t. their starts), that are either right to stop or all intersecting
	 * @param start
	 * @param stop
	 * @param re
	 * @return
	 */
	public <C extends Collection<? super I>> C getIntervalsRightNeighbor(int start, int stop, C re) {
		int size = re.size();
		getIntervalsIntersecting(start, stop, re);
		if (size<re.size()) return re;

		Stack<AugmentedTreeMap.Entry<I, ?>> stack = new Stack<AugmentedTreeMap.Entry<I,?>>();
		if (root!=null)
			stack.push(root);

		int curr = Integer.MAX_VALUE;
		AugmentedTreeMap.Entry<I, ?> best = null;
		while (!stack.isEmpty()) {
			AugmentedTreeMap.Entry<I, ?> n = stack.pop();
			int a = n.getKey().getStart();
			if (a>stop &&a<curr) {
				curr=a;
				best = n;
			}

			if (stop<a) {
				if (n.left!=null)
					stack.push(n.left);
			}
			else{
				if (n.right!=null)
					stack.push(n.right);
			}
		}
		if (best==null) return re; // no hit

		stack.push(best);
		while (!stack.isEmpty()) {
			AugmentedTreeMap.Entry<I, ?> n = stack.pop();
			int a = n.getKey().getStart();
			if (a==curr) {
				re.add(n.key);
				if (n.left!=null)
					stack.push(n.left);
				if (n.right!=null)
					stack.push(n.right);
			}
		}

		return re;
	}


	public <C extends Collection<? super I>> C getIntervalsSpannedBy(int start, int stop, C re) {
		Stack<AugmentedTreeMap.Entry<I, ?>> stack = new Stack<AugmentedTreeMap.Entry<I,?>>();
		if (root!=null)
			stack.push(root);

		while (!stack.isEmpty()) {
			AugmentedTreeMap.Entry<I, ?> n = stack.pop();
			if (start<=getMaxStop(n)) {
				if (n.left!= null)
					stack.push(n.left);
				int a = n.getKey().getStart();
				int b = n.getKey().getStop();
				if (start<=a && b<=stop)
					re.add(n.getKey());
				if (stop>=n.getKey().getStart() && n.right != null)
					stack.push(n.right);
			}
		}
		return re;
	}


	public int[] getCutPoints() {
		IntArrayList re = new IntArrayList(size()*2);
		for (Interval i : this.keySet()) {
			re.add(i.getStart());
			re.add(i.getStop()+1);
		}
		re.sort();
		re.unique();
		return re.toIntArray();
	}

	
	public GenomicRegion toGenomicRegion(Function<I,GenomicRegion> intronFun) {
		GenomicRegion re = new ArrayGenomicRegion();
		for (I i : this.keySet()) 
			re = re.union(intronFun.apply(i));
		return re;
	}

	@Override
	public int getStart() {
		return firstKey().getStart();
	}


	@Override
	public int getStop() {
		return getMaxStop(root);
	}


	private int getMaxStop(AugmentedTreeMap.Entry<I, ?> node) {
		return ((MutableInteger)node.augmentation).N;
	}

	/**
	 * Iterates over overlapping groups of intervals
	 * @return
	 */
	public GroupIterator groupIterator() {
		return groupIterator(0);
	}

	/**
	 * Iterates over overlapping groups of intervals; overlap with a tolerance means that two intervals overlap as long as their distance is smaller or equal to the given distance
	 * @return
	 */
	public GroupIterator groupIterator(int tolerance) {
		return new GroupIterator(tolerance);
	}


	public class GroupIterator implements ExtendedIterator<NavigableMap<I,D>> {

		private Iterator<I> it;
		private I first;
		private int intervalMax;

		private NavigableMap<I,D> next;
		private int tolerance;

		public GroupIterator(int tolerance) {
			this.tolerance = tolerance;
			it = keyIterator();
			first = it.hasNext()?it.next():null;
			intervalMax = first!=null?first.getStop():-1;
		}

		@Override
		public boolean hasNext() {
			lookAhead();
			return next!=null;
		}

		@Override
		public NavigableMap<I,D> next() {
			lookAhead();
			NavigableMap<I,D> re = next;
			next = null;
			return re;
		}
		

		private void lookAhead() {
			if (next==null && first!=null) {
				while (it.hasNext()) {
					I current = it.next();
					if (intervalMax+tolerance<current.getStart()) {// new group
						next = subMap(first, true, current, false);
						first = current;
						intervalMax = Math.max(current.getStop(), intervalMax);
						return;
					}
					intervalMax = Math.max(current.getStop(), intervalMax);
				}
				next = tailMap(first, true);
				first = null;
			}
		}

		@Override
		public void remove() {}

	}


	public void check() {
		AugmentedTreeMap.Entry<I,?> node = root;
		check(node);
	}

	private static <I extends Interval> int check(AugmentedTreeMap.Entry<I, ?> node) {
		int c = node.getKey().getStop();
		if (node.left!=null)
			c = Math.max(c,check(node.left));
		if (node.right!=null)
			c = Math.max(c,check(node.right));
		if (c!=((MutableInteger)node.augmentation).N)
			throw new RuntimeException("Inconsistent!");
		return c;
	}


	public IntervalTree<I,D> clone() {
		return new IntervalTree<I,D>(this, reference);
	}

	public static class IntervalTreeAugmentation<I extends Interval> implements MapAugmentation<I,MutableInteger> {

		@Override
		public MutableInteger computeAugmentation(AugmentedTreeMap.Entry<I,?> n, MutableInteger currentAugmentation, MutableInteger leftAugmentation,
				MutableInteger rightAugmentation) {
			currentAugmentation.N = n.getKey().getStop();
			if (leftAugmentation!=null)
				currentAugmentation.N = Math.max(currentAugmentation.N,leftAugmentation.N);
			if (rightAugmentation!=null)
				currentAugmentation.N = Math.max(currentAugmentation.N,rightAugmentation.N);
			return currentAugmentation;
		}

		@Override
		public MutableInteger init(I key) {
			return new MutableInteger(key.getStop());
		}

	}


	public static class CountIntervalsCollection extends AbstractCollection<Interval> implements Interval{

		private boolean fractional;
		private int start;
		private int stop;

		private double sum;

		public CountIntervalsCollection(boolean fractional, int start, int stop) {
			this.fractional = fractional;
			this.start = start;
			this.stop = stop;
			this.sum= 0;
		}

		public CountIntervalsCollection reset(boolean fractional, int start, int stop) {
			this.fractional = fractional;
			this.start = start;
			this.stop = stop;
			this.sum= 0;
			return this;
		}

		@Override
		public int getStart() {
			return start;
		}

		@Override
		public int getStop() {
			return stop;
		}

		public double getCount() {
			return sum;
		}

		@Override
		public boolean add(Interval e) {
			if (fractional) {
				float inter = MathUtils.getIntersection(e, this);
				sum+=inter/(stop-start+1);
			} else {
				sum++;
			}
			return true;
		}

		@Override
		public Iterator<Interval> iterator() {
			return null;
		}

		@Override
		public int size() {
			return -1;
		}

	}

	public static class GetLastIntervalCollection<T extends Interval> extends AbstractCollection<T> {

		private T re;

		public GetLastIntervalCollection<T> reset() {
			re = null;
			return this;
		}

		@Override
		public boolean add(T e) {
			re=e;
			return true;
		}

		@Override
		public Iterator<T> iterator() {
			return FunctorUtils.singletonIterator(re);
		}

		public T get() {
			return re;
		}

		@Override
		public int size() {
			return re==null?0:1;
		}

	}


}
