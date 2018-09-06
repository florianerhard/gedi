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
package gedi.util.math.stat.inference;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;

import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.StringUtils;
import gedi.util.functions.EI;

public class EquivalenceClassEffectiveLengthSingleReference {

	private Node[] nodes;

	
	public EquivalenceClassEffectiveLengthSingleReference(GenomicRegion... regions) {
		// construct transcript graph
		int N = regions.length;
		
		ArrayGenomicRegion union = new ArrayGenomicRegion();
		for (GenomicRegion r : regions)
			union = union.union(r);
		
		
		GenomicRegion[] indu = new GenomicRegion[regions.length];
		TreeMap<Integer,BitSet> transi = new TreeMap<Integer,BitSet>();
		for (int i=0; i<N; i++) {
			indu[i] = union.induce(regions[i]);
			for (int b=0; b<indu[i].getNumBoundaries(); b++)
				transi.computeIfAbsent(indu[i].getBoundary(b),x->new BitSet(N));
		}
		
		for (int i=0; i<N; i++) {
			int ui = i;
			for (int p=0; p<indu[i].getNumParts(); p++) {
				transi.subMap(indu[i].getStart(p), true, indu[i].getEnd(p), false).forEach((pos,set)->set.set(ui));
			}
		}
		
		TreeMap<Integer, Node> nodes = new TreeMap<Integer, Node>();
		for (Integer tr : transi.keySet()) {
			if (tr==transi.lastKey()) break;
			
			Node n = new Node(transi.get(tr), transi.ceilingKey(tr+1)-tr);
			nodes.put(tr, n);
			BitSet found = new BitSet(N);
			for (Integer predPos : transi.subMap(0, true, tr, false).descendingKeySet()) {
				BitSet potentialPredecessor = transi.get(predPos);
				if (potentialPredecessor.intersects(n.equi)) {
					BitSet e = intersection(potentialPredecessor,n.equi);
					e.andNot(found);
					nodes.get(predPos).adjacent.put(e, n);
					found.or(e);
					if (found.cardinality()==n.equi.cardinality())
						break;
				}
			}
		}
			
		this.nodes = nodes.values().toArray(new Node[0]);
		for (int i=0; i<this.nodes.length; i++)
			this.nodes[i].index = i;
	}
	public double effectiveLength(BitSet e, double[] eff) {
		double re = 0;
		for (int ni=0; ni<nodes.length; ni++) {
			Node n = nodes[ni];
			
			if (isSubset(e,n.equi)) {
				// construct path and dyn prog matrices
				ArrayList<Node> path = new ArrayList<>();
				ArrayList<BitSet> next = new ArrayList<>();
				path.add(n);
				for (;n!=null; n=n.nextAndAdd(e,path,next));
				
				int[][] l = new int[path.size()][path.size()];
				BitSet[][] ee = new BitSet[path.size()][path.size()];
				for (int i=0; i<l.length; i++) {
					l[i][i] = path.get(i).len;
					ee[i][i] = path.get(i).equi;
				}
				for (int li=1; li<l.length; li++) {
					for (int i=0; i+li<l.length; i++) {
						int j=i+li;
						l[i][j] = l[i+1][j]+l[i][i];
						ee[i][j] = and(ee[i+1][j],ee[i][j-1]);
						if (li==1) 
							ee[i][j].and(next.get(i));
					}
				}
				if (ee[0][ee[0].length-1].equals(e)) {
					if (l[0][l[0].length-1]>=eff.length)
						throw new RuntimeException("Choose a larger max size for eff (need at least: "+(l[0][l[0].length-1]+1)+", have: "+eff.length+")!");
					double pl = eff[l[0][l[0].length-1]];
					for (int i=0; i<l.length; i++) {
						for (int j=i; j<l.length; j++) {
							if (!ee[i][j].equals(e)) {
								// left bottom is subtracted twice!
								if (i>0 && j+1<ee[i].length && !ee[i-1][j].equals(e) && !ee[i][j+1].equals(e) && ee[i-1][j+1].equals(e))
									pl+=eff[l[i][j]];
								
								if ((i==0 || ee[i-1][j].equals(e)) && (j+1==ee[i].length || ee[i][j+1].equals(e)))
									pl-=eff[l[i][j]];
							}
						}
					}
					re+=pl;
				}
				ni = path.get(path.size()-1).index;
			}
		}
		
		return re;
	}
	
	private BitSet and(BitSet a, BitSet b) {
		BitSet re = new BitSet(a.size());
		re.or(a);
		re.and(b);
		return re;
	}
//	public double effectiveLength(BitSet e, double[] eff, int checkTotal) {
//		double re = 0;
//		int total = 0;
//		NodeBoolean buff = new NodeBoolean();
//		for (int ni=0; ni<nodes.length; ni++) {
//			Node n = nodes[ni];
//			
//			if (isSubset(e,n.equi)) {
//				
//				// construct path and dyn prog matrices
//				int totalLen = 0;
//				int neqLen = 0;
//				double sub = 0;
//				Node last = n;
//				for (buff.set(n, false); buff.node!=null; buff.node.next(e,buff)) {
//					if (!buff.node.equi.equals(e)) {
//						if (buff.equal) {
//							sub+=eff[neqLen];
//							neqLen = 0;
//						}
//						neqLen+=buff.node.len;
//					}
//					else if (neqLen>0) {
//						sub+=eff[neqLen];
//						neqLen = 0;
//					}
//					
//					totalLen+=buff.node.len;
//					last = buff.node;
//				}
//				if (neqLen>0) {
//					sub+=eff[neqLen];
//					neqLen = 0;
//				}
//				
//				total += totalLen;
//				
//				re+=eff[totalLen]-sub;
//				ni = last.index;
//			}
//		}
//		
//		assert checkTotal==-1 || checkTotal==total;
//		
//		return re;
//	}
	
	
	@Override
	public String toString() {
		return StringUtils.toString(nodes);
	}
	
	private static boolean isSubset(BitSet subSet, BitSet superSet) {
		return intersection(superSet, subSet).equals(subSet);
	}

	private static BitSet intersection(BitSet a, BitSet b) {
		BitSet re = (BitSet) a.clone();
		re.and(b);
		return re;
	}
	
	private static class NodeBoolean {
		Node node;
		boolean equal;
		public NodeBoolean set(Node node, boolean equal) {
			this.node = node;
			this.equal = equal;
			return this;
		}
	}


	private static class Node {
		int index;
		BitSet equi;
		int len;
		HashMap<BitSet,Node> adjacent = new HashMap<BitSet, Node>();
		public Node(BitSet equi, int len) {
			this.equi = equi;
			this.len = len;
		}
		public Node nextAndAdd(BitSet e, ArrayList<Node> path, ArrayList<BitSet> next) {
			for (BitSet k : adjacent.keySet())
				if (isSubset(e, k)) {
					path.add(adjacent.get(k));
					next.add(k);
					return adjacent.get(k);
				}
			return null;
		}
		public void next(BitSet e, NodeBoolean re) {
			for (BitSet k : adjacent.keySet())
				if (isSubset(e, k)) {
					re.set(adjacent.get(k),e.equals(k));
					return;
				}
			re.set(null,false);
		}
		public Node next(BitSet e) {
			for (BitSet k : adjacent.keySet())
				if (isSubset(e, k)) {
					return adjacent.get(k);
				} // return both node and bitset and use the bitset in next!
			return null;
		}
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append(equi).append(" (").append(len).append(")");
			for (BitSet e : adjacent.keySet())
				sb.append(" ").append(e).append("->").append(adjacent.get(e).equi);
					
			return sb.toString();
		}
		
	}
	
}
