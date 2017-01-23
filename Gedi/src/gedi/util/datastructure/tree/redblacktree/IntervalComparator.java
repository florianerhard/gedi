package gedi.util.datastructure.tree.redblacktree;

import gedi.util.GeneralUtils;

import java.util.Comparator;

public class IntervalComparator implements Comparator<Interval> {

	@Override
	public int compare(Interval o1, Interval o2) {
		if (o1 instanceof Comparable && o2 instanceof Comparable)
			return ((Comparable)o1).compareTo(o2);
		int re = Integer.compare(o1.getStart(), o2.getStart());
		if (re==0) re = Integer.compare(o1.getStop(), o2.getStop());
		return re;
	}

}
