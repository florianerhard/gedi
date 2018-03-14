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
import gedi.util.FunctorUtils;
import gedi.util.MathUtils;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.mutable.MutableInteger;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Stack;
import java.util.function.Consumer;


public class IntervalTreeSet<I extends Interval> extends AugmentedTreeSet<I,MutableInteger> implements Interval {

	
	private static final long serialVersionUID = 9064248848470532710L;
	private ReferenceSequence ref;
	
	public IntervalTreeSet(ReferenceSequence ref) {
		super(new IntervalComparator(),new IntervalTreeAugmentation<I>());
		this.ref = ref;
	}
	
	public IntervalTreeSet(Collection<? extends I> map, ReferenceSequence ref) {
		super(new IntervalComparator(),new IntervalTreeAugmentation<I>());
		this.ref = ref;
		addAll(map);
	}

	public int countIntervalsSpanning(int p) {
		Stack<AugmentedTreeMap.Entry<I, ?>> stack = new Stack<AugmentedTreeMap.Entry<I,?>>();
		if (((AugmentedTreeMap<I,?,MutableInteger>)m).root!=null)
			stack.push(((AugmentedTreeMap<I,?,MutableInteger>)m).root);
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
		if (((AugmentedTreeMap<I,?,MutableInteger>)m).root!=null)
			stack.push(((AugmentedTreeMap<I,?,MutableInteger>)m).root);

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
		if (((AugmentedTreeMap<I,?,MutableInteger>)m).root!=null)
			stack.push(((AugmentedTreeMap<I,?,MutableInteger>)m).root);

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
		if (((AugmentedTreeMap<I,?,MutableInteger>)m).root!=null)
			stack.push(((AugmentedTreeMap<I,?,MutableInteger>)m).root);

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
		if (((AugmentedTreeMap<I,?,MutableInteger>)m).root!=null)
			stack.push(((AugmentedTreeMap<I,?,MutableInteger>)m).root);

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


	public <C extends Collection<? super I>> C getIntervalsIntersecting(int start, int stop, C re) {
		Stack<AugmentedTreeMap.Entry<I, ?>> stack = new Stack<AugmentedTreeMap.Entry<I,?>>();
		if (((AugmentedTreeMap<I,?,MutableInteger>)m).root!=null)
			stack.push(((AugmentedTreeMap<I,?,MutableInteger>)m).root);

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
	
	public int forEachIntervalIntersecting(int start, int stop, Consumer<I> consumer) {
		Stack<AugmentedTreeMap.Entry<I, ?>> stack = new Stack<AugmentedTreeMap.Entry<I,?>>();
		if (((AugmentedTreeMap<I,?,MutableInteger>)m).root!=null)
			stack.push(((AugmentedTreeMap<I,?,MutableInteger>)m).root);

		int re = 0;
		while (!stack.isEmpty()) {
			AugmentedTreeMap.Entry<I, ?> n = stack.pop();
			if (start<=getMaxStop(n)) {
				if (n.left!= null)
					stack.push(n.left);
				int a = n.getKey().getStart();
				int b = n.getKey().getStop();
				if (a<=stop && b>=start) {
					consumer.accept(n.getKey());
					re++;
				}
				if (stop>=n.getKey().getStart() && n.right != null)
					stack.push(n.right);
			}
		}
		return re;
	}

	public <C extends Collection<? super I>> C getIntervalsNeighbor(int start, int stop, C re) {
		int size = re.size();
		getIntervalsIntersecting(start, stop, re);
		if (size<re.size()) return re;

		getIntervalsLeftNeighbor(start, stop, re);
		if (re.size()==0) return getIntervalsLeftNeighbor(start, stop, re);
		
		ArrayList<I> buff = getIntervalsLeftNeighbor(start, stop, new ArrayList<I>());
		if (buff.size()==0) return re;
		
		int ldist = start-((Interval)re.iterator().next()).getStop();
		int rdist = buff.get(0).getStart()-stop;
		if (ldist<rdist) return re;
		if (rdist<ldist) re.clear();
		re.addAll(buff);
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
		if (((AugmentedTreeMap<I,?,MutableInteger>)m).root!=null)
			stack.push(((AugmentedTreeMap<I,?,MutableInteger>)m).root);

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
		if (((AugmentedTreeMap<I,?,MutableInteger>)m).root!=null)
			stack.push(((AugmentedTreeMap<I,?,MutableInteger>)m).root);

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
		if (((AugmentedTreeMap<I,?,MutableInteger>)m).root!=null)
			stack.push(((AugmentedTreeMap<I,?,MutableInteger>)m).root);

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
		for (Interval i : this) {
			re.add(i.getStart());
			re.add(i.getStop()+1);
		}
		re.sort();
		re.unique();
		return re.toIntArray();
	}

	public ArrayGenomicRegion toCoordinates() {
		IntArrayList re = new IntArrayList(size()*2);
		Iterator<NavigableSet<I>> it = groupIterator();
		while (it.hasNext()) {
			NavigableSet<I> n = it.next();
			int min = n.iterator().next().getStart();
			int max = n.iterator().next().getStop();
			for (I i : n) {
				min = Math.min(i.getStart(), min);
				max = Math.max(i.getStop(), max);
			}
			re.add(min);
			re.add(max+1);
		}
		return new ArrayGenomicRegion(re);
	}

	@Override
	public int getStart() {
		return first().getStart();
	}


	@Override
	public int getStop() {
		return getMaxStop(((AugmentedTreeMap<I,?,MutableInteger>)m).root);
	}


	private int getMaxStop(AugmentedTreeMap.Entry<I, ?> node) {
		return ((MutableInteger)node.augmentation).N;
	}
	
	/**
	 * Iterates over overlapping groups of intervals
	 * @return
	 */
	public Iterator<NavigableSet<I>> groupIterator() {
		return groupIterator(0);
	}

	/**
	 * Iterates over overlapping groups of intervals; overlap with a tolerance means that two intervals overlap as long as their distance is smaller or equal to the given distance
	 * @return
	 */
	public Iterator<NavigableSet<I>> groupIterator(int tolerance) {
		return new GroupIterator(tolerance);
	}


	private class GroupIterator implements Iterator<NavigableSet<I>> {

		private Iterator<I> it;
		private I first;
		private int intervalMax;

		private NavigableSet<I> next;
		private int tolerance;

		public GroupIterator(int tolerance) {
			this.tolerance = tolerance;
			it = iterator();
			first = it.hasNext()?it.next():null;
			intervalMax = first!=null?first.getStop():-1;
		}

		@Override
		public boolean hasNext() {
			lookAhead();
			return next!=null;
		}

		@Override
		public NavigableSet<I> next() {
			lookAhead();
			NavigableSet<I> re = next;
			next = null;
			return re;
		}

		private void lookAhead() {
			if (next==null && first!=null) {
				while (it.hasNext()) {
					I current = it.next();
					if (intervalMax+tolerance<current.getStart()) {// new group
						next = subSet(first, true, current, false);
					first = current;
					intervalMax = Math.max(current.getStop(), intervalMax);
					return;
					}
					intervalMax = Math.max(current.getStop(), intervalMax);
				}
				next = tailSet(first, true);
				first = null;
			}
		}

		@Override
		public void remove() {}

	}


	public void check() {
		AugmentedTreeMap.Entry<I,?> node = ((AugmentedTreeMap<I,?,MutableInteger>)m).root;
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

	public ReferenceSequence getReference() {
		return ref;
	}

    public IntervalTreeSet<I> clone() {
    	return new IntervalTreeSet<I>(this,ref);
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
