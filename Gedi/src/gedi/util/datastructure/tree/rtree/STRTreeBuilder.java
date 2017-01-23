package gedi.util.datastructure.tree.rtree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class STRTreeBuilder<T extends SpatialObject> {

	private static final int DEFAULT_NODE_CAPACITY = 10;

	private static Comparator<SpatialObject> xComparator =
			new Comparator<SpatialObject>() {
		public int compare(SpatialObject o1, SpatialObject o2) {
			return Double.compare(o1.getBounds().getCenterX(),o2.getBounds().getCenterX());
		}
	};
	private static Comparator<SpatialObject> yComparator =
			new Comparator<SpatialObject>() {
		public int compare(SpatialObject o1, SpatialObject o2) {
			return Double.compare(o1.getBounds().getCenterY(),o2.getBounds().getCenterY());
		}
	};


	private int nodeCapacity;
	private STRTree<T> tree;


	/**
	 * Creates parent nodes, grandparent nodes, and so forth up to the root
	 * node, for the data that has been inserted into the tree. Can only be
	 * called once, and thus can be called only after all of the data has been
	 * inserted into the tree.
	 */
	public STRTreeBuilder(Collection<T> bulk) {
		STRInnerNode root = bulk.isEmpty()
				? new STRInnerNode()
		: createHigherLevels(bulk);
				tree = new STRTree<T>(root);
	}

	public STRTree<T> getTree() {
		return tree;
	}

	/**
	 * Creates the levels higher than the given level
	 *
	 * @param boundablesOfALevel
	 *            the level to build on
	 * @param level
	 *            the level of the Boundables, or -1 if the boundables are item
	 *            boundables (that is, below level 0)
	 * @return the root, which may be a ParentNode or a LeafNode
	 */
	private STRInnerNode createHigherLevels(Collection<? extends SpatialObject> boundablesOfALevel) {
		assert(!boundablesOfALevel.isEmpty());
		ArrayList<STRInnerNode> parentBoundables = createParentBoundables(boundablesOfALevel);
		if (parentBoundables.size() == 1) {
			return parentBoundables.get(0);
		}
		return createHigherLevels(parentBoundables);
	}

	/**
	 * Creates the parent level for the given child level. First, orders the items
	 * by the x-values of the midpoints, and groups them into vertical slices.
	 * For each slice, orders the items by the y-values of the midpoints, and
	 * group them into runs of size M (the node capacity). For each run, creates
	 * a new (parent) node.
	 */
	private ArrayList<STRInnerNode> createParentBoundables(Collection<? extends SpatialObject> childBoundables) {
		assert(!childBoundables.isEmpty());
		int minLeafCount = (int) Math.ceil((childBoundables.size() / (double) nodeCapacity));
		ArrayList<SpatialObject> sortedChildBoundables = new ArrayList<>(childBoundables);
		Collections.sort(sortedChildBoundables, xComparator);
		ArrayList<? extends SpatialObject>[] verticalSlices = verticalSlices(sortedChildBoundables,
				(int) Math.ceil(Math.sqrt(minLeafCount)));
		return createParentBoundablesFromVerticalSlices(verticalSlices);
	}

	private ArrayList<STRInnerNode> createParentBoundablesFromVerticalSlices(ArrayList<? extends SpatialObject>[] verticalSlices) {
		assert(verticalSlices.length > 0);
		ArrayList<STRInnerNode> parentBoundables = new ArrayList<>();
		for (int i = 0; i < verticalSlices.length; i++) {
			parentBoundables.addAll(
					createParentBoundablesFromVerticalSlice(verticalSlices[i]));
		}
		return parentBoundables;
	}

	/**
	 * @param childBoundables Must be sorted by the x-value of the envelope midpoints
	 */
	protected ArrayList<? extends SpatialObject>[] verticalSlices(Collection<? extends SpatialObject> childBoundables, int sliceCount) {
		int sliceCapacity = (int) Math.ceil(childBoundables.size() / (double) sliceCount);
		ArrayList<SpatialObject>[] slices = new ArrayList[sliceCount];
		Iterator<? extends SpatialObject> i = childBoundables.iterator();
		for (int j = 0; j < sliceCount; j++) {
			slices[j] = new ArrayList<>();
			int boundablesAddedToSlice = 0;
			while (i.hasNext() && boundablesAddedToSlice < sliceCapacity) {
				SpatialObject childBoundable = i.next();
				slices[j].add(childBoundable);
				boundablesAddedToSlice++;
			}
		}
		return slices;
	}


	/**
	 * Sorts the childBoundables then divides them into groups of size M, where
	 * M is the node capacity.
	 */
	private ArrayList<STRInnerNode> createParentBoundablesFromVerticalSlice(Collection<? extends SpatialObject> childBoundables) {
		assert(!childBoundables.isEmpty());
		ArrayList<STRInnerNode> parentBoundables = new ArrayList<>();
		parentBoundables.add(new STRInnerNode());
		ArrayList<SpatialObject> sortedChildBoundables = new ArrayList<>(childBoundables);

		Collections.sort(sortedChildBoundables, yComparator);
		for (SpatialObject ch : sortedChildBoundables) {
			if (last(parentBoundables).getChildren().size() == nodeCapacity) {
				parentBoundables.add(new STRInnerNode());
			}
			last(parentBoundables).getChildren().add(ch);
		}
		return parentBoundables;
	}

	private final static STRInnerNode last(ArrayList<STRInnerNode> l){
		return l.get(l.size()-1);
	}

}
