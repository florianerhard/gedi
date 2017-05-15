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

package gedi.core.data.reads;

import gedi.util.ArrayUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.datastructure.collections.intcollections.IntArrayList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import cern.colt.bitvector.BitVector;

public class AlignedReadsDataFactory {

	protected int currentDistinct;
	protected int conditions;
	
	protected ArrayList<int[]> count = new ArrayList<int[]>();
//	protected ArrayList<IntArrayList> var = new ArrayList<IntArrayList>();
//	protected ArrayList<ArrayList<CharSequence>> indels = new ArrayList<ArrayList<CharSequence>>();
	protected ArrayList<TreeSet<VarIndel>> var = new ArrayList<TreeSet<VarIndel>>();
	protected IntArrayList multiplicity = new IntArrayList();
	protected DoubleArrayList weights = new DoubleArrayList();
	protected IntArrayList ids = new IntArrayList();
//	protected ArrayList ids = new ArrayList();

	public AlignedReadsDataFactory(int conditions) {
		this.conditions = conditions;
	}
	
	public static void removeId(DefaultAlignedReadsData ard) {
		ard.ids = null;
	}
	
	public AlignedReadsDataFactory start() {
		currentDistinct=-1;
		count.clear();
		var.clear();
		multiplicity.clear();
		weights.clear();
		ids.clear();
		return this;
	}
	public int getDistinctSequences() {
		return currentDistinct+1;
	}
	public AlignedReadsDataFactory newDistinctSequence() {
		currentDistinct++;
		setMultiplicity(1);
		
		this.count.add(new int[conditions]);
		this.var.add(new TreeSet<VarIndel>());
		
		return this;
	}
	
	private void checkDistinct() {
		if (currentDistinct<0) throw new RuntimeException("Call newDistinctSequence first!");
	}
	private void checkDistinct(int distinct) {
		if (currentDistinct<0 || distinct>currentDistinct) throw new RuntimeException("Call newDistinctSequence first!");
	}
	
//	private void checkPos(int pos) {
//		if (var.get(currentDistinct).size()==0) return;
//		int lastPos = DefaultAlignedReadsData.pos(var.get(currentDistinct).getLastInt());
//		if (lastPos>=pos) throw new RuntimeException("Provide variations in-order: "+lastPos+" >= "+pos);
//	}

	public AlignedReadsDataFactory setMultiplicity(int m) {
		checkDistinct();
		multiplicity.set(currentDistinct, m);
		return this;
	}
	
	public AlignedReadsDataFactory setId(int id) {
		checkDistinct();
		if (ids.size()<currentDistinct)
			throw new RuntimeException("Call setId for all distinct sequences!");
		setId(currentDistinct, id);
		return this;
	}	
	public AlignedReadsDataFactory setWeight(float weight) {
		checkDistinct();
		if (weights.size()<currentDistinct)
			throw new RuntimeException("Call setWeight for all distinct sequences!");
		setWeight(currentDistinct, weight);
		return this;
	}
	
	public AlignedReadsDataFactory setCount(int[] count) {
		checkDistinct();
		System.arraycopy(count, 0, this.count.get(currentDistinct), 0, conditions);
		return this;
	}
	
	public AlignedReadsDataFactory setCount(int condition, int count) {
		checkDistinct();
		this.count.get(currentDistinct)[condition]=count;
		return this;
	}
	
	public AlignedReadsDataFactory incrementCount(int condition, int count) {
		checkDistinct();
		this.count.get(currentDistinct)[condition]+=count;
		return this;
	}
	
	public AlignedReadsDataFactory setId(int distinct,int id) {
		checkDistinct(distinct);
		ids.set(distinct, id);
		return this;
	}
	public AlignedReadsDataFactory setWeight(int distinct,float weight) {
		checkDistinct(distinct);
		weights.set(distinct, weight);
		return this;
	}
	
//	
//	public AlignedReadsDataFactory setId(int distinct,long id) {
//		checkDistinct(distinct);
//		checkId(Long.class);
//		ids.set(distinct, id);
//		return this;
//	}
//	
//	public AlignedReadsDataFactory setId(int distinct,String id) {
//		checkDistinct(distinct);
//		checkId(String.class);
//		ids.set(distinct, id);
//		return this;
//	}
//	
//	
//	private void checkId(Class<?> cls) {
//		if (currentDistinct>0) {
//			if (ids.get(0).getClass()!=cls)
//				throw new RuntimeException("Id types must be homogeneous!");
//		}
//	}

	public AlignedReadsDataFactory setMultiplicity(int distinct,int m) {
		checkDistinct(distinct);
		multiplicity.set(distinct, m);
		return this;
	}
	
	public int getMultiplicity(int distinct) {
		checkDistinct(distinct);
		return multiplicity.getInt(distinct);
	}
	
	public AlignedReadsDataFactory setCount(int distinct,int[] count) {
		checkDistinct(distinct);
		System.arraycopy(count, 0, this.count.get(distinct), 0, conditions);
		return this;
	}
	
