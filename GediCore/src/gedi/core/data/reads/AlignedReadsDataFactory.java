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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.function.UnaryOperator;

import cern.colt.bitvector.BitVector;
import gedi.util.ArrayUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.sequence.DnaSequence;

public class AlignedReadsDataFactory {

	protected int currentDistinct;
	protected int conditions;
	
	protected ArrayList<int[]> count = new ArrayList<int[]>();
	protected ArrayList<int[]> nonzeros;
	protected IntArrayList nextcount;
	
//	protected ArrayList<IntArrayList> var = new ArrayList<IntArrayList>();
//	protected ArrayList<ArrayList<CharSequence>> indels = new ArrayList<ArrayList<CharSequence>>();
	protected ArrayList<TreeSet<VarIndel>> var = new ArrayList<TreeSet<VarIndel>>();
	protected IntArrayList multiplicity = new IntArrayList();
	protected DoubleArrayList weights = new DoubleArrayList();
	protected IntArrayList geometry = new IntArrayList();
	protected IntArrayList ids = new IntArrayList();
//	protected ArrayList ids = new ArrayList();
	
	protected ArrayList<ArrayList<DnaSequence>[]> barcodes = new ArrayList<ArrayList<DnaSequence>[]>();

	public AlignedReadsDataFactory(int conditions) {
		this(conditions,conditions>5);
	}
	public AlignedReadsDataFactory(int conditions, boolean sparse) {
		this.conditions = conditions;
		if (sparse) {
			nonzeros = new ArrayList<int[]>();
			nextcount = new IntArrayList();
		}
	}
	
	
	
	public static void removeId(DefaultAlignedReadsData ard) {
		ard.ids = null;
	}
	
