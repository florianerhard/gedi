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

package gedi.util.datastructure.tree.suffixTree.tree;


import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;


public class SuffixTree {

	public static final char SUPER_ROOT_TO_ROOT = '*';
	
	protected char[] alphabet;
	protected CharSequence t;
	protected Map<String,Object> nodeAttributes;

	private SuffixTreeStorage storage;
	
	public SuffixTree(SuffixTreeStorage storage) {
		this.storage = storage;
	}
	
	public char[] getAlphabet() {
		return alphabet;
	}

	public SuffixTreeStorage getStorage() {
		return storage;
	}
	
	public int getNumNodes() {
		return storage.getMaxNode()+1;
	}
	
	

	public CharSequence getText() {
		return t;
	}
	
	public Localization getRoot() {
		return new Localization(this,0,0,0);
	}
	
	public void initialize(char[] alphabet, CharSequence s) {
		this.alphabet = alphabet;
		this.t = s;
		nodeAttributes = new HashMap<String, Object>();
		
		storage.initialize(alphabet,s);
	}
	
	public String[] getAttributeNames() {
		return nodeAttributes.keySet().toArray(new String[0]);
	}
	
	public Class<?> getAttributeComponentType(String type) {
		return nodeAttributes.get(type).getClass().getComponentType();
	}
	
	public void setNodeAttribute(String type, Object array) {
		if (array.getClass().getComponentType()==null)
			throw new RuntimeException("Not an array!");
		nodeAttributes.put(type,array);
	}
	
	public int[] getIntAttributes(String type) {
		return (int[]) nodeAttributes.get(type);
	}
	public boolean[] getBooleanAttributes(String type) {
		return (boolean[]) nodeAttributes.get(type);
	}
	public double[] getDoubleAttributes(String type) {
		return (double[]) nodeAttributes.get(type);
	}
	public Object[] getObjectAttributes(String type) {
		return (Object[]) nodeAttributes.get(type);
	}
	public Object getAttributes(String type) {
		return nodeAttributes.get(type);
	}
	
	
	
	public void finished() {
	}
	
	
	public CharSequence follow(Localization l, char c) {
		return storage.follow(l, c);
	}
	
	public int[] getChildNodes(int node) {
		return storage.getChildNodes(node);
	}
	
	public int getNumChildren(int node) {
		return storage.getNumChildren(node);
	}
	
	public int getParentNode(Localization l) {
		storage.canonize(l);
		return l.getNode();
	}
	
	public int getChildNode(Localization l) {
		storage.canonize(l);
		int infixLength = l.getInfixLength();
		if (infixLength==0)
			return l.getNode();
		char firstChar = t.charAt(l.getInfixStart());
		int save = l.getInfixEnd();
		l.setInfixEnd(l.getInfixStart());
		int re = storage.lookup(l, firstChar);
		l.setInfixEnd(save);
		return re;
	}
	
	public CharSequence getSubSequence(int parent, int child) {
		return storage.getSubSequence(parent, child);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i=-1; i<=storage.getMaxNode(); i++) {
			sb.append(i+":\t");
			for (int c : storage.getChildNodes(i)) {
				sb.append(c+" ("+storage.getSubSequence(i,c)+")\t");
			}
			sb.append("\n");
		}
		return sb.toString();
	}
	
	public String toString(String... indexTypes) {
		StringBuilder sb = new StringBuilder();
		for (int i=-1; i<=storage.getMaxNode(); i++) {
			sb.append(i);
			for (int ti=0; ti<indexTypes.length; ti++) {
				sb.append(ti==0?'\t':',');
				if (i>=0)
					sb.append(Array.get(getAttributes(indexTypes[ti]), i));
			}
			sb.append("\t");
			for (int c : storage.getChildNodes(i)) {
				sb.append(c+" ("+storage.getSubSequence(i,c)+")\t");
			}
			sb.append("\n");
		}
		return sb.toString();
	}
	
	
}
