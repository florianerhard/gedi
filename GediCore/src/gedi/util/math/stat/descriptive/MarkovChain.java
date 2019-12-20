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
package gedi.util.math.stat.descriptive;

import gedi.util.ArrayUtils;
import gedi.util.FunctorUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.functions.ExtendedIterator;
import gedi.util.math.stat.RandomNumbers;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.EigenvalueDecomposition;

public class MarkovChain<T> {

	private RandomNumbers stoch = new RandomNumbers();
	private T[] v;
	private HashMap<T, Integer> index;
	private double[][] p;
	private double[] start;
	private static double EPS = 1E-6;
	
	private double[] cumStart;
	private double[][] cumP;
	private double[] stationary;
	
	public MarkovChain(T[] v, double[][] p, double[] start) {
		this.p = p;
		this.v = v;
		this.start = start;
		this.index = ArrayUtils.createIndexMap(v);
		int n = v.length;
		if (p.length!=n || start.length!=n)
			throw new IllegalArgumentException("Incorrect length!");
		for (int i=0; i<n; i++)
			if (p[i].length!=n)
				throw new IllegalArgumentException("Must be a matrix!");
			else if (Math.abs(ArrayUtils.sum(p[i])-1)>EPS)
				throw new IllegalArgumentException("Must be a semi probabilistic matrix!");
		
		cumStart = start.clone();
		ArrayUtils.cumSumInPlace(cumStart, 1);
		cumP = new double[n][];
		for (int i=0; i<n; i++) {
			cumP[i] = p[i].clone();
			ArrayUtils.cumSumInPlace(cumP[i], 1);
		}
	}

	public void setRandom(RandomNumbers stoch) {
		this.stoch = stoch;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(StringUtils.concat(",", v));
		sb.append("\n");
		sb.append(StringUtils.concat(",", start));
		sb.append("\n");
		sb.append(ArrayUtils.matrixToString(p));
		return sb.toString();
	}
	
	public double[][] getTransitionMatrix() {
		return p;
	}
	
	public double getTransitionProbability(T from, T to) {
		return p[index.get(from)][index.get(to)];
	}
	
	public double[] getStartDistribution() {
		return start;
	}
	
	public T[] getItems() {
		return v;
	}
	
	public double[] getStationary() {
		if (stationary==null) {
			EigenvalueDecomposition eig = new EigenvalueDecomposition(new DenseDoubleMatrix2D(p));
	        DoubleMatrix2D V = eig.getV();
	        DoubleMatrix1D real = eig.getRealEigenvalues();
	        for (int i = 0; i < v.length; i++) {
	            if (Math.abs(real.getQuick(i)-1) < EPS) {
	                stationary = V.viewColumn(i).toArray();
	                ArrayUtils.normalize(stationary);
	                return stationary;
	            }
	        }
	        throw new RuntimeException("Could not find eigenvalue 1!");
		}
		return stationary;
	}
	
	public double computeUnconditionedProbability(Iterator<T> it) {
		if (!it.hasNext()) return Double.NaN;
		int last = index.get(it.next());
		double r = 1;
		while (it.hasNext()) {
			int next = index.get(it.next()); 
			r*=p[last][next];
			last = next;
		}
		return r;
	}
	public double computeProbability(Iterator<T> it) {
		double[] stat = getStationary();
		if (!it.hasNext()) return Double.NaN;
		int last = index.get(it.next());
		double r = stat[last];
		while (it.hasNext()) {
			int next = index.get(it.next()); 
			r*=p[last][next];
			last = next;
		}
		return r;
	}
	public double computeProbabilityFromStart(Iterator<T> it) {
		if (!it.hasNext()) return Double.NaN;
		Integer Last = index.get(it.next());
		if (Last==null) return 0;
		int last = Last.intValue();
		double r = start[last];
		while (it.hasNext()) {
			Last = index.get(it.next());
			if (Last==null) return 0;
			int next = Last.intValue(); 
			r*=p[last][next];
			last = next;
		}
		return r;
	}
	public ExtendedIterator<T> iterate() {
		return iterate(-1);
	}
	