	public AlignedReadsDataFactory setCount(int distinct,int condition, int count) {
		checkDistinct(distinct);
		this.count.get(distinct)[condition]=count;
		return this;
	}
	
	public AlignedReadsDataFactory incrementCount(int distinct,int condition, int count) {
		checkDistinct(distinct);
		this.count.get(distinct)[condition]+=count;
		return this;
	}
	
	public AlignedReadsDataFactory addMismatch(int pos, char genomic, char read) {
		getCurrentVariationBuffer().add(createMismatch(pos, genomic, read));
		return this;
	}
	
	public AlignedReadsDataFactory addSoftclip(int pos, CharSequence read) {
		getCurrentVariationBuffer().add(createSoftclip(pos, read));
		return this;
	}
	
	public AlignedReadsDataFactory addDeletion(int pos, CharSequence genomic) {
		getCurrentVariationBuffer().add(createDeletion(pos, genomic));
		return this;
	}
	
	public AlignedReadsDataFactory addInsertion(int pos, CharSequence read) {
		getCurrentVariationBuffer().add(createInsertion(pos, read));
		return this;
	}
	
	protected Collection<VarIndel> getCurrentVariationBuffer() {
		return this.var.get(currentDistinct);
	}
	
	public VarIndel createVarIndel(String variationString) {
		
		String rest = variationString.substring(1);
		int n = StringUtils.countPrefixInt(rest);
		if (n==0) throw new IllegalArgumentException("No pos found in "+variationString);
		
		int pos = Integer.parseInt(rest.substring(0, n));
		rest = rest.substring(n);
		
		if (variationString.startsWith("M"))
			return createMismatch(pos, rest.charAt(0), rest.charAt(1));
		
		if (variationString.startsWith("I"))
			return createInsertion(pos, rest);
		
		if (variationString.startsWith("D"))
			return createDeletion(pos, rest);
		
		throw new IllegalArgumentException("Must start with M/I/D: "+variationString);
	}
	
	public void addVariation(AlignedReadsVariation vari) {
		if (vari.isDeletion())
			addDeletion(vari.getPosition(), vari.getReferenceSequence());
		else if (vari.isInsertion())
			addInsertion(vari.getPosition(), vari.getReadSequence());
		else if (vari.isMismatch())
			addMismatch(vari.getPosition(), vari.getReferenceSequence().charAt(0),vari.getReadSequence().charAt(0));
		else if (vari.isSoftclip())
			addSoftclip(vari.getPosition(), vari.getReadSequence());
	}

	public VarIndel createSoftclip(int pos, CharSequence read) {
		checkDistinct();
//		checkPos(pos);
		VarIndel v = new VarIndel();
		v.var = DefaultAlignedReadsData.encodeSoftclip(pos, read);
		v.indel = DefaultAlignedReadsData.encodeSoftclipIndel(pos, read);
		return v;
	}
	
	
	public VarIndel createMismatch(int pos, char genomic, char read) {
		checkDistinct();
//		checkPos(pos);
		VarIndel v = new VarIndel();
		v.var = DefaultAlignedReadsData.encodeMismatch(pos, genomic, read);
		v.indel = DefaultAlignedReadsData.encodeMismatchIndel(pos, genomic, read);
		return v;
	}
	
	public VarIndel createDeletion(int pos, CharSequence genomic) {
		checkDistinct();
//		checkPos(pos);
		VarIndel v = new VarIndel();
		v.var = DefaultAlignedReadsData.encodeDeletion(pos, genomic);
		v.indel = DefaultAlignedReadsData.encodeDeletionIndel(pos, genomic);
		return v;
	}
	
	public VarIndel createInsertion(int pos, CharSequence read) {
		checkDistinct();
//		checkPos(pos);
		VarIndel v = new VarIndel();
		v.var = DefaultAlignedReadsData.encodeInsertion(pos, read);
		v.indel = DefaultAlignedReadsData.encodeInsertionIndel(pos, read);
		return v;
	}
	
	public static class VarIndel implements Comparable<VarIndel> {
		short var;
		CharSequence indel;
		@Override
		public int compareTo(VarIndel o) {
			int re = Integer.compare(DefaultAlignedReadsData.pos(var), DefaultAlignedReadsData.pos(o.var));
			if (re==0) re = StringUtils.compare(indel, o.indel);
			return re;
		}
		
		public int getPosition() {
			return DefaultAlignedReadsData.pos(var);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((indel == null) ? 0 : indel.hashCode());
			result = prime * result + var;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			VarIndel other = (VarIndel) obj;
			if (indel == null) {
				if (other.indel != null)
					return false;
			} else if (!indel.equals(other.indel))
				return false;
			if (var != other.var)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "VarIndel [var=" + var + ", indel=" + indel + "]";
		}

		
		
	}
	
