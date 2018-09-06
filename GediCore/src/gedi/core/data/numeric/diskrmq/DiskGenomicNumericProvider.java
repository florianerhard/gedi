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
package gedi.core.data.numeric.diskrmq;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;

import gedi.core.data.numeric.GenomicNumericProvider;
import gedi.core.data.numeric.GenomicNumericProvider.PositionNumericIterator;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.util.algorithm.rmq.DiskMinMaxSumIndex;
import gedi.util.algorithm.rmq.DoubleDiskSuccinctRmaxq;
import gedi.util.datastructure.array.DiskIntegerArray;
import gedi.util.datastructure.array.IntegerArray;
import gedi.util.datastructure.array.MemoryIntegerArray;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.computed.ComputedIntegerArray;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.randomaccess.PageFileView;
import gedi.util.io.randomaccess.diskarray.IntDiskArray;

public class DiskGenomicNumericProvider implements GenomicNumericProvider, AutoCloseable {

	private HashMap<ReferenceSequence,IntegerArray> positions;
	private HashMap<ReferenceSequence,DiskMinMaxSumIndex[]> rmqs;
	private PageFile file;
	private int rows = -1;
	
	private boolean coverageMode = false;
	private boolean dense = false;
	
	
	public DiskGenomicNumericProvider(String file) throws IOException {
		positions = new HashMap<ReferenceSequence, IntegerArray>();
		rmqs = new HashMap<ReferenceSequence, DiskMinMaxSumIndex[]>();
		
		this.file = new PageFile(file);
		if (!this.file.getAsciiChars(DiskGenomicNumericBuilder.MAGIC.length()).equals(DiskGenomicNumericBuilder.MAGIC))
			throw new RuntimeException("Not a valid file!");
		
		int refs = this.file.getInt();
		for (int i=0; i<refs; i++) {
			Chromosome chr = Chromosome.read(this.file);
			long pos = this.file.getLong();
			long cur = this.file.position();
			
			this.file.position(pos);
			char type = this.file.getAsciiChar();
			if (type!='S' && type!='C' && type!='W' && type!='D') throw new RuntimeException("Not a valid file!");
			coverageMode = type=='C' || type=='W';
			dense = type=='W' || type=='D';
			
			int size = this.file.getInt();
			int numCond = this.file.getInt();
			
			if (rows==-1) rows = numCond;
			else if(rows!=numCond) throw new RuntimeException("Inconsistent number of conditions!");
			
			if (!dense) {
				PageFileView view = new PageFileView(this.file, this.file.position(), this.file.position()+size*Integer.BYTES);
				DiskIntegerArray ida = new DiskIntegerArray();
				ida.deserialize(view, size);
				positions.put(chr, ida);
				this.file.position(view.getEnd());
			} else {
				positions.put(chr, new ComputedIntegerArray(n->n,size));
			}
			
			long rmqPos = this.file.position();
			DiskMinMaxSumIndex[] ind = new DiskMinMaxSumIndex[numCond];
			for (int j=0; j<numCond; j++) {
				PageFileView pfv = new PageFileView(this.file,rmqPos,this.file.size());
				ind[j] = new DiskMinMaxSumIndex(pfv);
				rmqPos = pfv.position()+pfv.getStart();
			}
			rmqs.put(chr, ind);
			
			
			this.file.position(cur);
			
//			if (chr.getName().equals())
			
		}
		
	}
	
	public boolean hasSum() {
		return rmqs.values().iterator().next()[0].hasSum();
	}
	
	@Override
	public int getLength(String name) {
		Chromosome reference = Chromosome.obtain(name);
		IntegerArray dia = positions.get(reference);
		if (dia==null) {
			// test all three strands
			dia = positions.get(reference.toStrandIndependent());
			if (dia==null)
				dia = positions.get(reference.toPlusStrand());
			if (dia==null)
				dia = positions.get(reference.toMinusStrand());
			if (dia==null)
				return -1;
		}
		return -dia.getInt(dia.length()-1);
	}
	
	public Collection<ReferenceSequence> getRefereneSequences() {
		return positions.keySet();
	}
	