	public ExtendedIterator<T> iterate(final int n) {
		return new ExtendedIterator<T>() {
			int i=n;
			int last = -1;
			@Override
			public boolean hasNext() {
				return i!=0;
			}

			@Override
			public T next() {
				if (i==0) return null;
				i--;
				int re = last==-1?getRandomStartIndex():getRandomTransitionIndex(last);
				last = re;
				return v[re];
			}

			@Override
			public void remove() {
			}
			
		};
	}
	
	public T getRandomStart() {
		return v[stoch.getCategorial(cumStart)];
	}
	
	public int getRandomStartIndex() {
		return stoch.getCategorial(cumStart);
	}

	public T getRandomTransition(T state) {
		return v[stoch.getCategorial(cumP[index.get(state)])];
	}
	
	public int getRandomTransitionIndex(int stateIndex) {
		return stoch.getCategorial(cumP[stateIndex]);
	}


	/**
	 * s=1: dinucleotide!
	 * @param str
	 * @param s
	 * @return
	 */
	public static MarkovChain<String> fromString(String str,int s) {
		return fromIterator(String.class,FunctorUtils.substringIterator(str,s));
	}
	
	/**
	 * s=1: dinucleotide!
	 * @param str
	 * @param s
	 * @return
	 */
	public static MarkovChain<String> fromString(String str,int s,boolean estimateStartByMarginal) {
		return fromIterator(String.class,FunctorUtils.substringIterator(str,s),estimateStartByMarginal);
	}
	
	
	/**
	 * null is stopper!
	 * @param <T>
	 * @param it
	 * @return
	 */
	public static <T> MarkovChain<T> fromIterator(Class<T> cls, Iterator<T> it) {
		return new MarkovChainCreator<T>(cls).addAll(it).create(false);
	}
	
	public static <T> MarkovChain<T> fromIterator(Class<T> cls, Iterator<T> it,boolean estimateStartByMarginal) {
		return new MarkovChainCreator<T>(cls).addAll(it).create(estimateStartByMarginal);
	}
	
	public static class MarkovChainCreator<T> {
		Class<T> cls;
		HashMap<T,Integer> index = new HashMap<T, Integer>();
		ArrayList<IntArrayList> counter = new ArrayList<IntArrayList>(); 
		IntArrayList start = new IntArrayList();
		T last = null;
		int n = 0;
		
		public MarkovChainCreator(Class<T> cls) {
			this.cls = cls;
		}

		public MarkovChainCreator<T> add(T n) {
			if (n!=null && !index.containsKey(n)) {
				index.put(n, index.size());
				counter.add(new IntArrayList());
			}
			if (last!=null && n!=null) 
				counter.get(index.get(last)).increment(index.get(n));
			
			if (n!=null && last==null) {
				this.n++;
				start.increment(index.get(n));
			}
			
			last = n;
			return this;
		}
		
		public MarkovChainCreator<T> addAll(Iterator<T> it) {
			last = null;
			while (it.hasNext()) {
				T n = it.next();
				if (n!=null && !index.containsKey(n)) {
					index.put(n, index.size());
					counter.add(new IntArrayList());
				}
				if (last!=null && n!=null) 
					counter.get(index.get(last)).increment(index.get(n));
				
				if (n!=null && last==null) {
					this.n++;
					start.increment(index.get(n));
				}
				
				last = n;
			}
			last = null;
			return this;
		}
		
		public int getNumSequences() {
			return n;
		}
		
		public MarkovChain<T> create(boolean estimateStartByMarginal) {
			double[] start =this.start.toDoubleArray(index.size());
			double[][] re = new double[index.size()][];
			for (int i=0; i<counter.size(); i++) { 
				re[i] = counter.get(i).toDoubleArray(counter.size());
				if (estimateStartByMarginal)
					start[i] = ArrayUtils.sum(re[i]);
				ArrayUtils.normalize(re[i]);
			}
			ArrayUtils.normalize(start);
			T[] v = (T[])Array.newInstance(cls, index.size());
			for (T t : index.keySet())
				v[index.get(t)] = t;
			
			return new MarkovChain<T>(v, re, start);
		}

	}
	
}