	public boolean areTrulyDistinct() {
		HashSet<TreeSet<VarIndel>> h = new HashSet<TreeSet<VarIndel>>();
		for (TreeSet<VarIndel> t : var)
			h.add(t);
		return h.size()==var.size();
	}
	
	public void makeDistinct() {
		HashMap<TreeSet<VarIndel>,Integer> h = new HashMap<TreeSet<VarIndel>,Integer>();
		for (int i=0; i<var.size(); i++) {
			Integer idx = h.get(var.get(i));
			if (idx==null) h.put(var.get(i), i);
			else {
				ArrayUtils.add(count.get(idx), count.get(i));
				ids.set(idx, Math.min(ids.getInt(idx),ids.getInt(i)));
			}
		}
		
		
		
		BitVector keep = new BitVector(var.size());
		for (Integer i : h.values()) 
			keep.putQuick(i, true);
		
		count = restrict(count,keep);
		var = restrict(var,keep);
		multiplicity = restrict(multiplicity,keep);
		weights = restrict(weights,keep);
		ids = restrict(ids,keep);
		
		currentDistinct = count.size()-1;
	}
	
	
	private <T> ArrayList<T> restrict(ArrayList<T> l, BitVector keep) {
		ArrayList<T> re = new ArrayList<T>();
		for (int i=0; i<l.size(); i++)
			if (keep.getQuick(i))
				re.add(l.get(i));
		return re;
	}
	private IntArrayList restrict(IntArrayList l, BitVector keep) {
		IntArrayList re = new IntArrayList();
		for (int i=0; i<l.size(); i++)
			if (keep.getQuick(i))
				re.add(l.getInt(i));
		return re;
	}
	private DoubleArrayList restrict(DoubleArrayList l, BitVector keep) {
		DoubleArrayList re = new DoubleArrayList();
		for (int i=0; i<l.size(); i++)
			if (keep.getQuick(i))
				re.add(l.getDouble(i));
		return re;
	}

	public DefaultAlignedReadsData create() {
		DefaultAlignedReadsData re = new DefaultAlignedReadsData();
		re.count = convInt(count);
		re.var = convShort(var);
		re.indels = convS(var);
		re.multiplicity = multiplicity.toIntArray();
		if (ids.size()==re.count.length)
			re.ids = ids.toIntArray();
		else if (ids.size()>0)
			throw new RuntimeException("Call setId for each distinct sequence or for none!");
		
		if (weights.size()==re.count.length)
			re.weights = weights.toFloatArray();
		else if (weights.size()>0)
			throw new RuntimeException("Call setWeight for each distinct sequence or for none!");
		
		return re;
	}
	
	public AlignedReadsData createDigital() {
		DigitalAlignedReadsData re = new DigitalAlignedReadsData();
		if (count.size()!=1 || var.get(0).size()>0) throw new RuntimeException();
		
		re.count = new BitVector(count.get(0).length);
		for (int i=0; i<re.count.size(); i++)
			re.count.putQuick(i, count.get(0)[i]>0);
		return re;
	}


	private CharSequence[][] convS(ArrayList<TreeSet<VarIndel>> var) {
		CharSequence[][] re = new CharSequence[currentDistinct+1][];
		for (int i=0; i<re.length; i++) {
			re[i] = new CharSequence[var.get(i).size()];
			int j=0;
			for (VarIndel v : var.get(i))
				re[i][j++] = v.indel;
		}
		return re;
	}

	private int[][] convInt(ArrayList<int[]> a) {
		int[][] re = new int[currentDistinct+1][];
		for (int i=0; i<re.length; i++) 
			re[i] = a.get(i).clone();
		return re;
	}

	private short[][] convShort(ArrayList<TreeSet<VarIndel>> var) {
		short[][] re = new short[currentDistinct+1][];
		for (int i=0; i<re.length; i++) {
			re[i] = new short[var.get(i).size()];
			int j=0;
			for (VarIndel v : var.get(i))
				re[i][j++] = v.var;
		}
		return re;
	}


	public int getNumConditions() {
		return conditions;
	}

	/**
	 * 
	 * @param ard
	 * @param distinct take the distinct from ard!
	 */
	public AlignedReadsDataFactory add(AlignedReadsData ard, int distinct) {
		newDistinctSequence();
		for (int i=0; i<ard.getNumConditions(); i++)
			setCount(i, ard.getCount(distinct, i));
		setMultiplicity(ard.getMultiplicity(distinct));
		for (int i=0; i<ard.getVariationCount(distinct); i++)
			addVariation(ard.getVariation(distinct, i));
		if (ard.hasId())
			setId(ard.getId(distinct));
		if (ard.hasWeights())
			setWeight(ard.getWeight(distinct));
		return this;
	}

	public void incrementCount(AlignedReadsData ard) {
		for (int i=0; i<ard.getNumConditions(); i++)
			incrementCount(i, ard.getTotalCountForConditionInt(i,ReadCountMode.All));
	}

	

	
	
	
}