	/**
	 * Dumps the content of the disk arrays (positions and values) from the specified region.
	 * @param reference
	 * @param region
	 * @param w
	 */
	public void dump(ReferenceSequence reference, Consumer<String> output) {
		dump(reference, null, output);
	}
	public void dump(ReferenceSequence reference, GenomicRegion region, Consumer<String> output) {
		if (!positions.containsKey(reference)) 
			return;
		
		IntegerArray arr = positions.get(reference);
		DiskMinMaxSumIndex[] vals = rmqs.get(reference);
		
		if (region!=null) {
			for (int p=0; p<region.getNumParts(); p++) {
				int idx1 = positions.get(reference).binarySearch(region.getStart(p));
				int idx2 = positions.get(reference).binarySearch(region.getStop(p));
				if (idx1==idx2 && idx1<0) continue;
				if (idx1<0) idx1 = -idx1-1;
				if (idx2<0) idx2 = -idx2-2;
				idx2++;
				
				dump(arr,vals,idx1,idx2,output);
				
			}
		} else {
			dump(arr,vals,0,arr.length(),output);
		}
		
	}

	private void dump(IntegerArray arr, DiskMinMaxSumIndex[] vals,
			int start, int end, Consumer<String> a) {
		for (int i=start; i<end; i++) {
			a.accept(arr.format(i));
			for (int v=0;v<vals.length; v++) {
				a.accept("\t");
				a.accept(vals[v].format(i));
			}
			a.accept("\n");
		}
	}

	public PositionNumericIterator oldCoverageIterator(ReferenceSequence reference, GenomicRegion region) {
		return new PositionNumericIterator() {
			
			private int p = 0;
			
			@Override
			public boolean hasNext() {
				return p<region.getTotalLength();
			}
			
			@Override
			public int nextInt() {
				return region.map(p++);
			}
			
			@Override
			public double getValue(int row) {
				return DiskGenomicNumericProvider.this.getValue(reference, region.map(p-1), row);
			}
			
			@Override
			public double[] getValues(double[] re) {
				if (re==null || re.length!=getNumDataRows()) 
					re = new double[getNumDataRows()];
				for (int row=0; row<re.length; row++)
					re[row] = getValue(row);
				return re;
			}
		};
	}
	
