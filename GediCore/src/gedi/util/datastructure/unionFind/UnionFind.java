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

package gedi.util.datastructure.unionFind;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

public class UnionFind<T> {

	private HashMap<T,Integer> map;
	private int[] size;
	private int[] parent;
	
	public UnionFind(Collection<? extends T> items) {
		int n = items.size();
		size = new int[n];
		parent = new int[n];
		for (int i=0; i<n; i++) {
			size[i] = 1;
			parent[i] = i;
		}
		int index = 0;
		map = new HashMap<T, Integer>();
		for (T item : items)
			map.put(item, index++);
	}
	
	public int find(T item) {
		int i=map.get(item);
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
	
	/**
	 * Gets how many sets were unified
	 * @param n
	 * @return
	 */
	public int unionAll(Iterable<T> n) {
		Iterator<T> it = n.iterator();
		if (!it.hasNext()) return 0;
		T f = it.next();
		int re = 0;
		while (it.hasNext()) {
			if (union(f, it.next()))
				re++;
		}
		return re;
	}
	
	/**
	 * Gets whether or not to sets were unified.
	 * @param i1
	 * @param i2
	 * @return
	 */
	public boolean union(T i1, T i2) {
		int i=find(i1);
		int j=find(i2);
		if (i==j) return false;
		int root = size[i]>size[j]?i:j;
		int child = size[i]>size[j]?j:i;
		parent[child]=root;
		size[root]+=size[child];
		return true;
	}
	
	public ArrayList<T>[] getGroups() {
		return getGroups(new ArrayList<T>());
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
	public <C extends Collection<T>> C[] getGroups(C prototype)  {
		HashMap<Integer,C> map = new HashMap<Integer, C>();
		for (T t : this.map.keySet()) {
			int g = find(t);
			C c = map.get(g);
			if (c==null)
				try {
					map.put(g,c = (C) prototype.getClass().newInstance());
				} catch (InstantiationException e) {
					throw new RuntimeException(e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			c.add(t);
		}
		return map.values().toArray((C[]) Array.newInstance(prototype.getClass(), map.size()));
	}

	
	
	
}
