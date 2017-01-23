package gedi.util.datastructure.unionFind;

import gedi.util.datastructure.collections.intcollections.IntArrayList;

public class IntUnionFind {

	private int[] size;
	private int[] parent;
	
	public IntUnionFind(int n) {
		size = new int[n];
		parent = new int[n];
		for (int i=0; i<n; i++) {
			size[i] = 1;
			parent[i] = i;
		}
	}
	
	public int find(int i) {
		int j=i;
		for (;parent[j]!=j; j=parent[j]);
		int root = j;
		j=i;
		int p;
		while (parent[j]!=j) {
			p=parent[j];
			parent[j]=root;
			j=p;
		}
		return root;
	}
	
	public int union(int i, int j) {
		int root = size[i]>size[j]?i:j;
		int child = size[i]>size[j]?j:i;
		parent[child]=root;
		size[root]+=size[child];
		return root;
	}
	
	/**
	 * C must have public empty constructor
	 * @param <C>
	 * @param prototype
	 * @return
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	@SuppressWarnings("unchecked")
	public IntArrayList[] getGroups() {
		IntArrayList[] map = new IntArrayList[size.length];
		for (int t=0; t<size.length; t++) {
			int g = find(t);
			IntArrayList c = map[g];
			if (c==null)
				c = map[g] = new IntArrayList();
			c.add(t);
		}
		int r = 0;
		for (int i=0; i<map.length; i++) 
			if (map[i]!=null) 
				r++;
		IntArrayList[] re = new IntArrayList[r];
		r = 0;
		for (int i=0; i<map.length; i++) 
			if (map[i]!=null) 
				re[r++] = map[i];
		return re;
	}

	
}
