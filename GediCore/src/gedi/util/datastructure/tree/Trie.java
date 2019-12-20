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
package gedi.util.datastructure.tree;


import gedi.util.StringUtils;
import gedi.util.datastructure.charsequence.CharDag;
import gedi.util.datastructure.charsequence.CharDag.CharDagVisitor;
import gedi.util.datastructure.charsequence.CharDag.PosLengthVariant;
import gedi.util.datastructure.charsequence.CharDag.SequenceVariant;
import gedi.util.datastructure.charsequence.CharIterator;
import gedi.util.datastructure.charsequence.CharRingBuffer;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.datastructure.tree.BinaryTrie.AhoCorasickResult;
import gedi.util.datastructure.tree.BinaryTrie.Node;
import gedi.util.datastructure.tree.redblacktree.Interval;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.userInteraction.progress.Progress;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;


public class Trie<T> implements Map<String,T> {

	
	public static final class NodeList {
		NodeList next;
		Node node;
		public NodeList(NodeList last, Node node) {
			this.next = last;
			this.node = node;
		}
	}
	
	/**
	 * Stores the first child and its next sibling. Stores also the character to its parent
	 * 
	 * @author erhard
	 *
	 */
	public static final class Node {
		Node child;
		Node sibling;
		char c;
		Object value;
		
		Node suffixLink;
		int nodeLevel;
		Node additionalResult;
		
		public Node(int nodeLevel,Node sibling, char c) {
			this.nodeLevel = nodeLevel;
			this.sibling = sibling;
			this.c = c;
		}
		
		public void clear() {
			child = null;
		}

		public boolean isEmpty() {
			return child==null;
		}

		public Node addChild(char c) {
			child = new Node(nodeLevel+1,child,c);
			return child;
		}
		
		public Node findChild(char c) {
			for (Node ch=child; ch!=null; ch=ch.sibling)
				if (ch.c==c)
					return ch;
			return null;
		}
		
		public Node uniqueChild() {
			if (child.sibling==null)
				return child;
			return null;
		}
		
	}
	
	
	private Node root = new Node(0,null,'\0');
	private int size = 0;
	private Object nullValue = new Object();
	
	@Override
	public T put(String key, T value) {
		ahoCorasickDirty = true;
		Node n = root;
		int i = 0;
		for (Node t = n; t!=null && i<key.length(); ) {
			t = n.findChild(key.charAt(i));
			if (t!=null) {
				n = t;
				i++;
			}
		}
		if (i<key.length())
			size++;
		for (; i<key.length(); i++) 
			n = n.addChild(key.charAt(i));
		
		n.value = value==null?nullValue:value;
		return value;
	}
	
	public int getNodeCount() {
		Stack<Node> dfs = new Stack<>();
		dfs.push(root);
		int re = 0;
		while (!dfs.isEmpty()) {
			Node n = dfs.pop();
			re++;
			for (Node ch=n.child; ch!=null; ch=ch.sibling)
				dfs.push(ch);
		}
		return re;
	}
	
	@Override
	public int size() {
		return size;
	}
	
	@Override
	public void clear() {
		ahoCorasickDirty = true;
		root.clear();
		size=0;
	}

	@Override
	public boolean containsKey(Object key) {
		if (key instanceof String) {
			String s = (String) key;
			Node n = root;
			int i = 0;
			for (Node t = n; t!=null && i<s.length(); ) {
				t = n.findChild(s.charAt(i));
				if (t!=null) {
					n = t;
					i++;
				}
			}
			return i==s.length()&&n.value!=null;
		}
		return false;
	}


