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

package gedi.util.datastructure.graph;



import gedi.util.ArrayUtils;
import gedi.util.io.Directory;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.math.stat.RandomNumbers;
import gedi.util.mutable.MutableInteger;
import gedi.util.mutable.MutablePair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;



public class SimpleDirectedGraph<T> {



	private Comparator<T> comparator;
	private NodeComparator nodeComparator = new NodeComparator();
	private String name;
	private HashMap<T,AdjacencyNode<T>> sources;
	private HashMap<T,AdjacencyNode<T>> targets;
	
	@SuppressWarnings("unchecked")
	public SimpleDirectedGraph(String name) {
		this(name,(a,b)->((Comparable) a).compareTo(b));
	}
	
	public SimpleDirectedGraph(String name, Comparator<T> comparator) {
		this.name = name;
		this.comparator = comparator;
		sources = new HashMap<T, AdjacencyNode<T>>();
	}
	
	public void setTargetIndexed(boolean indexed) {
		if (indexed^isTargetIndexed())
			targets = indexed?buildIndex():null;
	}
	
	private HashMap<T, AdjacencyNode<T>> buildIndex() {
		HashMap<T,AdjacencyNode<T>> re = new HashMap<T, AdjacencyNode<T>>();
		for (Entry<T, AdjacencyNode<T>> e :this.sources.entrySet()) {
			T m= e.getKey();
			for (AdjacencyNode<T> n =e.getValue(); n!=null; n=n.next) {
				if (n.getLabel()!=null)
					re.put(n.node, new LabelledAdjacencyNode(m, n.getLabel(), re.get(n.node)));
				else
					re.put(n.node, new AdjacencyNode<T>(m, re.get(n.node)));
			}
		}
		return re;
	}

	public boolean isTargetIndexed() {
		return targets!=null;
	}

	public void addInteraction(T source, T target) {
		sources.put(source, new AdjacencyNode<T>(target, sources.get(source)));
		if (this.targets!=null) 
			this.targets.put(target, new AdjacencyNode<T>(target, this.targets.get(target)));
	}
	
	public <L> void addInteraction(T source, T target, L label) {
		if (label==null) addInteraction(source, target);
		else {
			sources.put(source, new LabelledAdjacencyNode<T,L>(target, label, sources.get(source)));
			if (this.targets!=null) 
				this.targets.put(target, new LabelledAdjacencyNode<T,L>(target, label, this.targets.get(target)));
		}
	}

	public void unifyMultiEdges() {
		sortAdjacencyLists();
		for (Entry<T, AdjacencyNode<T>> en : this.sources.entrySet()) {
			AdjacencyNode<T> f = en.getValue();
			int num = 0;
			for (AdjacencyNode<T> n=en.getValue(); n!=null; n = n.next) {
				if (!n.node.equals(f.node)) {
					f.next = n;
					f = n;
					num++;
				}
			}
			f.next = null;
			f.successors=0;
			num++;
			
			for (AdjacencyNode<T> n=en.getValue(); n!=null; n = n.next)
				n.successors=--num;
		}
	}
	
	public void printNetwork() {
		for (Entry<T, AdjacencyNode<T>> en : this.sources.entrySet()) {
			System.out.print(en.getKey()+" -> [");
			for (AdjacencyNode<T> n=en.getValue(); n!=null; n = n.next)
				System.out.print(n==en.getValue()?n:","+n);
			System.out.println("]");
		}
	}
	
	public void retainEdges(Predicate<MutablePair<T, AdjacencyNode<T>>> pred) {
	
		MutablePair<T,AdjacencyNode<T>> p = new MutablePair<T, AdjacencyNode<T>>(null, null);
		
		for (Iterator<Entry<T, AdjacencyNode<T>>> it = this.sources.entrySet().iterator(); it.hasNext(); ) {
			Entry<T, AdjacencyNode<T>> en = it.next();
			AdjacencyNode<T> root = new AdjacencyNode<T>(null, en.getValue());
			AdjacencyNode<T> last = root;
			int num = 0;
			for (AdjacencyNode<T> n=en.getValue(); n!=null; n = n.next) {
				if (!pred.test(p.set(en.getKey(), n)))
					last.next=n.next;
				else {
					last = n;
					num++;
				}
			}
			
			for(AdjacencyNode<T> t=root.next; t!=null; t=t.next)
				t.successors=--num;
			if (num!=0)
				throw new RuntimeException();
				
			if (root.next==null)
				it.remove();
			else
				en.setValue(root.next);
		}
		
		if (isTargetIndexed())
			targets = buildIndex();
	}
	
