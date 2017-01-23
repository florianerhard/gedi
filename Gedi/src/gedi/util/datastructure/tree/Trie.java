package gedi.util.datastructure.tree;


import gedi.util.functions.ExtendedIterator;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
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
		
		public Node(Node sibling, char c) {
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
			child = new Node(child,c);
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
	
	
	private Node root = new Node(null,'\0');
	private int size = 0;
	private Object nullValue = new Object();
	
	@Override
	public T put(String key, T value) {
		suffixLinks = null;
		Node n = root;
		int i = 0;
		for (Node t = n; t!=null && i<key.length(); ) {
			t = n.findChild(key.charAt(i));
			if (t!=null) {
				n = t;
				i++;
			}
		}
		for (; i<key.length(); i++) 
			n = n.addChild(key.charAt(i));
		
		n.value = value==null?nullValue:value;
		size++;
		return value;
	}
	
	
	@Override
	public int size() {
		return size;
	}
	
	@Override
	public void clear() {
		suffixLinks = null;
		root.clear();
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
				for (Node nn : sorter)
					dfs.push(nn);
				sorter.clear();
			}
		}
		return this;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public T get(Object key) {
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
			if (i==s.length())
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
				suffixLinks = null;
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
	
	private transient HashMap<Node,Node> suffixLinks;
	private transient HashMap<Node,Integer> nodeLevel;
	private transient HashMap<Node,LinkedList<Node>> additionalResults;
	private void prepareAhoCorasick() {
		if (suffixLinks!=null) return;
		suffixLinks = new HashMap<Node, Node>();
		nodeLevel = new HashMap<Trie.Node, Integer>();
		additionalResults = new HashMap<Node, LinkedList<Node>>();
		LinkedList<Node> q  = new LinkedList<Node>();
		nodeLevel.put(root, 0);
		for (Node ch=root.child; ch!=null; ch=ch.sibling) {
			suffixLinks.put(ch, root);
			q.add(ch);
			nodeLevel.put(ch, 1);
		}
		while (!q.isEmpty()) {
			Node r = q.removeFirst();
			int rlevel = nodeLevel.get(r);
			for (Node ch=r.child; ch!=null; ch=ch.sibling) {
				nodeLevel.put(ch, rlevel+1);
				q.add(ch);
				Node v = acFailure(r);
				while (acGoto(v, ch.c)==null)
					v = acFailure(v);
				suffixLinks.put(ch, acGoto(v, ch.c));
				LinkedList<Node> ar = additionalResults.get(ch);
				if (ar==null) additionalResults.put(ch, ar=new LinkedList<Node>());
				acOut(acFailure(ch), ar);
			}	
		}
		Iterator<Node> it2 = additionalResults.keySet().iterator();
		while (it2.hasNext()) 
			if (additionalResults.get(it2.next()).size()==0)
				it2.remove();
	}
	
	private Node acGoto(Node q, char a) {
		Node re = q.findChild(a);
		if (re==null && q==root) return root;
		return re;
	}
	
	private Node acFailure(Node q) {
		if (q==root) return root;
		return suffixLinks.get(q);
	}
	
	private void acOut(Node q, List<Node> l) {
		if (q.value!=null) l.add(q);
		if (additionalResults.containsKey(q))
			l.addAll(additionalResults.get(q));
	}
	
	
	public <C extends Collection<AhoCorasickResult<T>>> C ahoCorasick(String text, C re) {
		Iterator<AhoCorasickResult<T>> it = iterateAhoCorasick(text, false);
		while (it.hasNext())
			re.add(it.next());
		return re;
	}
	
	public ArrayList<AhoCorasickResult<T>> ahoCorasick(String text) {
		return ahoCorasick(text, new ArrayList<AhoCorasickResult<T>>());
	}
	
	public ExtendedIterator<AhoCorasickResult<T>> iterateAhoCorasick(final String text) {
		return iterateAhoCorasick(text, false);
	}
	public ExtendedIterator<AhoCorasickResult<T>> iterateAhoCorasick(final String text, final boolean reUse) {
		prepareAhoCorasick();
		final AhoCorasickResult<T> re = new AhoCorasickResult<T>();
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
				re.end = index;
				re.start = index-nodeLevel.get(n);
				re.object = (T)n.value;
				return reUse?re:new AhoCorasickResult<T>(re);
			}
			
			private void lookAhead() {
				for (; values.size()==0 && index<text.length(); index++) {
					while (acGoto(q, text.charAt(index))==null)
						q = acFailure(q);
					q = acGoto(q, text.charAt(index));
					acOut(q, values);
				}
			}
			
		};
	}
	
	
	
	public static class AhoCorasickResult<T> {
		int start;
		int end;
		T object;
		AhoCorasickResult(){}
		AhoCorasickResult(AhoCorasickResult<T> ori) {
			this.start = ori.start;
			this.end =ori.end;
			this.object = ori.object;
		}
		
		public int getEnd() {
			return end;
		}
		
		public T getValue() {
			return object;
		}
		@Override
		public String toString() {
			return object+"@"+start+"-"+end;
		}
		public int getStart() {
			return start;
		}
		public int getLength() {
			return end-start;
		}
	}
	
	

}