	/**
	 * Inplace
	 * @return
	 */
	public Trie<T> sort() {
		Stack<Node> dfs = new Stack<Node>(); 
		dfs.push(root);
		ArrayList<Node> sorter = new ArrayList<Node>();
		while (!dfs.isEmpty()) {
			Node n = dfs.pop();
			if (n.child!=null && n.child.sibling!=null) {
				for (Node nn=n.child; nn!=null; nn=nn.sibling)
					sorter.add(nn);
				Collections.sort(sorter, (a,b)->Character.compare(a.c, b.c));
				n.child = sorter.get(0);
				for (int i=1; i<sorter.size(); i++) 
					sorter.get(i-1).sibling = sorter.get(i);
				sorter.get(sorter.size()-1).sibling=null;
				sorter.clear();
			}
			if (n.child!=null)
				for (Node nn=n.child; nn!=null; nn=nn.sibling)
					dfs.push(nn);
				
		}
		return this;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public T get(Object key) {
		if (key instanceof CharSequence) {
			CharSequence s = (CharSequence) key;
			Node n = root;
			int i = 0;
			for (Node t = n; t!=null && i<s.length(); ) {
				t = n.findChild(s.charAt(i));
				if (t!=null) {
					n = t;
					i++;
				}
			}
			if (i==s.length())
				return n.value==nullValue?null:(T) n.value;
			return null;
		}
		if (key instanceof CharIterator) {
			CharIterator s = (CharIterator) key;
			Node n = root;
			for (Node t = n; t!=null && s.hasNext(); ) {
				t = n.findChild(s.nextChar());
				if (t!=null) {
					n = t;
				}
			}
			if (!s.hasNext())
				return n.value==nullValue?null:(T) n.value;
			return null;
		}	
		return null;
	}
	
	public HashSet<T> getByPrefix(String key) {
		return getByPrefix(key, new HashSet<T>());
	}
	
	public <C extends Collection<? super T>> C getByPrefix(String key, C re) {
		String s = (String) key;
		Node n = root;
		int i = 0;
		for (Node t = n; t!=null && i<s.length(); ) {
			t = n.findChild(s.charAt(i));
			if (t!=null) {
				n = t;
				i++;
			}
		}
		if (i<s.length())
			return re;
		
		ValueIterator it = new ValueIterator(key,n);
		while (it.hasNext())
			re.add(it.next());
		return re;
	}
	
	
	public HashSet<String> getKeysByPrefix(String key) {
		return getKeysByPrefix(key, new HashSet<String>());
	}
	
	public <C extends Collection<? super String>> C getKeysByPrefix(String key, C re) {
		String s = (String) key;
		Node n = root;
		int i = 0;
		for (Node t = n; t!=null && i<s.length(); ) {
			t = n.findChild(s.charAt(i));
			if (t!=null) {
				n = t;
				i++;
			}
		}
		if (i<s.length())
			return re;
		
		if (n!=null && n.value!=null)
			re.add(key);
		
		if (n!=null && n.child!=null) {
			KeyIterator it = new KeyIterator(key,n);
			while (it.hasNext())
				re.add(it.next());
		}
		return re;
	}
	
	
	/**
	 * Returns all prefixes of s, that are in this trie.
	 * @param s
	 * @return
	 */
	public ExtendedIterator<T> iteratePrefixMatches(CharSequence s) {
		return new ExtendedIterator<T>() {
			
			int i=0;
			Node n = root;
			T next;
			
			@Override
			public boolean hasNext() {
				findNext();
				return next!=null;
			}

			@Override
			public T next() {
				findNext();
				T re = next;
				next = null;
				return re;
			}

			private void findNext() {
				if (next==null) {
					for (; n!=null && i<s.length(); ) {
						n = n.findChild(s.charAt(i));
						if (n!=null) {
							i++;
							if (n.value!=null) {
								next = (T) n.value;
								return;
							}
						}
					}
				}
			}
			
		};
	}
	
	public T getUniqueWithPrefix(String s) {
		Node n = root;
		int i = 0;
		for (Node t = n; t!=null && i<s.length(); ) {
			t = n.findChild(s.charAt(i));
			if (t!=null) {
				n = t;
				i++;
			}
		}
		for (; n!=null && n.value==null; n = n.uniqueChild());
		if (n==null || n.child!=null) return null;
		return n.value==nullValue?null:(T) n.value;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public T remove(Object key) {
		if (key instanceof String) {
			String s = (String) key;
			Node n = root;
			NodeList list = new NodeList(null,n);
			
			int i = 0;
			for (Node t = n; t!=null && i<s.length(); ) {
				t = n.findChild(s.charAt(i));
				if (t!=null) {
					n = t;
					i++;
					list = new NodeList(list,t);
				}
			}
			
			if (i==s.length()) {
				T re = (T) n.value;
				n.value = null;
				
				while (list.next!=null && list.node.value==null && list.node.child==null) {
					list.next.node.child = list.node.sibling;
					list = list.next;
				}
				
				size--;
				ahoCorasickDirty = true;
				return re;
			}
				
			return null;
		}
		return null;
	}
	
	@Override
	public boolean isEmpty() {
		return root.isEmpty();
	}
	
	@Override
	public void putAll(Map<? extends String, ? extends T> m) {
		for (Entry<? extends String, ? extends T> e : m.entrySet())
			put(e.getKey(),e.getValue());
	}
	
	@Override
	public boolean containsValue(Object value) {
		return values().contains(value);
	}

	@Override
	public Set<java.util.Map.Entry<String, T>> entrySet() {
		return new TrieEntries();
	}

	@Override
	public Set<String> keySet() {
		return new TrieKeys();
	}
	

	@Override
	public Collection<T> values() {
		return new TrieValues();
	}
	
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		for (Entry<String,T> e : entrySet())
			sb.append(e.getKey()+":"+e.getValue()+",");
		if (size>0)sb.deleteCharAt(sb.length()-1);
		sb.append("}");
		return sb.toString();
	}

	
	private class TrieKeys extends AbstractSet<String> {

		@Override
		public Iterator<String> iterator() {
			return new KeyIterator("",root);
		}

		@Override
		public int size() {
			return Trie.this.size;
		}

		@Override
		public void clear() {
			Trie.this.clear();
		}

		@Override
		public boolean contains(Object o) {
			return Trie.this.containsKey(o);
		}

		@Override
		public boolean isEmpty() {
			return Trie.this.isEmpty();
		}

		@Override
		public boolean remove(Object o) {
			return Trie.this.remove(o)!=null;
		}
	}
	
	private class TrieEntries extends AbstractSet<Entry<String,T>> {

		@Override
		public Iterator<Entry<String,T>> iterator() {
			return new EntryIterator("",root);
		}

		@Override
		public int size() {
			return Trie.this.size;
		}

		@Override
		public void clear() {
			Trie.this.clear();
		}

		@Override
		public boolean isEmpty() {
			return Trie.this.isEmpty();
		}
	}
	
	private class TrieValues extends AbstractSet<T> {

		@Override
		public Iterator<T> iterator() {
			return new ValueIterator("",root);
		}

		@Override
		public int size() {
			return Trie.this.size;
		}

		@Override
		public void clear() {
			Trie.this.clear();
		}

		@Override
		public boolean isEmpty() {
			return Trie.this.isEmpty();
		}
	}
	
	
	private abstract class BaseIterator<T> implements Iterator<T> {

		protected StringBuilder sb = new StringBuilder();
		protected NodeList list;
		protected T cur;
		
		// list contains a stack of nodes, starting from a child of the start node; the top entry always is a node with a value
		// order is depth first, return a value once encountered (i.e. short prefixes first).
		// sb always contains the start prefix + the string corresponding to the list
		
		public BaseIterator(String prefix, Node start) {
			sb.append(prefix);
			if (start.value!=null) 
				cur = makeCur();
			
			findNextValue(start);
		}
		
		protected abstract T makeCur();
		

		// Starting from node (excluding it!), walk down the first children until a values is encountered
		// if so, appends the nodes to the list and the characters to sb
		// this will always succeed, if the node has a child (as all leaves have values)
		private boolean findNextValue(Node node) {
			if (node.child==null) return false;
			
			Node n;
			for (n = node.child; n!=null && n.value==null; n=n.child) {
				sb.append(n.c);
				list = new NodeList(list, n);
			}
			if (n==null) throw new RuntimeException("Should not happen, as leaves should have values!");
			sb.append(n.c);
			list = new NodeList(list, n);
			return true;
		}

		
		private boolean popAndNextSibling() {
			while (list.node.sibling==null) {
				list = list.next;
				sb.deleteCharAt(sb.length()-1);
				if (list==null) return false;
			} 
			
			
			list = new NodeList(list.next, list.node.sibling);
			sb.deleteCharAt(sb.length()-1);
			sb.append(list.node.c);
			return true;
		}
		

		@Override
		public boolean hasNext() {
			lookAhead();
			return cur!=null;
		}

		@Override
		public T next() {
			lookAhead();
			T re = cur;
			cur=null;
			return re;
		}
		
		private void lookAhead() {
			if (cur==null) {
				if (list==null) return;
				cur = makeCur();
				
				// find next:
				while (true) {
					if (findNextValue(list.node)) return;
					if (!popAndNextSibling()) return;
					if (list!=null && list.node.value!=null)
						// direct child of root has value
						return;
				}
			}
		}
	
	}
	
	
	private class KeyIterator extends BaseIterator<String> {
		
		public KeyIterator(String prefix, Node start) {
			super(prefix, start);
		}
		@Override
		protected String makeCur() {
			return sb.toString();
		}
		
	}
	
	private class EntryIterator implements Iterator<Entry<String,T>> {

		private StringBuilder sb = new StringBuilder();
		private NodeList list;
		private Entry<String,T> cur;
		private String toRemove;
		
		public EntryIterator(String prefix, Node start) {
			sb.append(prefix);
			if (start.value!=null) 
				cur = new AbstractMap.SimpleEntry<String, T>(sb.toString(),start.value==nullValue?null:(T) start.value);
			list = new NodeList(null,start.child);
			if (start.child!=null) // happens only if trie is empty
				sb.append(start.child.c);
		}
		
		@Override
		public boolean hasNext() {
			lookAhead();
			return cur!=null;
		}

		@Override
		public Entry<String,T> next() {
			lookAhead();
			Entry<String,T> re = cur;
			toRemove = cur.getKey();
			cur=null;
			return re;
		}
		
		@SuppressWarnings("unchecked")
		private void lookAhead() {
			if (cur==null) {
				if (list.node==null)return; // happens only if trie is empty
				do {
					if (list.node.child!=null) {
						list = new NodeList(list,list.node.child);
					} else if (list.node.sibling!=null) {
						list.node = list.node.sibling;
						sb.deleteCharAt(sb.length()-1);
					} else {
						int toDelete=2;
						for (list = list.next; list!=null && list.node.sibling==null; list = list.next)
							toDelete++;
						if (list==null)
							return;
						sb.delete(sb.length()-toDelete,sb.length());
						list.node = list.node.sibling;
					}
					sb.append(list.node.c);
				} while (list!=null && list.node.value==null);
				cur = new AbstractMap.SimpleEntry<String, T>(sb.toString(),list.node.value==nullValue?null:(T)list.node.value);
			}
		}

		@Override
		public void remove() {
			Trie.this.remove(toRemove);
		}
		
	}
	
	private class ValueIterator implements Iterator<T> {

		private StringBuilder sb = new StringBuilder();
		private NodeList list;
		private String curKey;
		private T cur;
		private String toRemove;
		
		public ValueIterator(String prefix, Node start) {
			sb.append(prefix);
			if (start.value!=null) 
				cur = start.value==nullValue?null:(T) start.value;
			list = new NodeList(null,start.child);
			if (start.child!=null)
				sb.append(start.child.c);
		}
		
		@Override
		public boolean hasNext() {
			lookAhead();
			return cur!=null;
		}

		@Override
		public T next() {
			lookAhead();
			T re = cur;
			toRemove = curKey;
			cur=null;
			return re;
		}
		
		@SuppressWarnings("unchecked")
		private void lookAhead() {
			if (cur==null) {
				if (list.node==null)return; // happens only if trie is empty
				do {
					if (list.node.child!=null) {
						list = new NodeList(list,list.node.child);
					} else if (list.node.sibling!=null) {
						list.node = list.node.sibling;
						sb.deleteCharAt(sb.length()-1);
					} else {
						int toDelete=2;
						for (list = list.next; list!=null && list.node.sibling==null; list = list.next)
							toDelete++;
						if (list==null)
							return;
						sb.delete(sb.length()-toDelete,sb.length());
						list.node = list.node.sibling;
					}
					sb.append(list.node.c);
				} while (list!=null && list.node.value==null);
				curKey = sb.toString();
				cur = list.node.value==nullValue?null:(T) list.node.value;
			}
		}

		@Override
		public void remove() {
			Trie.this.remove(toRemove);
		}
		
	}
	
	// Aho Corasick stuff
	private boolean ahoCorasickDirty = false;
	
	public synchronized void prepareAhoCorasick() {
		prepareAhoCorasick(null);
	}
	public synchronized void prepareAhoCorasick(Progress progress) {
		if (!ahoCorasickDirty) return;
		
		if (progress!=null) 
			progress.init().setCount(getNodeCount()).setDescription("Preparing Aho-Corasick");
		
		
		LinkedList<Node> q  = new LinkedList<Node>();
		root.nodeLevel = 0;
		root.suffixLink = root;
		if (progress!=null) 
			progress.incrementProgress();
		
		for (Node ch=root.child; ch!=null; ch=ch.sibling) {
			ch.suffixLink = root;
			q.add(ch);
			if (progress!=null) 
				progress.incrementProgress();
		}
		while (!q.isEmpty()) {
			Node r = q.removeFirst();
			for (Node ch=r.child; ch!=null; ch=ch.sibling) {
				q.add(ch);
				Node v = r.suffixLink;
				while (acGoto(v, ch.c)==null)
					v = v.suffixLink;
				ch.suffixLink = acGoto(v, ch.c);
				
				for (Node testSl = ch.suffixLink; testSl!=root; testSl = testSl.suffixLink)
					if (testSl.value!=null) {
						ch.additionalResult = testSl;
						break;
					}
				if (progress!=null) 
					progress.incrementProgress();
			}	
		}
		
		if (progress!=null) 
			progress.finish();
		
		ahoCorasickDirty = false;
		
	}
	
	private Node acGoto(Node q, char a) {
		Node re = q.findChild(a);
		if (re==null && q==root) return root;
		return re;
	}
	
	
	public <C extends Collection<AhoCorasickResult<T>>> C ahoCorasick(CharSequence text, C re) {
		Iterator<AhoCorasickResult<T>> it = iterateAhoCorasick(text, false);
		while (it.hasNext())
			re.add(it.next());
		return re;
	}
	
	public ArrayList<AhoCorasickResult<T>> ahoCorasick(CharSequence text) {
		return ahoCorasick(text, new ArrayList<AhoCorasickResult<T>>());
	}
	
	public ExtendedIterator<AhoCorasickResult<T>> iterateAhoCorasick(final CharSequence text) {
		return iterateAhoCorasick(text, false);
	}
	public ExtendedIterator<AhoCorasickResult<T>> iterateAhoCorasick(final CharSequence text, final boolean reUse) {
		if (text==null) return EI.empty();
		
		prepareAhoCorasick();
		final AhoCorasickResult<T> one = reUse?new AhoCorasickResult<T>():null;
		return new ExtendedIterator<AhoCorasickResult<T>>() {
			Node q = root;
			int index = 0;
			LinkedList<Node> values = new LinkedList<Node>(); 

			@Override
			public boolean hasNext() {
				lookAhead();
				return !values.isEmpty();
			}

			@Override
			public AhoCorasickResult<T> next() {
				lookAhead();
				Node n = values.removeFirst();
				AhoCorasickResult<T> re = reUse?one:new AhoCorasickResult<T>();
				re.end = index;
				re.start = index-n.nodeLevel;
				re.object = (T)n.value;
				re.key = text.subSequence(re.start, re.end);
				return re;
			}
			
			private void lookAhead() {
				for (; values.size()==0 && index<text.length(); index++) {
					char c = text.charAt(index);
					Node nq;
					while ((nq=acGoto(q, c))==null)
						q = q.suffixLink;
					q = nq;
					if (nq.value!=null)
						values.add(nq);
					for (nq = nq.additionalResult; nq!=null; nq = nq.additionalResult)
						values.add(nq);
				}
			}
			
		};
	}
	
	
	
	

	public <C extends Collection<AhoCorasickResult<T>>> C ahoCorasick(CharIterator text, C re) {
		Iterator<AhoCorasickResult<T>> it = iterateAhoCorasick(text, false);
		while (it.hasNext())
			re.add(it.next());
		return re;
	}
	
	public ArrayList<AhoCorasickResult<T>> ahoCorasick(CharIterator text) {
		return ahoCorasick(text, new ArrayList<AhoCorasickResult<T>>());
	}
	
	public ExtendedIterator<AhoCorasickResult<T>> iterateAhoCorasick(final CharIterator text) {
		return iterateAhoCorasick(text, false);
	}
	public ExtendedIterator<AhoCorasickResult<T>> iterateAhoCorasick(final CharIterator text, final boolean reUse) {
		if (text==null) return EI.empty();
		
		prepareAhoCorasick();
		final AhoCorasickResult<T> one = reUse?new AhoCorasickResult<T>():null;
		return new ExtendedIterator<AhoCorasickResult<T>>() {
			Node q = root;
			int index = 0;
			LinkedList<Node> values = new LinkedList<Node>(); 
			CharRingBuffer buff = new CharRingBuffer(1024);

			@Override
			public boolean hasNext() {
				lookAhead();
				return !values.isEmpty();
			}

			@Override
			public AhoCorasickResult<T> next() {
				lookAhead();
				Node n = values.removeFirst();
				AhoCorasickResult<T> re = reUse?one:new AhoCorasickResult<T>();
				re.end = index;
				re.start = index-n.nodeLevel;
				re.object = (T)n.value;
				re.key = buff.getLast(n.nodeLevel);
				return re;
			}
			
			private void lookAhead() {
				for (; values.size()==0 && text.hasNext(); index++) {
					char c = text.nextChar();
					if (buff.capacity()<q.nodeLevel)
						buff.resize(buff.capacity()*2);
					buff.add(c);
					Node nq;
					while ((nq=acGoto(q, c))==null)
						q = q.suffixLink;
					q = nq;
					if (nq.value!=null)
						values.add(nq);
					for (nq = nq.additionalResult; nq!=null; nq = nq.additionalResult)
						values.add(nq);
				}
			}
			
		};
	}
	
	
	

//	public <C extends Collection<AhoCorasickResult<T>>> C ahoCorasick(AlternativesCharIterator text, C re) {
//		Iterator<AhoCorasickResult<T>> it = iterateAhoCorasick(text, false);
//		while (it.hasNext())
//			re.add(it.next());
//		return re;
//	}
//	
//	public ArrayList<AhoCorasickResult<T>> ahoCorasick(AlternativesCharIterator text) {
//		return ahoCorasick(text, new ArrayList<AhoCorasickResult<T>>());
//	}
//	
//	public ExtendedIterator<AhoCorasickResult<T>> iterateAhoCorasick(final AlternativesCharIterator text) {
//		return iterateAhoCorasick(text, false);
//	}
//	public ExtendedIterator<AhoCorasickResult<T>> iterateAhoCorasick(final AlternativesCharIterator text, final boolean reUse) {
//		if (text==null) return EI.empty();
//		
//		prepareAhoCorasick();
//		final AhoCorasickResult<T> one = reUse?new AhoCorasickResult<T>():null;
//		return new ExtendedIterator<AhoCorasickResult<T>>() {
//			ArrayList<NodeCharRingBuffer> ql = new ArrayList<>(Arrays.asList(new NodeCharRingBuffer(1024,root)));
//			HashSet<NodeCharRingBuffer> qs = new HashSet<>();
//			int index = 0;
//			LinkedList<AhoCorasickResult<T>> values = new LinkedList<AhoCorasickResult<T>>(); 
//
//			@Override
//			public boolean hasNext() {
//				lookAhead();
//				return !values.isEmpty();
//			}
//
//			@Override
//			public AhoCorasickResult<T> next() {
//				lookAhead();
//				return  values.removeFirst();
//			}
//			
//			private AhoCorasickResult<T> createRes(NodeCharRingBuffer n) {
//				AhoCorasickResult<T> re = reUse?one:new AhoCorasickResult<T>();
//				re.end = index+1;
//				re.start = n.correct(index+1-n.n.nodeLevel);
//				re.object = (T)n.n.value;
//				re.key = n.getLast(n.n.nodeLevel);
//				return re;
//			}
//			
//			private void lookAhead() {
//				for (int n; values.size()==0 && (n=text.next())>0; index++) {
//					for (int qi=0; qi<ql.size(); qi++) {
//						NodeCharRingBuffer q = ql.get(qi);
//						for (int ni=n-1; ni>=0; ni--) 
//							qs.add(advance(ni==0?q:q.clone(),ni));
//					}
//					ql.clear();
//					ql.addAll(qs);
//					qs.clear();
//				}
//			}
//			
//
//			private NodeCharRingBuffer advance(NodeCharRingBuffer q, int ni) {
//				char[] cl = text.fill(ni);
//				if (cl.length!=1)
//					q.shift(index,cl.length-1);
//				
//				for (int ci=0; ci<cl.length; ci++) {
//					char c = cl[ci];
//					if (q.capacity()<q.n.nodeLevel)
//						q.resize(q.capacity()*2);
//					q.add(c);
//					Node nq;
//					while ((nq=acGoto(q.n, c))==null)
//						q.n = q.n.suffixLink;
//					Node re = nq;
//					if (nq.value!=null)
//						values.add(createRes(q.set(nq)));
//					for (nq = nq.additionalResult; nq!=null; nq = nq.additionalResult)
//						values.add(createRes(q.set(nq)));
//					q.set(re);
//				}
//				q.prune(index-q.n.nodeLevel);
//				return q;
//			}
//			
//		};
//	}
	
	public CharDagVisitor<AhoCorasickResult<T>> ahoCorasickVisitor() {
		return ahoCorasickVisitor(null);
	}
	
	public CharDagVisitor<AhoCorasickResult<T>> ahoCorasickVisitor(IntUnaryOperator lengthCorrection) {
		prepareAhoCorasick();
		return new AhoCorasickCharDagVisitor(lengthCorrection);
	}
	
	private class AhoCorasickCharDagVisitor implements CharDagVisitor<AhoCorasickResult<T>> {
		private NodeCharRingBuffer q;
		private IntUnaryOperator lengthCorrection;
		
		public AhoCorasickCharDagVisitor(IntUnaryOperator lengthCorrection) {
			q = new NodeCharRingBuffer(1024,root);
			this.lengthCorrection = lengthCorrection==null?i->i:lengthCorrection;
		}

		public AhoCorasickCharDagVisitor(AhoCorasickCharDagVisitor p,IntUnaryOperator lengthCorrection) {
			this.lengthCorrection = lengthCorrection;
			this.q = p.q.clone();
		}

		
		@Override
		public String toString() {
			return q.getLast(q.n.nodeLevel).toString()+" s"+StringUtils.toString(q.correction);
		}

		@Override
		public void shift(int pos, int shift, SequenceVariant variant) {
			q.shift(pos, shift,variant);
		}
		
		@Override
		public void prune(int pos) {
			q.prune(pos-lengthCorrection.applyAsInt(q.n.nodeLevel));
		}

		@Override
		public int compareTo(CharDagVisitor<AhoCorasickResult<T>> obj) {
			AhoCorasickCharDagVisitor o = (AhoCorasickCharDagVisitor)obj;
			int re = Integer.compare(System.identityHashCode(this.q.n), System.identityHashCode(o.q.n));
			if (re==0) {
				int len1 = this.q.correction.size();
		        int len2 = o.q.correction.size();
		        Iterator<PosLengthVariant> tid = this.q.correction.iterator();
		        Iterator<PosLengthVariant> oit = o.q.correction.iterator();
		        while (tid.hasNext() & oit.hasNext()) {
		            PosLengthVariant c1 = tid.next();
		            PosLengthVariant c2 = oit.next();
		            re = c1.compareTo(c2);
		            if (re!=0) {
		                return re;
		            }
		        }
		        re = len1 - len2;
			}
			return re;
		}
		
		private AhoCorasickResult<T> createRes(NodeCharRingBuffer n, int pos) {
			AhoCorasickResult<T> re = new AhoCorasickResult<T>();
			re.end = pos+1;
			int s = q.correct(pos+1-lengthCorrection.applyAsInt(n.n.nodeLevel));
			int l = re.end-s;
//			l = lengthCorrection.applyAsInt(l);
			re.start = re.end-l;
			re.object = (T)n.n.value;
			re.key = n.getLast(n.n.nodeLevel);
			return re;
		}
		

		@Override
		public Iterator<AhoCorasickResult<T>> accept(char c, int pos) {
			if (q.capacity()<q.n.nodeLevel)
				q.resize(q.capacity()*2);
			q.add(c);
			Node nq;
			while ((nq=acGoto(q.n, c))==null)
				q.n = q.n.suffixLink;
			Node re = nq;
			ArrayList<AhoCorasickResult<T>> values = new ArrayList<AhoCorasickResult<T>>(); 
			if (nq.value!=null)
				values.add(createRes(q.set(nq),pos));
			for (nq = nq.additionalResult; nq!=null; nq = nq.additionalResult)
				values.add(createRes(q.set(nq),pos));
			q.set(re);
			
			return EI.wrap(values);
		}

		@Override
		public CharDagVisitor<AhoCorasickResult<T>> branch() {
			AhoCorasickCharDagVisitor re = new AhoCorasickCharDagVisitor(this, lengthCorrection);
			return re;
		}
		
		@Override
		public Iterator<SequenceVariant> getVariants() {
			return EI.wrap(q.correction).map(plv->plv.getVariant());
		}
		
	}
	
	static class NodeCharRingBuffer extends CharRingBuffer {
		private Node n;
		private LinkedList<CharDag.PosLengthVariant> correction = new LinkedList<CharDag.PosLengthVariant>();
		
		public NodeCharRingBuffer(int len, Node n) {
			super(len);
			this.n = n;
		}
		
		public void shift(int pos, int shift,SequenceVariant variant) {
			if (correction.size()>0 && correction.getLast().getPos()==pos) {
				correction.getLast().incrementLen(shift);
			} else
				correction.add(new CharDag.PosLengthVariant(pos,shift,variant));
		}
		
		public void prune(int maxStart) {
			Iterator<CharDag.PosLengthVariant> it = correction.iterator();
			while (it.hasNext() && it.next().getPos()<=maxStart) {
				it.remove();
			}
		}
		
		public int correct(int start) {
			int re = start;
			Iterator<CharDag.PosLengthVariant> it = correction.descendingIterator();
			while (it.hasNext()) {
				CharDag.PosLengthVariant n = it.next();
				if (n.getPos()>=start)
					re+=n.getLen();
			}
			return re;
		}

		public NodeCharRingBuffer set(Node n) {
			this.n = n;
			return this;
		}
		
		public NodeCharRingBuffer clone() {
			NodeCharRingBuffer re = new NodeCharRingBuffer(b.length,n);
			System.arraycopy(b, 0, re.b, 0, b.length);
			re.next = next;
			re.n = n;
			re.correction.addAll(correction);
			return re;
		}
		
		@Override
		public int hashCode() {
			return n.hashCode();
		}
		
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof NodeCharRingBuffer))
				return false;
			NodeCharRingBuffer o = (NodeCharRingBuffer)obj;
			if(o.n!=n) 
				return false;
			return o.getLast(n.nodeLevel).equals(getLast(n.nodeLevel));
		}
		
	}
	
	public static class AhoCorasickResult<T> implements Interval {
		CharSequence key;
		int start;
		int end;
		T object;
		AhoCorasickResult(){}
		AhoCorasickResult(AhoCorasickResult<T> ori) {
			this.start = ori.start;
			this.end =ori.end;
			this.object = ori.object;
			this.key = ori.key;
		}
		
		public int getEnd() {
			return end;
		}
		public CharSequence getKey() {
			return key;
		}
		public T getValue() {
			return object;
		}
		@Override
		public String toString() {
			return key+"->"+object+"@"+start+"-"+end;
		}
		public int getStart() {
			return start;
		}
		public int getLength() {
			return end-start;
		}
		@Override
		public int getStop() {
			return end-1;
		}
	}
	
	

}
