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

package gedi.lfc.em;

import java.util.Arrays;

import gedi.util.ArrayUtils;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import cern.colt.bitvector.BitVector;

public class SingleConditionSimpleEm {

	private double[] el;
	private DoubleArrayList counts = new DoubleArrayList();
	
	public SingleConditionSimpleEm(double[] el) {
		this.el = el;
		if (el.length>31) throw new RuntimeException("Too many isoforms!");
	}
	
	public void add(double count, int... iso) {
		counts.increment(atoi(iso), count);
	}
	
	public void add(double count, BitVector iso) {
		counts.increment(vtoi(iso), count);
	}
	
	public void clear() {
		counts.clear();
	}
	
	private int atoi(int[] iso) {
		int r = 0;
		for (int i : iso)
			r|=1<<i;
		return r;
	}
	
	private int vtoi(BitVector iso) {
		return (int)iso.elements()[0];
	}


	public double[] em() {
		double[] v = new double[el.length];
		Arrays.fill(v, 1.0/v.length);
		
		for (int it=0; it<1000; it++) {
			double[] a = new double[el.length];
			for (int g=1; g<counts.size(); g++){
				if (counts.getDouble(g)>0) {
					double sum = 0;
					for (int i=0; i<g; i++)
						if ((g&(1<<i)) != 0) {
							sum+=v[i];
						}
					
					for (int i=0; i<g; i++)
						if ((g&(1<<i)) != 0) {
							a[i]+=v[i]/sum*counts.getDouble(g);
						}
				}
			}
			
			ArrayUtils.divide(a, el);
			ArrayUtils.normalize(a);
			double d = diff(a,v);
			v = a;
			if (d<1E-4) break;
//			System.out.println(Arrays.toString(v));
		}
		return v;
	}
	
	
	private double diff(double[] a, double[] b) {
		double d = 0;
		for (int i=0; i<a.length; i++)
			d+=Math.abs(a[i]-b[i]);
		return d;
	}

	public static void main(String[] args) {
		SingleConditionSimpleEm em = new SingleConditionSimpleEm(new double[] {1,1});
		em.add(10,0,1);
		em.add(10,0);
		em.add(0,1);
		em.add(10,0,1);
		System.out.println(Arrays.toString(em.em()));
		
		
		em = new SingleConditionSimpleEm(new double[] {1,1});
		em.add(10,0,1);
		em.add(5,0);
		em.add(5,1);
		em.add(10,0,1);
		System.out.println(Arrays.toString(em.em()));
		
		
		em = new SingleConditionSimpleEm(new double[] {1,1});
		em.add(10,0,1);
		em.add(5,0);
		em.add(5,1);
		em.add(1000,0,1);
		System.out.println(Arrays.toString(em.em()));
		
		em = new SingleConditionSimpleEm(new double[] {1,1});
		em.add(10,0,1);
		em.add(5,0);
		em.add(500,1);
		em.add(10,0,1);
		System.out.println(Arrays.toString(em.em()));
	}
	
}
