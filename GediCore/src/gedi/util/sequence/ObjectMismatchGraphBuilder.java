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
package gedi.util.sequence;

import gedi.util.datastructure.graph.SimpleDirectedGraph;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Could be done without comparable by using hashcode and equals and unionfind
 * @author erhard
 *
 * @param <T>
 */
public class ObjectMismatchGraphBuilder<T extends Comparable<T>> {

	private List<T>[] data;
	private int offset;
	private int len;

	/**
	 * All reads must have same length (not checked)!
	 * @param reads
	 */
	public ObjectMismatchGraphBuilder(List<T>[] data) {
		this.data = data;
		offset = 0;
		len = data.length;
	}
	
	public ObjectMismatchGraphBuilder(List<T>[] data, int offset, int len) {
		this.data = data;
		this.offset = 0;
		this.len = len;
	}
	
	public SimpleDirectedGraph<List<T>> build() {
		SimpleDirectedGraph<List<T>> g = new SimpleDirectedGraph<List<T>>("MismatchGraph");
		
		int l = data[0].size();
		for (int i=0; i<l; i++) 
			pass(i,g);
		
		return g;
	}
	
	
	private void pass(int mmpos, SimpleDirectedGraph<List<T>> g) {
		DisregardPosComparator<T> comp = new DisregardPosComparator<T>(mmpos, data[0].size());
		int t = offset+len;
		
//		System.out.println("sort");
		Arrays.sort(data, offset, t, comp);
		int blockstart = offset;
		
//		System.out.println("add edges");
		
		for (int i=offset+1; i<t; i++) {
			int c = comp.compare(data[blockstart], data[i]);
			if (c!=0) {
				// new block, create clique from blockstart - i-1
				for (int s=blockstart; s<i; s++)
					for (int e=s+1; e<i; e++) {
						g.addInteraction(data[s], data[e]);
						g.addInteraction(data[e], data[s]);
					}
				blockstart = i;
			}
		}
		
		for (int s=blockstart; s<t; s++)
			for (int e=s+1; e<t; e++) {
				g.addInteraction(data[s], data[e]);
				g.addInteraction(data[e], data[s]);
			}
		
//		System.out.println(mmpos+" "+(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/(1024*1024));
		
	}
	
	public static class DisregardPosComparator<T extends Comparable<T>> implements Comparator<List<T>> {
		
		private int pos;
		private int l;
		
		public DisregardPosComparator(int pos, int l) {
			this.pos = pos;
			this.l = l;
		}



		@Override
		public int compare(List<T> o1, List<T> o2) {
	        int k = 0;
	        while (k < l) {
	        	if (k==pos) {
	        		k++;
	        		continue;
	        	}
	        	int r = o1.get(k).compareTo(o2.get(k));
	            if (r!=0) {
	                return r;
	            }
	            k++;
	        }
	        return 0;
		}
		
	}
	
}
