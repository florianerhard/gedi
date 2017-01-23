package gedi.util.sequence;

import gedi.util.datastructure.graph.SimpleDirectedGraph;

import java.util.Arrays;
import java.util.Comparator;


public class MismatchGraphBuilder<T extends CharSequence> {

	private T[] reads;
	private int offset;
	private int len;

	/**
	 * All reads must have same length and must be distinct (not checked)!
	 * @param reads
	 */
	public MismatchGraphBuilder(T[] reads) {
		this.reads = reads;
		offset = 0;
		len = reads.length;
	}
	
	public MismatchGraphBuilder(T[] reads, int offset, int len) {
		this.reads = reads;
		this.offset = 0;
		this.len = len;
	}
	
	public SimpleDirectedGraph<T> build() {
		SimpleDirectedGraph<T> g = new SimpleDirectedGraph<T>("MismatchGraph");
		
		int l = reads[0].length();
		for (int i=0; i<l; i++) 
			pass(i,g);
		
		return g;
	}
	
	
	private void pass(int mmpos, SimpleDirectedGraph<T> g) {
		DisregardPosComparator<T> comp = new DisregardPosComparator<T>(mmpos, reads[0].length());
		int t = offset+len;
		
//		System.out.println("sort");
		Arrays.sort(reads, offset, t, comp);
		int blockstart = offset;
		
//		System.out.println("add edges");
		
		for (int i=offset+1; i<t; i++) {
			int c = comp.compare(reads[blockstart], reads[i]);
			if (c!=0) {
				// new block, create clique from blockstart - i-1
				for (int s=blockstart; s<i; s++)
					for (int e=s+1; e<i; e++) {
						g.addInteraction(reads[s], reads[e]);
						g.addInteraction(reads[e], reads[s]);
					}
				blockstart = i;
			}
		}
		
		for (int s=blockstart; s<t; s++)
			for (int e=s+1; e<t; e++) {
				g.addInteraction(reads[s], reads[e]);
				g.addInteraction(reads[e], reads[s]);
			}
		
//		System.out.println(mmpos+" "+(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/(1024*1024));
		
	}
	
	public static class DisregardPosComparator<T extends CharSequence> implements Comparator<T> {
		
		private int pos;
		private int l;
		
		public DisregardPosComparator(int pos, int l) {
			this.pos = pos;
			this.l = l;
		}



		@Override
		public int compare(T o1, T o2) {
	        int k = 0;
	        while (k < l) {
	        	if (k==pos) {
	        		k++;
	        		continue;
	        	}
	            char c1 = o1.charAt(k);
	            char c2 = o2.charAt(k);
	            if (c1 != c2) {
	                return c1 - c2;
	            }
	            k++;
	        }
	        return 0;
		}
		
	}
	
}