	/**
	 * Precondition: this and n are simple networks (i.e. no multi edges!)
	 * PostCondition: this is again a simple network, n is in an undefined state!
	 * @param n
	 * @param unif
	 */
	public void mergeSimple(SimpleDirectedGraph<T> n) {
		sortAdjacencyLists();
		n.sortAdjacencyLists();
		
		for (Entry<T, AdjacencyNode<T>> en : n.sources.entrySet()) {
			// consider all sources, that are not in this	
			if (!this.sources.containsKey(en.getKey())) {
				this.sources.put(en.getKey(), en.getValue());
			} else {
				
				AdjacencyNode<T> t = this.sources.get(en.getKey());
				AdjacencyNode<T> o = en.getValue();
				AdjacencyNode<T> root = new AdjacencyNode<T>(null, t);
				AdjacencyNode<T> old_t = root;
				int num = t.successors+1;
				
				while (t!=null && o!=null) {
					int cmp = nodeComparator.compare(t, o);
					if (cmp==0) {
						old_t = t;
						t=t.next; o=o.next;
					} else if (cmp<0) { // t<o
						old_t = t;
						t=t.next;
					} else if (cmp>0) { // t>o
						AdjacencyNode<T> oldNext = o.next;
						old_t.next = o;
						o.next = t;
						old_t = o;
						o=oldNext;
						num++;
					}
				}
				num+=o!=null?o.successors+1:0;
				old_t.next=o;
				
				for(t=root.next; t!=null; t=t.next)
					t.successors=--num;
				if (num!=0)
					throw new RuntimeException();
					
				this.sources.put(en.getKey(), root.next);
			}
		}
		
		if (isTargetIndexed())
			targets = buildIndex();
	}
	
	
	public void transformNodes(UnaryOperator<T> trans) {
		HashMap<T,AdjacencyNode<T>> ns = new HashMap<T, AdjacencyNode<T>>();
		
		for (Entry<T, AdjacencyNode<T>> en : this.sources.entrySet()) {
			for (AdjacencyNode<T> n=en.getValue(); n!=null; n = n.next)
				n.node = trans.apply(n.node);
			
			T nk = trans.apply(en.getKey());
			if (ns.containsKey(nk)) 
				ns.get(nk).last().next = en.getValue();
			else
				ns.put(nk, en.getValue());
		}
		
		this.sources = ns;
		
		if (isTargetIndexed())
			targets = buildIndex();
	}
	
	public ArrayList<SimpleDirectedGraph<T>> getWeaklyConnectedComponents() {
		setTargetIndexed(true);
		HashSet<T> all = new HashSet<T>();
		all.addAll(sources.keySet());
		all.addAll(targets.keySet());
		
		
		ArrayList<SimpleDirectedGraph<T>> re = new ArrayList<SimpleDirectedGraph<T>>();
		
		while (!all.isEmpty()) {
			T s = all.iterator().next();
			
			SimpleDirectedGraph<T> g = new SimpleDirectedGraph<T>(getName()+".CC"+re.size(), comparator);
			Stack<T> dfs = new Stack<T>();
			dfs.push(s);
			while (!dfs.isEmpty()) {
				T node = dfs.pop();
				if (all.contains(node)) {
					for (AdjacencyNode<T> ne=sources.get(node); ne!=null; ne = ne.next) {
						if (all.contains(ne.node)) {
							g.addInteraction(node, ne.node,ne.getLabel());
							dfs.push(ne.node);
						}
					}
					for (AdjacencyNode<T> ne=targets.get(node); ne!=null; ne = ne.next) {
						if (all.contains(ne.node)) {
							g.addInteraction(ne.node, node, ne.getLabel());
							dfs.push(ne.node);
						}
					}
				}
				all.remove(node);
			}
			re.add(g);
		}
		
		return re;
	}

	
	public void dfs(T node, DfsVisitor<T> visitor, Set<T> discovered) {
		discovered.add(node);
		for (AdjacencyNode<T> n=sources.get(node); n!=null; n = n.next) {
			if (!discovered.contains(n.node)) {
				boolean go = visitor.down(node,n);
				if (go)
					dfs(n.node,visitor,discovered);
				visitor.up(n,node);
			}
		}
	}
	
	
	
	
	public LinkedList<T> getTopologicalOrder() {
		LinkedList<T> re = new LinkedList<T>();
		HashSet<T> nodes = new HashSet<T>();
		HashMap<T,MutableInteger> pred = annotatePredecessorCount();
		for (Iterator<Entry<T,MutableInteger>> it = pred.entrySet().iterator(); it.hasNext(); ) {
			Entry<T, MutableInteger> e = it.next();
			if (e.getValue().N==0) {
				nodes.add(e.getKey());
				it.remove();
			}
		}
		
		while (!nodes.isEmpty()) {
			T no = nodes.iterator().next();
			nodes.remove(no);
			re.add(no);
			for (AdjacencyNode<T> n=sources.get(no); n!=null; n = n.next) {
				MutableInteger p = pred.get(n.node);
				if (--p.N==0) {
					nodes.add(n.node);
					pred.remove(n.node);
				}
			}
		}
		
		if (pred.size()>0)
			return null;
		else
			return re;
	}

