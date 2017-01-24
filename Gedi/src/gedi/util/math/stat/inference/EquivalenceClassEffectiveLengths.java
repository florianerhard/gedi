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

import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.StringUtils;
import gedi.util.functions.EI;

import java.util.BitSet;
import java.util.HashMap;
import java.util.TreeMap;

import jdistlib.Normal;

public class EquivalenceClassEffectiveLengths {
	
	private Node[] nodes;


	public EquivalenceClassEffectiveLengths(ReferenceGenomicRegion<?>[] rgr) {
		this(EI.wrap(rgr).map(r->r.getRegion()).toArray(GenomicRegion.class));
	}
	
	public EquivalenceClassEffectiveLengths(GenomicRegion[] regions) {
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
		return effectiveLength(e, eff, -1);
	}
	public double effectiveLength(BitSet e, double[] eff, int checkTotal) {
		double re = 0;
		int total = 0;
		NodeBoolean buff = new NodeBoolean();
		for (int ni=0; ni<nodes.length; ni++) {
			Node n = nodes[ni];
			
			if (isSubset(e,n.equi)) {
				
				// follow path
				int totalLen = 0;
				int neqLen = 0;
				double sub = 0;
				Node last = n;
				for (buff.set(n, false); buff.node!=null; buff.node.next(e,buff)) {
					if (!buff.node.equi.equals(e)) {
						if (buff.equal) {
							sub+=eff[neqLen];
							neqLen = 0;
						}
						neqLen+=buff.node.len;
					}
					else if (neqLen>0) {
						sub+=eff[neqLen];
						neqLen = 0;
					}
					
					totalLen+=buff.node.len;
					last = buff.node;
				}
				if (neqLen>0) {
					sub+=eff[neqLen];
					neqLen = 0;
				}
				
				total += totalLen;
				
				re+=eff[totalLen]-sub;
				ni = last.index;
			}
		}
		
		assert checkTotal==-1 || checkTotal==total;
		
		return re;
	}
	
	public static double[] preprocessEff(double mean, double sd, int maxLen) {
		
		double[] dens = new double[maxLen+1];
		double[] cum = new double[maxLen+1];
		for (int i=1; i<=maxLen; i++) {
			dens[i] = Normal.density(i, mean, sd, false);
			cum[i] = cum[i-1]+dens[i];
		}
		for (int i=1; i<=maxLen; i++) {
			dens[i]/=cum[maxLen];
			cum[i]/=cum[maxLen];
		}
		
		double[] re = new double[maxLen+1];
		for (int i=1; i<=maxLen; i++)
			re[i] = re[i-1] + cum[i-1] + dens[i];
		
		return re;
	}
	
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
		public void next(BitSet e, NodeBoolean re) {
			for (BitSet k : adjacent.keySet())
				if (isSubset(e, k)) {
					re.set(adjacent.get(k),e.equals(k));
					return;
				}
			re.set(null,false);
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
