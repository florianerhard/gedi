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

import gedi.util.ArrayUtils;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.math.stat.RandomNumbers;

import java.util.Arrays;
import java.util.function.Supplier;



/**
 * Allows to create n-nucleotide preserving permutations of a sequence according to the Altschul-Erickson algorithm (Mol Biol Evol 1985)
 * @author erhard
 *
 */
public class SequenceShuffler implements Supplier<String> {
	
	private String ori;
	private char[] s;
	private int k;
	private char sf;
	private int[] alphabet;

	private int[][] el;
	private RandomNumbers rnd = new RandomNumbers();
	
	public SequenceShuffler(String ori, int k) {
		this(new RandomNumbers(),ori,k);
	}
	
	public SequenceShuffler(RandomNumbers random, String ori, int k) {
		this.rnd = random;
		this.ori = ori;
		this.s = ori.toCharArray();
		this.k = k;
		
		if (k>2 || k<1)
			throw new RuntimeException("Not supported!");
		if (k>1)
			createGraphAndLists(s);
	}
	
	private void createGraphAndLists(char[] s) {
		IntArrayList alpha = new IntArrayList(256);
		IntArrayList[] g = new IntArrayList[256];
		for (int i=0; i<s.length-k+1; i++) {
			if (g[s[i]]==null) {
				g[s[i]] = new IntArrayList(s.length/2);
				alpha.add(s[i]);
			}
			g[s[i]].add(i);
		}
		el = new int[g.length][];
		for (int i=0; i<g.length; i++)
			el[i] = g[i]==null?null:g[i].toIntArray();
		sf = s[s.length-1];
		alphabet = alpha.toIntArray();
		Arrays.sort(alphabet);
	}

	public String getOriginal() {
		return ori;
	}
	
	
	@Override
	public String get() {
		if (k==1) {
			char[] s = ori.toCharArray();
			ArrayUtils.shuffleSlice(s, 0, s.length);
			return new String(s);
		}
		
		String re = null;
		while (re==null) {
			// choose last edge
			int[] lastDest = new int[el.length];
			int[] lastDestPos = new int[el.length];
			for (int i=0; i<lastDest.length; i++) 
				if (el[i]!=null && i!=sf)  {
					lastDestPos[i]=rnd.getUnif(0, el[i].length);
					lastDest[i]=s[el[i][lastDestPos[i]]+1];
				}
				
//			System.out.println(ori);
//			System.out.println(StringUtils.getScale(5, ori.length()));
//			for (int v : alphabet)
//				System.out.println(((char)v)+" -> "+((char)lastDest[v]));
			
			// test if edge ordering is eulerian
			// check if there exists a directed path from each vertex to sf in last edge graph
			// =0 -> not discovered, =1 -> discovered but probably not connected, =2 -> connected
			re = testEuler(lastDest, lastDestPos);
		}
		return re;
	}
	
	private String testEuler(int[] lastDest, int[] lastDestPos) {
		int[] connected = new int[el.length];
		connected[sf] = 2;
		for (int i : alphabet) {
			if (el[i]==null) continue;
			
			int v;
			for (v = i; connected[v]==0; v = lastDest[v]) 
				connected[v] = 1;
			
			if (connected[v]==2) 
				for (v = i; connected[v]==1; v = lastDest[v])  
					connected[v] = 2;
			else
				return null;
		}
		
		for (int i=0; i<lastDest.length; i++) 
			if (el[i]!=null)  {
				if (i==sf)
					ArrayUtils.shuffleSlice(el[i],0,el[i].length);
				else {
					ArrayUtils.swap(el[i], lastDestPos[i], el[i].length-1);
					ArrayUtils.shuffleSlice(el[i],0,el[i].length-1);
				}
			}
		

		int[] p = new int[el.length];
		int cp = s[0];
		char[] re = new char[s.length];
		for (int i=0; i<s.length; i++) {
			re[i] = (char) cp;
			p[cp]++;
			if (i+1<s.length)
				cp = s[el[cp][p[cp]-1]+1];
		}
		return new String(re);
	}
	
	
	
}