	public ArrayList<T> getNoPredecessorNodes() {
		HashMap<T,MutableInteger> pred = annotatePredecessorCount();
		ArrayList<T> re = new ArrayList<T>();
		for (Entry<T,MutableInteger> e : pred.entrySet()) {
			if (e.getValue().N==0)
				re.add(e.getKey());
		}
		return re;
	}
	
	public HashMap<T,MutableInteger> annotatePredecessorCount() {
		return annotatePredecessorCount(new HashMap<T, MutableInteger>());
	}
	
	public <M extends Map<T,MutableInteger>> M annotatePredecessorCount(M map) {
		map.clear();
		for (T s : this.sources.keySet())
			map.put(s, new MutableInteger(0));
		
		for (Iterator<Entry<T, AdjacencyNode<T>>> it = this.sources.entrySet().iterator(); it.hasNext(); ) {
			Entry<T, AdjacencyNode<T>> en = it.next();
			for (AdjacencyNode<T> n=en.getValue(); n!=null; n = n.next) {
				if (!map.containsKey(n.node))
					map.put(n.node, new MutableInteger(0));
				map.get(n.node).N++;
			}
		}
		return map;
	}
	
	public void sortAdjacencyLists() {
		ArrayList<AdjacencyNode<T>> sorter = new ArrayList<AdjacencyNode<T>>();
		for (Entry<T, AdjacencyNode<T>> en : this.sources.entrySet()) {
			for (AdjacencyNode<T> n=en.getValue(); n!=null; n = n.next)
				sorter.add(n);
			Collections.sort(sorter, nodeComparator);
			for (int i=0; i<sorter.size()-1; i++) {
				sorter.get(i).next=sorter.get(i+1);
				sorter.get(i).successors=sorter.size()-1-i;
			}
			sorter.get(sorter.size()-1).next = null;
			sorter.get(sorter.size()-1).successors = 0;
			
			en.setValue(sorter.get(0));
			sorter.clear();
		}
		if (isTargetIndexed())
			targets = buildIndex();
	}

	@SuppressWarnings("unchecked")
	public void permutateEdgesPreserveDegrees() {
		Map<T,Integer> s = getOutDegreeDistribution();
		
		AdjacencyNode<T>[] edges = new AdjacencyNode[getNumEdges()];
		int index = 0;
		for (Entry<T, AdjacencyNode<T>> en : this.sources.entrySet()) 
			for (AdjacencyNode<T> n=en.getValue(); n!=null; n = n.next)
				edges[index++] = n;
		ArrayUtils.shuffleSlice(edges, 0, edges.length);
		
		for (Entry<T, AdjacencyNode<T>> en : this.sources.entrySet()) {
			AdjacencyNode<T> v = null;
			int degree = s.get(en.getKey());
			for (int i=0; i<degree;i++) 
				v = edges[--index].update(v);
			en.setValue(v);
		}
		
		if (isTargetIndexed())
			targets = buildIndex();
	}
	
	@SuppressWarnings("unchecked")
	public void permutateEdgesKeepSourcesAndSinks() {
		HashSet<T> targetSet = new HashSet<T>();
		int numEdges = 0;
		for (Entry<T, AdjacencyNode<T>> en : this.sources.entrySet()) 
			for (AdjacencyNode<T> n=en.getValue(); n!=null; n = n.next) {
				targetSet.add(n.node);
				numEdges++;
			}
		
		Object[] sources = this.sources.keySet().toArray();
		Object[] targets = targetSet.toArray();
		
		clear();
		
		RandomNumbers s = new RandomNumbers();
		for (int i=0; i<numEdges; i++)
			addInteraction((T)sources[s.getUnif(0, sources.length)], (T)targets[s.getUnif(0, targets.length)]);
		
		if (isTargetIndexed())
			this.targets = buildIndex();
	}
	