	public AlignedReadsDataFactory start() {
		currentDistinct=-1;
		count.clear();
		if (isSparse()) {
			nonzeros.clear();
			nextcount.clear();
		}
		var.clear();
		multiplicity.clear();
		weights.clear();
		geometry.clear();
		ids.clear();
		barcodes.clear();
		return this;
	}
	public int getDistinctSequences() {
		return currentDistinct+1;
	}
	public AlignedReadsDataFactory newDistinctSequence() {
		currentDistinct++;
		setMultiplicity(1);
		
		this.count.add(new int[conditions]);
		if (isSparse()) {
			this.nonzeros.add(new int[conditions]);
			this.nextcount.add(0);
		}
		this.var.add(new TreeSet<VarIndel>());
		this.barcodes.add(new ArrayList[conditions]);
		
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
	
	public AlignedReadsDataFactory setGeometry(int before, int overlap, int after) {
		checkDistinct();
		if (geometry.size()<currentDistinct)
			throw new RuntimeException("Call setGeometry for all distinct sequences!");
		setGeometry(currentDistinct, before, overlap, after);
		return this;
	}
	
	public AlignedReadsDataFactory setCount(int[] count) {
		checkDistinct();
		if (isSparse()) {
			for (int i=0; i<count.length; i++)
				if (count[i]>0)
					setCount(i,count[i]);
		} 
		else 
			System.arraycopy(count, 0, this.count.get(currentDistinct), 0, conditions);
		return this;
	}
	
	public AlignedReadsDataFactory setCount(int condition, int count) {
		checkDistinct();
		if (isSparse()) {
			int c = Arrays.binarySearch(nonzeros.get(currentDistinct),0,nextcount.getInt(currentDistinct),condition);
			if (c>=0) {
				if (count==0) {
					this.nextcount.decrement(currentDistinct);
					this.count.get(currentDistinct)[c] = this.count.get(currentDistinct)[this.nextcount.getInt(currentDistinct)];
					this.nonzeros.get(currentDistinct)[c] = this.nonzeros.get(currentDistinct)[this.nextcount.getInt(currentDistinct)];
				} else {
					this.count.get(currentDistinct)[c] =  count;
				}
			} else {
				if (count>0) {
					this.count.get(currentDistinct)[this.nextcount.getInt(currentDistinct)] =  count;
					this.nonzeros.get(currentDistinct)[this.nextcount.getInt(currentDistinct)] =  condition;
					this.nextcount.increment(currentDistinct);
				}
			}
		} else {
			this.count.get(currentDistinct)[condition]=count;
		}
		return this;
	}
	
	public AlignedReadsDataFactory incrementCount(int condition, int count) {
		if (count==0) return this;
		
		checkDistinct();
		if (isSparse()) {
			int c = Arrays.binarySearch(nonzeros.get(currentDistinct),0,nextcount.getInt(currentDistinct),condition);
			if (c>=0) {
				this.count.get(currentDistinct)[c] +=  count;
			} else {
				this.count.get(currentDistinct)[this.nextcount.getInt(currentDistinct)] +=  count;
				this.nonzeros.get(currentDistinct)[this.nextcount.getInt(currentDistinct)] =  condition;
				this.nextcount.increment(currentDistinct);
			}
		} else {
			this.count.get(currentDistinct)[condition]+=count;
		}
		return this;
	}
	
	
	public AlignedReadsDataFactory incrementCount(NumericArray a) {
		checkDistinct();
		for (int i=0; i<getNumConditions(); i++)
			incrementCount(i,a.getInt(i));
		return this;
	}
	
	public AlignedReadsDataFactory addBarcode(int condition, String barcode) {
		checkDistinct();
		if (isSparse()) {
			int c = Arrays.binarySearch(nonzeros.get(currentDistinct),0,nextcount.getInt(currentDistinct),condition);
			if (c<0) throw new RuntimeException("Could not find index!");
			condition = c;
		}			
		if (this.barcodes.get(currentDistinct)[condition]==null)
			this.barcodes.get(currentDistinct)[condition]=new ArrayList<>();
		this.barcodes.get(currentDistinct)[condition].add(new DnaSequence(barcode));
		return this;
	}
	
	public AlignedReadsDataFactory setBarcodes(ArrayList<DnaSequence>[] barcodes) {
		checkDistinct();
		this.barcodes.set(currentDistinct,barcodes);
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
	public AlignedReadsDataFactory incrementWeight(int distinct,float weight) {
		checkDistinct(distinct);
		weights.set(distinct, weights.getDouble(distinct)+weight);
		return this;
	}
	
	public AlignedReadsDataFactory setGeometry(int distinct,int before, int overlap, int after) {
		checkDistinct(distinct);
		geometry.set(distinct, DefaultAlignedReadsData.encodeGeometry(before, overlap, after));
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
		if (isSparse()) {
			for (int i=0; i<count.length; i++)
				if (count[i]>0)
					setCount(distinct,i,count[i]);
		} 
		else 
			System.arraycopy(count, 0, this.count.get(distinct), 0, conditions);
		return this;
	}
	
	public AlignedReadsDataFactory setCount(int distinct,int condition, int count) {
		checkDistinct(distinct);
		
		
		if (isSparse()) {
			
			int c = Arrays.binarySearch(nonzeros.get(distinct),0,nextcount.getInt(distinct),condition);
			if (c>=0) {
				if (count==0) {
					this.nextcount.decrement(distinct);
					this.count.get(distinct)[c] = this.count.get(distinct)[this.nextcount.getInt(distinct)];
					this.nonzeros.get(distinct)[c] = this.nonzeros.get(distinct)[this.nextcount.getInt(distinct)];
				} else {
					this.count.get(distinct)[c] =  count;
				}
			} else {
				if (count>0) {
					this.count.get(distinct)[this.nextcount.getInt(distinct)] =  count;
					this.nonzeros.get(distinct)[this.nextcount.getInt(distinct)] =  condition;
					this.nextcount.increment(distinct);
				}
			}
			
		} else {
			this.count.get(distinct)[condition]=count;
		}
		return this;
	}
	
	public AlignedReadsDataFactory incrementCount(int distinct,int condition, int count) {
		if (count==0) return this;
		
		checkDistinct(distinct);
		if (isSparse()) {
			int c = Arrays.binarySearch(nonzeros.get(distinct),0,nextcount.getInt(distinct),condition);
			if (c>=0) {
				this.count.get(distinct)[c] +=  count;
			} else {
				this.count.get(distinct)[this.nextcount.getInt(distinct)] +=  count;
				this.nonzeros.get(distinct)[this.nextcount.getInt(distinct)] =  condition;
				this.nextcount.increment(distinct);
			}
		} else {
			this.count.get(distinct)[condition]+=count;
		}
		return this;
	}
	
	public AlignedReadsDataFactory addBarcode(int distinct, int condition, String barcode) {
		checkDistinct(distinct);
		if (isSparse()) {
			int c = Arrays.binarySearch(nonzeros.get(currentDistinct),0,nextcount.getInt(currentDistinct),condition);
			if (c<0) throw new RuntimeException("Could not find index!");
			condition = c;
		}		
		
		if (this.barcodes.get(distinct)[condition]==null)
			this.barcodes.get(distinct)[condition]=new ArrayList<>();
		this.barcodes.get(distinct)[condition].add(new DnaSequence(barcode));
		return this;
	}

	
	public AlignedReadsDataFactory addMismatch(int pos, char genomic, char read, boolean isFromSecondRead) {
		getCurrentVariationBuffer().add(createMismatch(pos, genomic, read,isFromSecondRead));
		return this;
	}
	
	public AlignedReadsDataFactory addSoftclip(boolean p5, CharSequence read, boolean isFromSecondRead) {
		getCurrentVariationBuffer().add(createSoftclip(p5, read,isFromSecondRead));
		return this;
	}
	
	public AlignedReadsDataFactory addDeletion(int pos, CharSequence genomic, boolean isFromSecondRead) {
		getCurrentVariationBuffer().add(createDeletion(pos, genomic,isFromSecondRead));
		return this;
	}
	
	public AlignedReadsDataFactory addInsertion(int pos, CharSequence read, boolean isFromSecondRead) {
		getCurrentVariationBuffer().add(createInsertion(pos, read,isFromSecondRead));
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
		
		boolean second = rest.endsWith("r");
		if (second)
			rest = rest.substring(0,rest.length()-1);
		
		if (variationString.startsWith("M"))
			return createMismatch(pos, rest.charAt(0), rest.charAt(1),second);
		
		if (variationString.startsWith("I"))
			return createInsertion(pos, rest,second);
		
		if (variationString.startsWith("D"))
			return createDeletion(pos, rest,second);
		
		throw new IllegalArgumentException("Must start with M/I/D: "+variationString);
	}
	
	public void addVariation(AlignedReadsVariation vari) {
		if (vari.isDeletion())
			addDeletion(vari.getPosition(), vari.getReferenceSequence(),vari.isFromSecondRead());
		else if (vari.isInsertion())
			addInsertion(vari.getPosition(), vari.getReadSequence(),vari.isFromSecondRead());
		else if (vari.isMismatch())
			addMismatch(vari.getPosition(), vari.getReferenceSequence().charAt(0),vari.getReadSequence().charAt(0),vari.isFromSecondRead());
		else if (vari.isSoftclip())
			addSoftclip(vari.getPosition()==0, vari.getReadSequence(),vari.isFromSecondRead());
	}

	public VarIndel createSoftclip(boolean p5, CharSequence read, boolean onSecondRead) {
		checkDistinct();
//		checkPos(pos);
		VarIndel v = new VarIndel();
		v.var = DefaultAlignedReadsData.encodeSoftclip(p5, read,onSecondRead);
		v.indel = DefaultAlignedReadsData.encodeSoftclipSequence(p5, read);
		return v;
	}
	
	
	public VarIndel createMismatch(int pos, char genomic, char read, boolean onSecondRead) {
		checkDistinct();
//		checkPos(pos);
		VarIndel v = new VarIndel();
		v.var = DefaultAlignedReadsData.encodeMismatch(pos, genomic, read,onSecondRead);
		v.indel = DefaultAlignedReadsData.encodeMismatchIndel(pos, genomic, read);
		return v;
	}
	
	public VarIndel createDeletion(int pos, CharSequence genomic, boolean onSecondRead) {
		checkDistinct();
//		checkPos(pos);
		VarIndel v = new VarIndel();
		v.var = DefaultAlignedReadsData.encodeDeletion(pos, genomic,onSecondRead);
		v.indel = DefaultAlignedReadsData.encodeDeletionIndel(pos, genomic);
		return v;
	}
	
	public VarIndel createInsertion(int pos, CharSequence read, boolean onSecondRead) {
		checkDistinct();
//		checkPos(pos);
		VarIndel v = new VarIndel();
		v.var = DefaultAlignedReadsData.encodeInsertion(pos, read,onSecondRead);
		v.indel = DefaultAlignedReadsData.encodeInsertionIndel(pos, read);
		return v;
	}
	
	public static class VarIndel implements Comparable<VarIndel> {
		short var;
		CharSequence indel;
		@Override
		public int compareTo(VarIndel o) {
			int re = Integer.compare(DefaultAlignedReadsData.pos(var), DefaultAlignedReadsData.pos(o.var));
			if (re==0) re = Short.compare(var, o.var);
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
			if (DefaultAlignedReadsData.type(var)==DefaultAlignedReadsData.TYPE_MISMATCH) 
				return new AlignedReadsMismatch(DefaultAlignedReadsData.pos(var), indel.subSequence(0, 1), indel.subSequence(1, 2),DefaultAlignedReadsData.isSecondRead(var)).toString();
			if (DefaultAlignedReadsData.type(var)==DefaultAlignedReadsData.TYPE_DELETION) 
				return new AlignedReadsDeletion(DefaultAlignedReadsData.pos(var), indel,DefaultAlignedReadsData.isSecondRead(var)).toString();
			if (DefaultAlignedReadsData.type(var)==DefaultAlignedReadsData.TYPE_INSERTION) 
				return new AlignedReadsInsertion(DefaultAlignedReadsData.pos(var), indel,DefaultAlignedReadsData.isSecondRead(var)).toString();
			if (DefaultAlignedReadsData.type(var)==DefaultAlignedReadsData.TYPE_SOFTCLIP) 
				return new AlignedReadsSoftclip(DefaultAlignedReadsData.pos(var)==0, indel,DefaultAlignedReadsData.isSecondRead(var)).toString();

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
		if (isSparse()) sortCounts();
		
		HashMap<TreeSet<VarIndel>,Integer> h = new HashMap<TreeSet<VarIndel>,Integer>();
		for (int i=0; i<var.size(); i++) {
			Integer idx = h.get(var.get(i));
			if (idx==null) h.put(var.get(i), i);
			else {
				if (!isSparse()) {
					ArrayUtils.add(count.get(idx), count.get(i));
				} else {
					IntArrayList cc = new IntArrayList();
					IntArrayList cn = new IntArrayList();
					int idxi = 0;
					int ii = 0;
					while (idxi<nextcount.getInt(idx) && ii<nextcount.getInt(i)) {
						if (nonzeros.get(idx)[idxi]==nonzeros.get(i)[ii]) { 
							cn.add(nonzeros.get(idx)[idxi]);
							cc.add(count.get(idx)[idxi++]+count.get(i)[ii++]);
						}
						else if (nonzeros.get(idx)[idxi]<nonzeros.get(i)[ii]){
							cn.add(nonzeros.get(idx)[idxi]);
							cc.add(count.get(idx)[idxi++]);
						}
						else {
							cn.add(nonzeros.get(i)[ii]);
							cc.add(count.get(i)[ii++]);
						}
					}
					while (idxi<nextcount.getInt(idx)) {
						cn.add(nonzeros.get(idx)[idxi]);
						cc.add(count.get(idx)[idxi++]);
					}
					while (ii<nextcount.getInt(i)) {
						cn.add(nonzeros.get(i)[ii]);
						cc.add(count.get(i)[ii++]);
					}
					count.set(idx,cc.toIntArray());
					nonzeros.set(idx, cn.toIntArray());
					nextcount.set(idx,cc.size());
				}
				if (ids.size()>0)
					ids.set(idx, Math.min(ids.getInt(idx),ids.getInt(i)));
			}
		}
		
		
		
		BitVector keep = new BitVector(var.size());
		for (Integer i : h.values()) 
			keep.putQuick(i, true);
		
		count = restrict(count,keep);
		if (isSparse()) {
			nonzeros = restrict(nonzeros,keep);
			nextcount = restrict(nextcount,keep);
		}
		var = restrict(var,keep);
		multiplicity = restrict(multiplicity,keep);
		weights = restrict(weights,keep);
		geometry = restrict(geometry,keep);
		ids = restrict(ids,keep);
		barcodes = restrict(barcodes,keep);
		
		currentDistinct = count.size()-1;
	}
	
	
	private void sortCounts() {
		for (int d=0; d<count.size(); d++) {
			int[] c = count.get(d);
			int[] n = nonzeros.get(d);
			ArrayUtils.parallelSort(n, c, 0, nextcount.getInt(d));
			count.set(d, c);
			nonzeros.set(d, n);
		}
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
	
	public static DefaultAlignedReadsData createSimple(int[] count) {
		DefaultAlignedReadsData re = new DefaultAlignedReadsData();
		re.conditions = 1;
		re.count = new int[][] {count};
		re.var = new short[1][0];
		re.indels = new CharSequence[1][0];
		re.multiplicity = new int[] {1};
		return re;
	}

	private DefaultAlignedReadsData fill(DefaultAlignedReadsData re) {
		re.conditions = getNumConditions();
		if (isSparse())
			sortCounts();
		re.count = convInt(count,nextcount);
		if (isSparse()) 
			re.nonzeros = convInt(nonzeros,nextcount);
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
		
		if (geometry.size()==re.count.length)
			re.geometry = geometry.toIntArray();
		else if (geometry.size()>0)
			throw new RuntimeException("Call setGeometry for each distinct sequence or for none!");
		
		return re;
	}

	public DefaultAlignedReadsData create() {
		return fill(new DefaultAlignedReadsData());
	}
	
	public BarcodedAlignedReadsData createBarcode() {
		return createBarcode(new BarcodedAlignedReadsData());
	}
	public BarcodedAlignedReadsData createBarcode(BarcodedAlignedReadsData re) {
		fill(re);
		
		if (isSparse()) {
			re.barcodes  = new DnaSequence[currentDistinct+1][][];
			for (int d=0; d<re.barcodes.length; d++){
				re.barcodes[d] = new DnaSequence[re.nonzeros[d].length][];
				for (int c=0; c<re.getNonzeroCountIndicesForDistinct(d).length; c++) {
					re.barcodes[d][c] = barcodes.get(d)[c].toArray(new DnaSequence[0]);
					if (re.barcodes[d][c].length!=re.count[d][c])
						throw new RuntimeException("Barcodes and counts do not match!");
				}
			}
		} else {
			re.barcodes  = new DnaSequence[currentDistinct+1][conditions][];
			for (int d=0; d<re.barcodes.length; d++){
				for (int c=0; c<re.barcodes[d].length; c++) {
					re.barcodes[d][c] = barcodes.get(d)[c]==null?new DnaSequence[0]:barcodes.get(d)[c].toArray(new DnaSequence[0]);
					if (re.barcodes[d][c].length!=re.count[d][c])
						throw new RuntimeException("Barcodes and counts do not match!");
				}
			}
		}
		return re;
	}
	
	public AlignedReadsData createDigital() {
		DigitalAlignedReadsData re = new DigitalAlignedReadsData();
		if (isSparse() || count.size()!=1 || var.get(0).size()>0) throw new RuntimeException();
		
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
	
	private int[][] convInt(ArrayList<int[]> a, IntArrayList nextInd) {
		int[][] re = new int[currentDistinct+1][];
		for (int i=0; i<re.length; i++) {
			if (nextInd==null)
				re[i] = a.get(i).clone();
			else {
				re[i] = new int[nextInd.getInt(i)];
				System.arraycopy(a.get(i), 0, re[i], 0, nextInd.getInt(i));
			}
		}
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

	
	public AlignedReadsDataFactory add(BarcodedAlignedReadsData ard, BarcodeMapping mapping) {
		for (int distinct=0; distinct<ard.getDistinctSequences(); distinct++) 
			add(ard,distinct,mapping);
		return this;
	}
	
	public boolean isSparse() {
		return nonzeros!=null;
	}
	
	public AlignedReadsDataFactory add(BarcodedAlignedReadsData ard, int distinct, BarcodeMapping mapping) {
		
		if (!isSparse()) throw new RuntimeException("Can only run in sparse mode!");
		
		IntArrayList collect = new IntArrayList();
		for (int i=0; i<ard.getNumConditions(); i++) {
			for (DnaSequence b : ard.getBarcodes(distinct, i)) {
				String str = b.toString();
				String cell = str.substring(0,mapping.getPrefixLength());
				int index = mapping.getIndex(i, cell);
				if (index!=-1) {
					collect.add(index);
				}
			}
		}
		if (collect.isEmpty()) return this;
		
		newDistinctSequence();
		collect.sort();
		int s = 0;
		for (int i=0; i<collect.size(); i++) {
			if (collect.getInt(s)!=collect.getInt(i)) {
				setCount(collect.getInt(s),i-s);
				s = i;
			}
		}
		setCount(collect.getInt(s),collect.size()-s);
		
		for (int i=0; i<ard.getNumConditions(); i++) {
			for (DnaSequence b : ard.getBarcodes(distinct, i)) {
				String str = b.toString();
				String cell = str.substring(0,mapping.getPrefixLength());
				int index = mapping.getIndex(i, cell);
				if (index!=-1) {
					addBarcode(index, str.substring(mapping.getPrefixLength()));
				}
			}
		}
		
		setMultiplicity(ard.getMultiplicity(distinct));
		for (int i=0; i<ard.getVariationCount(distinct); i++)
			addVariation(ard.getVariation(distinct, i));
		if (ard.hasId())
			setId(ard.getId(distinct));
		if (ard.hasWeights())
			setWeight(ard.getWeight(distinct));
		if (ard.hasGeometry())
			setGeometry(ard.getGeometryBeforeOverlap(distinct), ard.getGeometryOverlap(distinct), ard.getGeometryAfterOverlap(distinct));
		
		
		return this;
	}
	
	
	public AlignedReadsDataFactory add(AlignedReadsData ard) {
		for (int d=0; d<ard.getDistinctSequences(); d++)
			add(ard,d);
		return this;
	}
	
	/**
	 * 
	 * @param ard
	 * @param distinct take the distinct from ard!
	 */
	public AlignedReadsDataFactory add(AlignedReadsData ard, int distinct) {
		newDistinctSequence();
		
		if (ard.hasNonzeroInformation()) {
			for (int i:ard.getNonzeroCountIndicesForDistinct(distinct))
				setCount(i, ard.getCount(distinct, i));
		}
		else {
			for (int i=0; i<ard.getNumConditions(); i++)
				if (ard.getCount(distinct, i)>0)
					setCount(i, ard.getCount(distinct, i));
		}
		setMultiplicity(ard.getMultiplicity(distinct));
		for (int i=0; i<ard.getVariationCount(distinct); i++)
			addVariation(ard.getVariation(distinct, i));
		if (ard.hasId())
			setId(ard.getId(distinct));
		if (ard.hasWeights())
			setWeight(ard.getWeight(distinct));
		if (ard.hasGeometry())
			setGeometry(ard.getGeometryBeforeOverlap(distinct), ard.getGeometryOverlap(distinct), ard.getGeometryAfterOverlap(distinct));
		if (ard instanceof BarcodedAlignedReadsData) {
			BarcodedAlignedReadsData bc = (BarcodedAlignedReadsData)ard;
			for (int i=0; i<ard.getNumConditions(); i++)
				for (int b=0; b<ard.getCount(distinct, i); b++)
					addBarcode(i, bc.getBarcodes(distinct, i)[b].toString());
		}
		return this;
	}
	
	/**
	 * 
	 * @param ard
	 * @param distinct take the distinct from ard!
	 */
	public AlignedReadsDataFactory add(AlignedReadsData ard, int distinct, UnaryOperator<AlignedReadsVariation> varPred) {
		newDistinctSequence();
		if (ard.hasNonzeroInformation()) {
			for (int i:ard.getNonzeroCountIndicesForDistinct(distinct))
				setCount(i, ard.getCount(distinct, i));
		}
		else {
			for (int i=0; i<ard.getNumConditions(); i++)
				if (ard.getCount(distinct, i)>0)
					setCount(i, ard.getCount(distinct, i));
		}
		setMultiplicity(ard.getMultiplicity(distinct));
		for (int i=0; i<ard.getVariationCount(distinct); i++) {
			AlignedReadsVariation vari = ard.getVariation(distinct, i);
			vari = varPred.apply(vari);
			if (vari!=null)
				addVariation(vari);
		}
		if (ard.hasId())
			setId(ard.getId(distinct));
		if (ard.hasWeights())
			setWeight(ard.getWeight(distinct));
		if (ard.hasGeometry())
			setGeometry(ard.getGeometryBeforeOverlap(distinct), ard.getGeometryOverlap(distinct), ard.getGeometryAfterOverlap(distinct));
		if (ard instanceof BarcodedAlignedReadsData) {
			BarcodedAlignedReadsData bc = (BarcodedAlignedReadsData)ard;
			for (int i=0; i<ard.getNumConditions(); i++)
				for (int b=0; b<ard.getCount(distinct, i); b++)
					addBarcode(i, bc.getBarcodes(distinct, i)[b].toString());
		}
		return this;
	}

	public void incrementCount(AlignedReadsData ard) {
		for (int i=0; i<ard.getNumConditions(); i++)
			incrementCount(i, ard.getTotalCountForConditionInt(i,ReadCountMode.All));
	}

	

	
	
	
}
