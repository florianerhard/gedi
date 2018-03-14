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
package gedi.util.algorithm.clustering.hierarchical;

import gedi.util.FileUtils;
import gedi.util.StringUtils;
import gedi.util.math.stat.RandomNumbers;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Function;


public class HierarchicalCluster<C> extends AbstractSet<C> {

	private C singleton;
	private double value = Double.NaN;
	private HierarchicalCluster<C> left;
	private HierarchicalCluster<C> right;
	private Object userdata;
	
	
	public HierarchicalCluster(C singleton) {
		this.singleton = singleton;
	}
	
	public HierarchicalCluster(HierarchicalCluster<C> left, HierarchicalCluster<C> right, double value) {
		this.left = left;
		this.right = right;
		this.value = value;
	}
	
	public void setUserdata(Object userdata) {
		this.userdata = userdata;
	}
	
	public <T> T getUserdata() {
		return (T) userdata;
	}
	
	public C getSingleton() {
		return singleton;
	}
	
	public HierarchicalCluster<C> leftChild() {
		return left;
	}
	
	public HierarchicalCluster<C> rightChild() {
		return right;
	}
	
	/**
	 * Starting with this node, perform a depth first search
	 * @param visitor
	 */
	public void dfs(Consumer<HierarchicalCluster<C>> visitor) {
		Stack<HierarchicalCluster<C>> dfs = new Stack<HierarchicalCluster<C>>();
		dfs.push(this);
		while (!dfs.isEmpty()) {
			HierarchicalCluster<C> c = dfs.pop();
			visitor.accept(c);
			if (!c.isSingleton()) {
				dfs.push(c.rightChild());
				dfs.push(c.leftChild());
			}
		}
	}
	
	public boolean isSingleton() {
		return left==null;
	}
	
	public double getValue() {
		return value;
	}
	
	@Override
	public boolean isEmpty() {
		return false;
	}
	
	@Override
	public Iterator<C> iterator() {
		return new ClusteredItemsIterator<C>(this);
	}
	@Override
	public int size() {
		return isSingleton()?1:left.size()+right.size();
	}
	
	@SuppressWarnings("unchecked")
	/**
	 * Cutoff is inclusive, i.e. two objects having distance/similarity exactly cutoff will be placed in a single cluster!
	 * @param cutoff
	 * @param distance
	 * @return
	 */
	public HierarchicalCluster<C>[] cut(double cutoff, boolean distance) {
		ArrayList<HierarchicalCluster<C>> re = new ArrayList<HierarchicalCluster<C>>();
		cutAndAdd(cutoff,re, distance);
		return re.toArray(new HierarchicalCluster[0]);
	}
	
	/**
	 * Cutoff is inclusive, i.e. two objects having distance/similarity exactly cutoff will be placed in a single cluster!
	 * @param cutoff
	 * @param distance
	 * @return
	 */
	public void cutAndAdd(double cutoff,List<HierarchicalCluster<C>> re, boolean distance) {
		Stack<HierarchicalCluster<C>> dfs = new Stack<HierarchicalCluster<C>>();
		dfs.add(this);
		while (!dfs.isEmpty()) {
			HierarchicalCluster<C> cur = dfs.pop();
			if (cur.isSingleton() || (distance?cur.value<=cutoff:cur.value>=cutoff))
				re.add(cur);
			else {
				dfs.add(cur.left);
				dfs.add(cur.right);
			}
		}
	}
	
	public C[] randomRepresentatives(RandomNumbers rnd, double cutoff, boolean distance) {
		HierarchicalCluster<C>[] cl = cut(cutoff,distance);
		C[] re = (C[]) Array.newInstance(cl[0].getRandomElement(rnd).getClass(), cl.length);
		for (int i=0; i<cl.length; i++)
			re[i] = cl[i].getRandomElement(rnd);
		return re;
	}
	
	public C getRandomElement() {
		return getRandomElement(new RandomNumbers());
	}
	public C getRandomElement(RandomNumbers rnd) {
		if (isSingleton())
			return singleton;
		if (rnd.getBool())
			return left.getRandomElement(rnd);
		return right.getRandomElement(rnd);
	}

	@Override
	public boolean equals(Object o) {
		return o==this;
	}
	
	@Override
	public int hashCode() {
		return isSingleton()?singleton.hashCode():Double.hashCode(value);
	}
	
	private void toString(StringBuilder sb, int indent, Function<C,String> annotator) {
		for (int i=0; i<indent; i++) sb.append(' ');
		if (isSingleton()) {
			sb.append(singleton.toString());
			String anno = annotator.apply(singleton);
			if (anno!=null)
				sb.append(" (").append(anno).append(")");
			sb.append("\n");
		}else{
			sb.append(String.format(Locale.US,"%g",value));
			sb.append("\n");
			left.toString(sb,indent+1,annotator);
			right.toString(sb,indent+1,annotator);
		}
	}
	

	public String toString(Function<C,String> annotator) {
		StringBuilder sb = new StringBuilder();
		toString(sb,0, annotator);
		return sb.toString();
	}
	
	@Override
	public String toString() {
		return toString(c->null);
	}

	private HierarchicalCluster(String[] lines, int[] indents, int from, int to, Function<String,C> transformer) {
		if (to-from==1){
			singleton = transformer.apply(lines[from].substring(indents[from]));
		} else {
			int i = indents[from+1];
			int r;
			for (r=from+2; r<to && indents[r]>i; r++);
			left = new HierarchicalCluster<C>(lines,indents,from+1,r,transformer);
			right = new HierarchicalCluster<C>(lines,indents,r,to,transformer);
			value = Double.parseDouble(lines[from].substring(indents[from]));
		}
	}
	
	
	public static <C> HierarchicalCluster<C> readDendrogram(String lines, Function<String,C> transformer) {
		return readDendrogram(StringUtils.split(lines, '\n'),transformer);
	}
	
	public static <C> HierarchicalCluster<C> readDendrogram(File file, Function<String,C> transformer) throws IOException {
		return readDendrogram(FileUtils.readAllLines(file),transformer);
	}
	
	public static <C> HierarchicalCluster<C> readDendrogram(String[] lines, Function<String,C> transformer) {
		int[] indents = new int[lines.length];
		for (int i=0; i<lines.length; i++)
			for (;indents[i]<lines[i].length() && lines[i].charAt(indents[i])==' ';indents[i]++);
		return new HierarchicalCluster<C>(lines, indents, 0, lines[lines.length-1].length()==0?lines.length-1:lines.length, transformer);
	}
	
	
	private static class ClusteredItemsIterator<C> implements Iterator<C> {

		private Stack<HierarchicalCluster<C>> dfs = new Stack<HierarchicalCluster<C>>();
		
		public ClusteredItemsIterator(HierarchicalCluster<C> root) {
			dfs.add(root);
		}
		
		@Override
		public boolean hasNext() {
			lookAhead();
			return !dfs.isEmpty() && dfs.peek().isSingleton();
		}

		@Override
		public C next() {
			lookAhead();
			return dfs.pop().singleton;
		}
		
		private void lookAhead() {
			while (!dfs.isEmpty() && !dfs.peek().isSingleton()) {
				HierarchicalCluster<C> top = dfs.pop();
				dfs.add(top.left);
				dfs.add(top.right);
			}
		}

		@Override
		public void remove() {}
		
	}

	
}
