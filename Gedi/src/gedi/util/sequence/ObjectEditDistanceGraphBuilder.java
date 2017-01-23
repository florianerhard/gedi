package gedi.util.sequence;

import gedi.util.datastructure.graph.SimpleDirectedGraph;

import java.util.HashSet;
import java.util.List;
import java.util.function.BiPredicate;

public class ObjectEditDistanceGraphBuilder<T> {

	private List<T>[] data;

	/**
	 * Lists must be sorted, not checked!
	 * @param reads
	 */
	public ObjectEditDistanceGraphBuilder(List<T>[] data) {
		this.data = data;
	}
	
	public SimpleDirectedGraph<List<T>> build() {
		SimpleDirectedGraph<List<T>> g = new SimpleDirectedGraph<List<T>>("MismatchGraph");
		
		
		for (int i=0; i<data.length; i++) {
			for (int j=i+1; j<data.length; j++) {
				if (isEditDistance1(data[i],data[j])) {
					g.addInteraction(data[i], data[j]);
					g.addInteraction(data[j], data[i]);
				}
			}
		}
		
		return g;
	}
	
	
	public SimpleDirectedGraph<Integer> buildIndex() {
		SimpleDirectedGraph<Integer> g = new SimpleDirectedGraph<Integer>("MismatchGraph");
		
		
		for (int i=0; i<data.length; i++) {
			for (int j=i+1; j<data.length; j++) {
				if (isEditDistance1(data[i],data[j])) {
					g.addInteraction(i, j);
					g.addInteraction(j, i);
				}
			}
		}
		
		return g;
	}
	
	public static <T> boolean isEditDistance1(List<T> a, List<T> b) {
		return isEditDistance1(a, b, (x,y)->x.equals(y));
	}

	/**
	 * isMismatch decides whether one item can directly edited to another item. E.g. when applied to aligned read variations,
	 * M2AC and M2AG would be ok, M2AC M3AG not!
	 * @param a
	 * @param b
	 * @param isMismatch
	 * @return
	 */
	public static <T> boolean isEditDistance1(List<T> a, List<T> b, BiPredicate<T, T> isMismatch) {
		HashSet<T> sa = new HashSet<T>(a);
		HashSet<T> sb = new HashSet<T>(b);
		sa.removeAll(b);
		sb.removeAll(a);
		
		if (sa.size()+sb.size()<2) 
			return true;
		if (sa.size()==1 && sb.size()==1 && isMismatch.test(sa.iterator().next(), sb.iterator().next())) 
			return true;
		
		return false;
	}
	
	
}