	public PositionNumericIterator iterateValues(ReferenceSequence reference) {
		IntegerArray a = positions.get(reference);
		int l = a.getInt(a.length()-1)+1;
		return iterateValues(reference, new ArrayGenomicRegion(0,l));
	}
	@Override
	public PositionNumericIterator iterateValues(ReferenceSequence reference,
			GenomicRegion region) {
		
		if (coverageMode) {
			IntegerArray ind = positions.get(reference);

			IntArrayList idxs = new IntArrayList();
			for (int p=0; p<region.getNumParts(); p++) {
				int idx1 = ind.binarySearch(region.getStart(p));
				int idx2 = ind.binarySearch(region.getEnd(p));
				
				if (idx1<0) idx1 = -idx1-2;
				if (idx2<0) idx2 = -idx2-1;
				
				idxs.add(idx1);
				idxs.add(idx2);
			}
			
			ArrayGenomicRegion idxRegion = new ArrayGenomicRegion(idxs);
			
			rmqs.get(reference);
			double[] currValue = new double[getNumDataRows()];
			for (int i=0; i<currValue.length; i++)
				currValue[i] = rmqs.get(reference)[i].getValue(idxRegion.getStart());
			
			if(idxRegion.isEmpty()) return new PositionNumericIterator() {
				@Override
				public int nextInt() {
					return 0;
				}

				@Override
				public boolean hasNext() {
					return false;
				}

				@Override
				public double[] getValues(double[] re) {
					return null;
				}

				@Override
				public double getValue(int row) {
					return 0;
				}
				
			};
			
			return new PositionNumericIterator() {
				int p = 0;
				int currIdx = 0;
				int nextPos = currIdx+1>=idxRegion.getTotalLength()?Integer.MAX_VALUE:ind.getInt(idxRegion.map(currIdx+1));
				
				@Override
				public boolean hasNext() {
					return p<region.getTotalLength();
				}
				
				@Override
				public int nextInt() {
					int re = region.map(p++);
					if (re==nextPos) {
						int pp=idxRegion.map(++currIdx);
						nextPos = currIdx+1>=idxRegion.getTotalLength()?Integer.MAX_VALUE:ind.getInt(idxRegion.map(currIdx+1));
						for (int i=0; i<currValue.length; i++)
							currValue[i] = rmqs.get(reference)[i].getValue(pp);
					}
					
					return re;
				}
				
				@Override
				public double getValue(int row) {
					return currValue[row];
				}
				
				@Override
				public double[] getValues(double[] re) {
					if (re==null || re.length!=getNumDataRows()) 
						re = new double[getNumDataRows()];
					System.arraycopy(currValue, 0, re, 0, re.length);
					return re;
				}
				
			};
			
		}
		
		IntegerArray ind = positions.get(reference);

		IntArrayList idxs = new IntArrayList();
		for (int p=0; p<region.getNumParts(); p++) {
			int idx1 = ind.binarySearch(region.getStart(p));
			int idx2 = ind.binarySearch(region.getEnd(p));
			
			if (idx1==idx2 && idx1<0) continue;
			if (idx1<0) idx1 = -idx1-1;
			if (idx2<0) idx2 = -idx2-1;
			
			idxs.add(idx1);
			idxs.add(idx2);
		}
		
		ArrayGenomicRegion idxRegion = new ArrayGenomicRegion(idxs);
		
		return new PositionNumericIterator() {
			int p = 0;
			
			
			@Override
			public boolean hasNext() {
				return p<idxRegion.getTotalLength();
			}
			
			@Override
			public int nextInt() {
				return positions.get(reference).getInt(idxRegion.map(p++));
			}
			
			@Override
			public double getValue(int row) {
				return rmqs.get(reference)[row].getValue(idxRegion.map(p-1));
			}
			
			@Override
			public double[] getValues(double[] re) {
				DiskMinMaxSumIndex[] r = rmqs.get(reference);
				if (re==null || re.length!=getNumDataRows()) 
					re = new double[getNumDataRows()];
				for (int row=0; row<re.length; row++)
					re[row]=getValue(row);
				return re;
			}
			
		};
	}

	@Override
	public void close() throws Exception {
		file.close();
	}

	@Override
	public int getNumDataRows() {
		return rows;
	}

	@Override
	public double getValue(ReferenceSequence reference, int pos, int row) {
		IntegerArray ind = positions.get(reference);
		if (ind==null){
			reference = reference.toStrandIndependent();
			ind = positions.get(reference);
		}
		if (ind==null) return Double.NaN;
		
		int idx = ind.binarySearch(pos);
		idx = adaptIdx(idx);
		if (idx<0 || idx>=ind.length()) return 0;
		
		return rmqs.get(reference)[row].getValue(idx);
	}

	public double[] getValues(ReferenceSequence reference, int pos, double[] re) {
		if (re==null || re.length!=rows) re = new double[rows];
		
		IntegerArray ind = positions.get(reference);
		if (ind==null){
			reference = reference.toStrandIndependent();
			ind = positions.get(reference);
		}
		if (ind==null) {
			Arrays.fill(re,Double.NaN);
			return re;
		}
		
		int idx = ind.binarySearch(pos);
		idx = adaptIdx(idx);
		if (idx<0 || idx>=ind.length()) {
			Arrays.fill(re,0);
			return re;
		}
		DiskMinMaxSumIndex[] rm = rmqs.get(reference);
		for (int i=0; i<rows; i++)
			re[i] = rm[i].getValue(idx);
		return re;
	}
	
	private int adaptIdx(int idx) {
		if (coverageMode && idx<0) idx=-idx-2;
		return idx;
	}