	public Map<T, Integer> getOutDegreeDistribution() {
		return getOutDegreeDistribution(new HashMap<T,Integer>());
	}
	public Map<T, Integer> getOutDegreeDistribution(Map<T, Integer> map) {
		for (Entry<T, AdjacencyNode<T>> en : this.sources.entrySet()) 
			map.put(en.getKey(), en.getValue().successors+1);
		return map;
	}

	public Set<T> getSources() {
		return sources.keySet();
	}
	
	public AdjacencyNode<T> getTargets(T source) {
		return this.sources.get(source);
	}
	
	public Set<T> getTargetSet(T src) {
		return getTargetSet(src,new HashSet<T>());
	}
	
	public Set<T> getTargetSet(T src, Set<T> set) {
		for (AdjacencyNode<T> n=getTargets(src); n!=null; n=n.next)
			set.add(n.node);
		return set;
	}
	
	public int getNumTargets(T source) {
		AdjacencyNode<T> n = this.sources.get(source);
		if (n==null)return 0;
		else return n.successors+1;
	}
	
	public Set<T> getTargets() {
		if (!isTargetIndexed())
			throw new RuntimeException("Create an index first!");
		return targets.keySet();
	}
	
	public void clear() {
		sources.clear();
		if (isTargetIndexed())
			targets.clear();
	}
	
	public AdjacencyNode<T> getSources(T target) {
		if (!isTargetIndexed())
			throw new RuntimeException("Create an index first!");
		return this.targets.get(target);
	}
	
	
	public int getNumEdges(){
		int num = 0;
		for (Entry<T, AdjacencyNode<T>> en : sources.entrySet()) 
			num+=en.getValue().successors+1;
		return num;
	}
	
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void save(Directory dir) throws IOException {
		save(new File(dir,getName()+".network").getAbsolutePath());
	}
	
	public void save(String path) throws IOException {
		LineOrientedFile out = new LineOrientedFile(path);
		out.startWriting();
		StringBuilder sb = new StringBuilder();
		for (Entry<T, AdjacencyNode<T>> en : sources.entrySet()) {
			T mirna = en.getKey();
			sb.delete(0, sb.length());
			sb.append(mirna);
			sb.append("\t");
			int start = sb.length();
			for (AdjacencyNode<T> n=en.getValue(); n!=null; n = n.next) {
				sb.delete(start, sb.length());
				sb.append(n.node);
				out.writeLine(sb.toString());
			}
		}
		out.finishWriting();
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Entry<T, AdjacencyNode<T>> en : sources.entrySet()) {
			T s = en.getKey();
			for (AdjacencyNode<T> n=en.getValue(); n!=null; n = n.next) {
				sb.append(s);
				sb.append("\t");
				sb.append(n.node);
				sb.append("\n");
			}
		}
		if (sb.length()>0)
			sb.deleteCharAt(sb.length()-1);
		return sb.toString();
	}
	
	
	
	
	
	private class NodeComparator implements Comparator<AdjacencyNode<T>> {
		@Override
		public int compare(AdjacencyNode<T> o1, AdjacencyNode<T> o2) {
			return comparator.compare(o1.node, o2.node);
		}
	}
	
	public static interface DfsVisitor<T> {
		boolean down(T parent, AdjacencyNode<T> child);
		void up(AdjacencyNode<T> child,T parent);
	}
	
	
	public static class AdjacencyNode<T>  {
		public T node;
		public AdjacencyNode<T> next;
		public int successors;
		public AdjacencyNode(T node, AdjacencyNode<T> next) {
			this.node = node;
			this.next = next;
			successors = next==null?0:next.successors+1;
		}
		
		
		@Override
		public String toString() {
			return node.toString();
		}
		private AdjacencyNode<T> update(AdjacencyNode<T> next) {
			this.next = next;
			successors = next==null?0:next.successors+1;
			return this;
		}
		
		public <L> L getLabel() {
			return null;
		}


		public AdjacencyNode<T> last() {
			AdjacencyNode<T> re = this;
			for (;re.next!=null; re=re.next);
			return re;
		}
	}
	private static class LabelledAdjacencyNode<T,L> extends AdjacencyNode<T> {
		public L label;
		public LabelledAdjacencyNode(T node, L label, AdjacencyNode<T> next) {
			super(node,next);
			this.label = label;
		}

		public L getLabel() {
			return label;
		}
	}
	
	
}
