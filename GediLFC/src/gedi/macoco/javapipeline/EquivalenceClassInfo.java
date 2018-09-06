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
package gedi.macoco.javapipeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import gedi.util.ArrayUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;

public class EquivalenceClassInfo {
	
	private String[] cond;
	
	// [E classes][transcript names]
	private String[][] E;
	//[conditions][E classes]
	private double[][] counts;
	//[conditions][E classes]
	private double[][] effLen;
	private double[][] readLengths;
	
	public EquivalenceClassInfo(){}

			
	public EquivalenceClassInfo(String counts, String lengths, String efflen) throws IOException {
		ExtendedIterator<String[]> lit = EI.lines(counts).map(a->StringUtils.split(a, '\t'));
		String[] header = lit.next();
		this.cond = ArrayUtils.slice(header, 1);
		
		ArrayList<String[]> E = new ArrayList<>();
		
		DoubleArrayList[] c = new DoubleArrayList[header.length-1];
		for (int i=0; i<c.length; i++)
			c[i] = new DoubleArrayList();
		
		for (String[] f : lit.loop()) {
			E.add(StringUtils.split(f[0], ','));
			for (int i=0; i<c.length; i++)
				c[i].add(Double.parseDouble(f[i+1]));
		}
		
		// read lengths
		
		lit = EI.lines(lengths).map(a->StringUtils.split(a, '\t')).skip(1);
		
		DoubleArrayList[] rl = new DoubleArrayList[c.length];
		for (int i=0; i<c.length; i++)
			rl[i] = new DoubleArrayList();

		for (String[] f : lit.loop()) {
			int l = Integer.parseInt(f[0]);
			for (int i=0; i<c.length; i++)
				rl[i].set(l,Double.parseDouble(f[i+1]));
		}
		
		if (efflen!=null) {
			lit = EI.lines(efflen).map(a->StringUtils.split(a, '\t')).skip(1);
			
			DoubleArrayList[] effl = new DoubleArrayList[header.length-1];
			for (int i=0; i<effl.length; i++)
				effl[i] = new DoubleArrayList();
			
			for (String[] f : lit.loop()) {
				for (int i=0; i<effl.length; i++)
					effl[i].add(Double.parseDouble(f[i+1]));
			}
			
			this.effLen = new double[effl.length][];
			for (int i=0; i<effl.length; i++)
				this.effLen[i] = effl[i].toDoubleArray();
			
		}
			
		
		this.E = E.toArray(new String[0][]);
		this.counts = new double[c.length][];
		for (int i=0; i<c.length; i++)
			this.counts[i] = c[i].toDoubleArray();
		this.readLengths = new double[c.length][];
		for (int i=0; i<c.length; i++)
			this.readLengths[i] = rl[i].toDoubleArray();
	}
	
	
	public String[][] getE() {
		return E;
	}
	
	public double[][] getCounts() {
		return counts;
	}

	public double[][] getEffLen() {
		return effLen;
	}
	
	public double[][] getReadLengths() {
		return readLengths;
	}

	public String[] getConditionNames() {
		return cond;
	}
	
	
	/**
	 * Will return null if there is an equivalence class [name]!
	 * @param name
	 * @return
	 */
	public EquivalenceClassInfo removeTranscript(String name) {
		EquivalenceClassInfo re = new EquivalenceClassInfo();
		re.cond = cond;
		re.readLengths = readLengths;
		
		ArrayList<String[]> nE = new ArrayList<>();
		DoubleArrayList[] c = new DoubleArrayList[cond.length];
		for (int i=0; i<c.length; i++)
			c[i] = new DoubleArrayList();
		DoubleArrayList[] e = new DoubleArrayList[cond.length];
		for (int i=0; i<e.length; i++)
			e[i] = new DoubleArrayList();
		
		HashMap<List<String>,Integer> index = new HashMap<>();
		for (int i=0; i<E.length; i++) {
			int p = ArrayUtils.find(E[i], name);
			if (p>=0) {
				if (E[i].length==1) return null;
				String[] rE = ArrayUtils.removeItemFromArray(E[i], p);
				Integer ind = index.get(Arrays.asList(rE));
				if (ind==null) {
					index.put(Arrays.asList(rE), nE.size());
					nE.add(rE);
					for (int ci=0; ci<c.length; ci++)
						c[ci].add(counts[ci][i]);
					if (effLen!=null)
						for (int ci=0; ci<e.length; ci++)
							e[ci].add(effLen[ci][i]);
				} else {
					for (int ci=0; ci<c.length; ci++)
						c[ci].increment(ind, counts[ci][i]);
					if (effLen!=null)
						for (int ci=0; ci<e.length; ci++)
							e[ci].increment(ind, effLen[ci][i]);
				}
			} else {
				Integer ind = index.get(Arrays.asList(E[i]));
				if (ind==null) {
					index.put(Arrays.asList(E[i]), nE.size());
					nE.add(E[i]);
					for (int ci=0; ci<c.length; ci++)
						c[ci].add(counts[ci][i]);
					if (effLen!=null)
						for (int ci=0; ci<e.length; ci++)
							e[ci].add(effLen[ci][i]);
				} else {
					for (int ci=0; ci<c.length; ci++)
						c[ci].increment(ind, counts[ci][i]);
					if (effLen!=null)
						for (int ci=0; ci<e.length; ci++)
							e[ci].increment(ind, effLen[ci][i]);
				}
			}
		}
		
		re.E = nE.toArray(new String[0][]);
		re.counts = new double[c.length][];
		for (int i=0; i<c.length; i++)
			re.counts[i] = c[i].toDoubleArray();
		
		if (effLen!=null) {
			re.effLen = new double[e.length][];
			for (int i=0; i<e.length; i++)
				re.effLen[i] = e[i].toDoubleArray();
		}
		
		return re;
	}
	
}