	@Override
	public double getMax(ReferenceSequence reference, GenomicRegion region, int row) {
		if (!positions.containsKey(reference)) 
			return Double.NaN;
		try {
			double re = Double.NEGATIVE_INFINITY;
			for (int p=0; p<region.getNumParts(); p++) {
				int idx1 = positions.get(reference).binarySearch(region.getStart(p));
				int idx2 = positions.get(reference).binarySearch(region.getStop(p));
				
				idx1=adaptIdx(idx1);
				idx2=adaptIdx(idx2);
				
				if (idx1==idx2 && idx1<0) continue;
				if (idx1<0) idx1 = -idx1-1;
				if (idx2<0) idx2 = -idx2-2;
				DiskMinMaxSumIndex ind = rmqs.get(reference)[row];
				if (idx1<ind.length()) {
					idx2 = Math.min(idx2,ind.length()-1);
					re = Math.max(re,ind.getValue(ind.getMaxIndex(idx1, idx2)));
				}
			}
			return Double.isInfinite(re)?Double.NaN:re; 
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public double getMin(ReferenceSequence reference, GenomicRegion region, int row) {
		if (!positions.containsKey(reference)) 
			return Double.NaN;
		try {
			double re = Double.POSITIVE_INFINITY;
			
			for (int p=0; p<region.getNumParts(); p++) {
				int idx1 = positions.get(reference).binarySearch(region.getStart(p));
				int idx2 = positions.get(reference).binarySearch(region.getStop(p));
				
				idx1=adaptIdx(idx1);
				idx2=adaptIdx(idx2);
				
				if (idx1==idx2 && idx1<0) continue;
				if (idx1<0) idx1 = -idx1-1;
				if (idx2<0) idx2 = -idx2-2;
				DiskMinMaxSumIndex ind = rmqs.get(reference)[row];
				
				if (idx1<ind.length()) {
					idx2 = Math.min(idx2,ind.length()-1);
					re = Math.min(re,ind.getValue(ind.getMinIndex(idx1, idx2)));
				}
			}
			return Double.isInfinite(re)?Double.NaN:re; 
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public double getSum(ReferenceSequence reference, GenomicRegion region, int row) {
		if (!positions.containsKey(reference)) 
			return Double.NaN;
		try {
			double re = 0;
			
			for (int p=0; p<region.getNumParts(); p++) {
				int idx1 = positions.get(reference).binarySearch(region.getStart(p));
				int idx2 = positions.get(reference).binarySearch(region.getStop(p));
				
				idx1=adaptIdx(idx1);
				idx2=adaptIdx(idx2);
				
				if (idx1==idx2 && idx1<0) continue;
				if (idx1<0) idx1 = -idx1-1;
				if (idx2<0) idx2 = -idx2-2;
				DiskMinMaxSumIndex ind = rmqs.get(reference)[row];
				
				if (idx1<ind.length()) {
					idx2 = Math.min(idx2,ind.length()-1);
					re += ind.getSum(idx1, idx2);
				}
			}
			return re; 
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public double getMean(ReferenceSequence reference, GenomicRegion region, int row) {
		return getSum(reference, region, row)/region.getTotalLength();
	}
	
	@Override
	public double getAvailableMean(ReferenceSequence reference,
			GenomicRegion region, int row) {
		if (!positions.containsKey(reference)) 
			return Double.NaN;
		try {
			double re = 0;
			int n = 0;
			for (int p=0; p<region.getNumParts(); p++) {
				int idx1 = positions.get(reference).binarySearch(region.getStart(p));
				int idx2 = positions.get(reference).binarySearch(region.getStop(p));
				
				idx1=adaptIdx(idx1);
				idx2=adaptIdx(idx2);
				
				if (idx1==idx2 && idx1<0) continue;
				if (idx1<0) idx1 = -idx1-1;
				if (idx2<0) idx2 = -idx2-2;
				DiskMinMaxSumIndex ind = rmqs.get(reference)[row];
				
				if (idx1<ind.length()) {
					idx2 = Math.min(idx2,ind.length()-1);
					re += ind.getSum(idx1, idx2);
					n+=idx2-idx1+1;
				}
			}
			return re/n; 
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
}
 