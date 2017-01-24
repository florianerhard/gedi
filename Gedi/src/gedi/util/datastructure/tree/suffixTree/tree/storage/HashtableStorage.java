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

package gedi.util.datastructure.tree.suffixTree.tree.storage;

import java.util.HashMap;
import java.util.Locale;

import gedi.util.datastructure.charsequence.TransparentSubSequence;
import gedi.util.datastructure.tree.suffixTree.tree.Localization;
import gedi.util.datastructure.tree.suffixTree.tree.SuffixTree;

public class HashtableStorage extends AbstractSuffixTreeStorage {
	


	private HashMap<NodeCharPair, int[]> map = new HashMap<NodeCharPair, int[]>();
	private char[] alphabetArray;
	
	
	public void initialize(char[] alphabet, CharSequence s) {
		super.initialize(alphabet, s);
		map.clear();
		alphabetArray = alphabet;
	}

	private String edgeToString(NodeCharPair k) {
		int[] a = map.get(k);
		return String.format(Locale.US,"[%d,%s]->[%d,%d,%d], ",k.Node,k.FirstChar,a[0],a[1],a[2]);
	}
	
	private String edgeToString(NodeCharPair k, CharSequence text) {
		int[] a = map.get(k);
		return String.format(Locale.US,"[%d,%s]->[%s,%d], ",k.Node,k.FirstChar,text.subSequence(a[0],Math.min(text.length(), a[1])),a[2]);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (NodeCharPair k : map.keySet()) 
			sb.append(edgeToString(k));
		if (sb.length()>0)
			sb.delete(sb.length()-2,sb.length());
		return sb.toString();
	}
	
	public String toString(CharSequence text) {
		StringBuilder sb = new StringBuilder();
		for (NodeCharPair k : map.keySet()) 
			sb.append(edgeToString(k,text));
		if (sb.length()>0)
			sb.delete(sb.length()-2,sb.length());
		return sb.toString();
	}
	
	
	public void canonize(Localization l) {
		int infixLength = l.getInfixLength();
		
		int[] lookup;
		int node = l.getNode();
		int start = l.getInfixStart();
		int end = l.getInfixEnd();
		
		if (node==-1 && infixLength>0) {
			node = 0;
			start++;
		}
		
		while (start<end) {
			lookup = map.get(new NodeCharPair(node,t.charAt(start))); 
			if (lookup==null)
				throw new RuntimeException("Could not canonize "+l);
			int nextStart = start+lookup[1]-lookup[0];
			
			if (lookup[1]==Integer.MAX_VALUE || nextStart>end)
				break;
			
			start = nextStart;
			node = lookup[2];
		}
		
		l.setNode(node);
		l.setInfixStart(start);
	}

	
	public int lookup(Localization l, char c) {
		if (l.getNode()==-1)
			return 0;
		
		int infixLength = l.getInfixLength();
		char firstChar = t.charAt(l.getInfixStart());
		
		int[] lookup;
		if (infixLength==0) {
			lookup = map.get(new NodeCharPair(l.getNode(),c)); 
			if (lookup==null)
				return -1;
		} else { 
			lookup = map.get(new NodeCharPair(l.getNode(),firstChar)); 
			if (lookup==null || t.charAt(lookup[0]+infixLength)!=c)
				return -1;
		}
		return lookup[2];
	}
	
	public CharSequence follow(Localization l, char c) {
		if (l.getNode()==-1) {
			l.setInfixStart(0);
			l.setInfixEnd(0);
			l.setNode(0);
			return Character.toString(c);
		}
		
		int infixLength = l.getInfixLength();
		
		int[] lookup;
		if (infixLength==0) {
			lookup = map.get(new NodeCharPair(l.getNode(),c));
			if (lookup==null)
				return null;
		} else { 
			char firstChar = t.charAt(l.getInfixStart());
			lookup = map.get(new NodeCharPair(l.getNode(),firstChar));
			if (lookup==null || t.charAt(lookup[0]+infixLength)!=c)
				return null;
		}
		
		CharSequence re = new TransparentSubSequence(t,lookup[0]+l.getInfixLength(), Math.min(t.length(), lookup[1]));
		l.setInfixStart(lookup[0]);
		l.setInfixEnd(lookup[0]+re.length());
		return re;
	}
	
	public int[] getChildNodes(int node) {
		if (node==-1)
			return new int[] {0};
		
		int[] re = new int[alphabetArray.length];
		int index = 0;
		for (char c : alphabetArray) {
			int[] lookup = map.get(new NodeCharPair(node,c));
			if (lookup!=null)
				re[index++] = lookup[2];
		}
		int[] re2 = new int[index];
		System.arraycopy(re, 0, re2, 0, re2.length);
		return re2;
	}
	
	public int getNumChildren(int node) {
		if (node==-1)
			return 0;
		
		int re = 0;
		for (char c : alphabetArray) {
			int[] lookup = map.get(new NodeCharPair(node,c));
			if (lookup!=null)
				re++;
		}
		return re;
	}
	
	public int getIndex(int node) {
		if (node==-1)
			return 0;
		
		int re = t.length();
		for (char c : alphabetArray) {
			int[] lookup = map.get(new NodeCharPair(node,c));
			if (lookup!=null)
				re = Math.min(re, lookup[0]);
		}
		return re;
	}
	
	@Override
	public CharSequence getSubSequence(int parent, int child) {
		if (parent==-1 && child==0)
			return Character.toString(SuffixTree.SUPER_ROOT_TO_ROOT);
		
		for (char c : alphabetArray) {
			int[] lookup = map.get(new NodeCharPair(parent,c));
			if (lookup!=null && lookup[2]==child)
				return new TransparentSubSequence(t,lookup[0], Math.min(t.length(), lookup[1]));
		}
		
		return null;
	}
	
	
	
	@Override
	public void createSuperRootAndRoot() {
		map.put(new NodeCharPair(-1,SuffixTree.SUPER_ROOT_TO_ROOT), new int[] {0,0,0});
		nextIndex = 1;
	}
	

	@Override
	public int addLeaf(int parent, int start) {
		int re = nextIndex++;
		map.put(new NodeCharPair(parent,t.charAt(start)), new int[] {start,Integer.MAX_VALUE,re});
		return re;
	}

	

	@Override
	public int split(Localization l) {
		char firstChar = t.charAt(l.getInfixStart());
		int infixLength = l.getInfixLength();
		int[] removed = map.remove(new NodeCharPair(l.getNode(),firstChar));
		map.put(new NodeCharPair(l.getNode(), firstChar), new int[] {removed[0], removed[0]+infixLength, nextIndex++});
		map.put(new NodeCharPair(nextIndex-1, t.charAt(removed[0]+infixLength)), new int[] {removed[0]+infixLength, removed[1], removed[2]});
//		l.setNode(nextIndex-1);
//		l.setInfixStart(l.getInfixEnd());
		return nextIndex-1;
	}



}
