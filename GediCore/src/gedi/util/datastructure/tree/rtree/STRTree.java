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

package gedi.util.datastructure.tree.rtree;

import gedi.util.datastructure.tree.priorityQueue.FibonacciHeap;
import gedi.util.datastructure.tree.priorityQueue.PriorityQueueEntry;
import gedi.util.mutable.MutablePair;

import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Stack;

public class STRTree<T extends SpatialObject> {


	STRInnerNode root;

	STRTree(STRInnerNode root) {
		this.root = root;
	}


	public int computeSize() {
		Stack<STRInnerNode> dfs = new Stack<STRInnerNode>();
		dfs.add(root);
		int size = 0;
		while (!dfs.isEmpty()) {
			STRInnerNode n = dfs.pop();
			for (SpatialObject ch : n.getChildren()) {
				if (ch instanceof STRInnerNode)
					dfs.push((STRInnerNode) ch);
				else
					size++;
			}
		}
		return size;
	}
	
	/**
	 * Leaves are not counted!
	 * @return
	 */
	public int computeDepth() {
		if (root.getChildren().isEmpty()) return 0;
		int d = -1;
		for (STRInnerNode n = root; n!=null; n=getFirstChild(n), d++);
		return d;
	}


	private STRInnerNode getFirstChild(STRInnerNode n) {
		SpatialObject re = n.getChildren().get(0);
		if (re instanceof STRInnerNode)
			return (STRInnerNode) re;
		return null;
	}


	public <C extends Collection<T>> C query(Rectangle2D bounds, C re) {
		if (bounds==null) throw new IllegalArgumentException("Bounds may not be null!");
		if (root.getChildren().isEmpty() || bounds.isEmpty()) return re;
		if (root.getBounds().intersects(bounds)) {
			query(bounds, root, re);
		}
		return re;
	}

	@SuppressWarnings("unchecked")
	private void query(Rectangle2D bounds, STRInnerNode node, Collection<T> re) {
		for (SpatialObject ch : node.getChildren()) {
			if (!ch.getBounds().intersects(bounds))
				continue;
			if (ch instanceof STRInnerNode) 
				query(bounds, (STRInnerNode) ch, re);
			else 
				re.add((T)ch);
		}
	}

	public T nearestNeighbour(SpatialDistance distance, Rectangle2D rect) {
		return (T) nearestNeighbour(new MutablePair<SpatialObject,SpatialObject>(root,new DefaultSpatialObject(rect)),distance,Double.POSITIVE_INFINITY).Item1;
	}


	private MutablePair<SpatialObject, SpatialObject> nearestNeighbour(MutablePair<SpatialObject, SpatialObject> initBndPair, SpatialDistance distance, double maxDistance) {
		double distanceLowerBound = maxDistance;
		MutablePair<SpatialObject, SpatialObject> minPair = null;

		// initialize internal structures
		FibonacciHeap<MutablePair<SpatialObject, SpatialObject>> priQ = new FibonacciHeap<MutablePair<SpatialObject, SpatialObject>>();

		// initialize queue
		priQ.insert(distance.distance(initBndPair.Item1.getBounds(), initBndPair.Item2.getBounds()), initBndPair);

		while (! priQ.isEmpty() && distanceLowerBound > 0.0) {
			// pop head of queue and expand one side of pair
			PriorityQueueEntry<MutablePair<SpatialObject, SpatialObject>> ent = priQ.deleteMin();
			MutablePair<SpatialObject, SpatialObject> bndPair = ent.getObject();
			double currentDistance = ent.getKey();

			/**
			 * If the distance for the first node in the queue
			 * is >= the current minimum distance, all other nodes
			 * in the queue must also have a greater distance.
			 * So the current minDistance must be the true minimum,
			 * and we are done.
			 */
			if (currentDistance >= distanceLowerBound) 
				break;  

			/**
			 * If the pair members are leaves
			 * then their distance is the exact lower bound.
			 * Update the distanceLowerBound to reflect this
			 * (which must be smaller, due to the test 
			 * immediately prior to this). 
			 */
			if (!(bndPair.Item1 instanceof STRInnerNode) && !(bndPair.Item2 instanceof STRInnerNode)) {
				// assert: currentDistance < minimumDistanceFound
				distanceLowerBound = currentDistance;
				minPair = bndPair;
			}
			else {
				// testing - does allowing a tolerance improve speed?
				// Ans: by only about 10% - not enough to matter
				/*
	        double maxDist = bndPair.getMaximumDistance();
	        if (maxDist * .99 < lastComputedDistance) 
	          return;
	        //*/

				/**
				 * Otherwise, expand one side of the pair,
				 * (the choice of which side to expand is heuristically determined) 
				 * and insert the new expanded pairs into the queue
				 */
				expandToQueue(bndPair, priQ, distance, distanceLowerBound);
			}
		}

		return minPair;
	}

	private static final void expandToQueue(MutablePair<SpatialObject, SpatialObject> pair, 
			FibonacciHeap<MutablePair<SpatialObject, SpatialObject>> priQ, SpatialDistance distance, double minDistance) {

		boolean isComp1 = pair.Item1 instanceof STRInnerNode;
		boolean isComp2 = pair.Item2 instanceof STRInnerNode;

		/**
		 * HEURISTIC: If both boundable are composite,
		 * choose the one with largest area to expand.
		 * Otherwise, simply expand whichever is composite.
		 */
		if (isComp1 && isComp2) {
			double a1 = pair.Item1.getBounds().getWidth()*pair.Item1.getBounds().getHeight();
			double a2 = pair.Item2.getBounds().getWidth()*pair.Item2.getBounds().getHeight();
			if (a1 > a2) {
				expand((STRInnerNode) pair.Item1, pair.Item2, priQ, distance, minDistance);
				return;
			}
			else {
				expand((STRInnerNode) pair.Item2, pair.Item1, priQ, distance, minDistance);
				return;
			}
		}
		else if (isComp1) {
			expand((STRInnerNode) pair.Item1, pair.Item2, priQ, distance, minDistance);
			return;
		}
		else if (isComp2) {
			expand((STRInnerNode) pair.Item2, pair.Item1, priQ, distance, minDistance);
			return;
		}

		throw new IllegalArgumentException("neither boundable is composite");
	}
	private static final void expand(STRInnerNode bndComposite, SpatialObject other,
			FibonacciHeap<MutablePair<SpatialObject, SpatialObject>> priQ, SpatialDistance distance, double minDistance)
	{

		for (SpatialObject ch : bndComposite.getChildren()) {
			double dist = distance.distance(ch.getBounds(),other.getBounds());
			// only add to queue if this pair might contain the closest points
			// MD - it's actually faster to construct the object rather than called distance(child, bndOther)!
			if (dist < minDistance) {
				priQ.insert(dist, new MutablePair<SpatialObject,SpatialObject>(ch,other));
			}
		}
	}


}
