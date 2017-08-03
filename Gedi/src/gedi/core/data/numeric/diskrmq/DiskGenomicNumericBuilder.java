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

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.FunctorUtils;
import gedi.util.FunctorUtils.MergeIterator;
import gedi.util.ReflectionUtils;
import gedi.util.algorithm.rmq.DiskMinMaxSumIndex;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class DiskGenomicNumericBuilder {

	public static final String MAGIC = "DGN";
	
	private String file;
	private PageFileWriter writer;
	
	private int numCond = -1;
	private Class<? extends Number> cls;
	
	private HashMap<ReferenceSequence,WriterInfo> perCh = new HashMap<ReferenceSequence, WriterInfo>();

	private boolean indexSum; 
	
	public DiskGenomicNumericBuilder(String file) throws IOException {
		this(file,true);
	}
	
	public DiskGenomicNumericBuilder(String file, boolean indexSum) throws IOException {
		this.file = file;
		this.writer = new PageFileWriter(file);
		this.indexSum = indexSum;
	}
	
	public void addValue(ReferenceSequence reference, int position, NumericArray value) throws IOException {
		check(value.length(),value.getType().getType());
		getInfo(reference).addValue(position,value);
	}

	public void addValue(ReferenceSequence reference, int position, double[] value) throws IOException {
		check(value.length,Double.TYPE);
		getInfo(reference).addValue(position,value);
	}
	public void addValue(ReferenceSequence reference, int position, double value) throws IOException {
		check(1,Double.TYPE);
		getInfo(reference).addValue(position,value);
	}
	
	public void addValue(ReferenceSequence reference, int position, float[] value) throws IOException {
		check(value.length,Float.TYPE);
		getInfo(reference).addValue(position,value);
	}
	public void addValue(ReferenceSequence reference, int position, float value) throws IOException {
		check(1,Float.TYPE);
		getInfo(reference).addValue(position,value);
	}
	
	public void addValue(ReferenceSequence reference, int position, Number value) throws IOException {
		check(1,ReflectionUtils.toPrimitveClass(value.getClass()));
		getInfo(reference).addValue(position,value);
	}
	
	public void addValue(ReferenceSequence reference, int position, int[] value) throws IOException {
		check(value.length,Integer.TYPE);
		getInfo(reference).addValue(position,value);
	}
	public void addValue(ReferenceSequence reference, int position, int value) throws IOException {
		check(1,Integer.TYPE);
		getInfo(reference).addValue(position,value);
	}
	
	public void addCoverage(ReferenceGenomicRegion<? extends AlignedReadsData> rgr) throws IOException {
		addCoverage(rgr.getReference(),rgr.getRegion(),rgr.getData());
	}
	
	public void addCoverage(ReferenceSequence reference, GenomicRegion region, AlignedReadsData read) throws IOException {
		check(read.getNumConditions(),Integer.TYPE);
		WriterInfo inf = getInfo(reference);
		int[] count = read.getTotalCountsForConditionsInt(ReadCountMode.All);
		int[] negCount = count.clone();
		for (int i=0; i<negCount.length; i++)
			negCount[i]*=-1;
		
		for (int p=0; p<region.getNumParts(); p++) {
			inf.addValue(region.getStart(p), count);
			inf.addValue(region.getEnd(p), negCount);
		}
	}
	
	
	public void addCoverage(ReferenceGenomicRegion<? extends AlignedReadsData> rgr, ReadCountMode mode) throws IOException {
		addCoverage(rgr.getReference(),rgr.getRegion(),rgr.getData(), mode);
	}
	
	/**
	 * Not tested after ReadCountMode
	 * @param reference
	 * @param region
	 * @param read
	 * @param mode
	 * @throws IOException
	 */
	public void addCoverage(ReferenceSequence reference, GenomicRegion region, AlignedReadsData read, ReadCountMode mode) throws IOException {
		check(read.getNumConditions(),Integer.TYPE);
		WriterInfo inf = getInfo(reference);
		NumericArray count = read.getTotalCountsForConditions(null,mode);
		NumericArray negCount = count.copy();
		for (int i=0; i<negCount.length(); i++)
			negCount.applyInPlace(d->-d);
		
		for (int p=0; p<region.getNumParts(); p++) {
			inf.addValue(region.getStart(p), count);
			inf.addValue(region.getEnd(p), negCount);
		}
	}
	
//	public void addCoverage(ReferenceGenomicRegion<? extends AlignedReadsData> rgr, ContrastMapping contrast) throws IOException {
//		addCoverage(rgr.getReference(),rgr.getRegion(),rgr.getData(),contrast);
//	}
//
//	public void addCoverage(ReferenceSequence reference, GenomicRegion region, AlignedReadsData read, ContrastMapping contrast) throws IOException {
//		check(contrast.getNumMergedConditions(),Integer.TYPE);
//		WriterInfo inf = getInfo(reference);
//		int[] count = new int[contrast.getNumMergedConditions()];
//		for (int i=0; i<read.getNumConditions(); i++)
//			if (contrast.getMappedIndex(i)!=-1)
//				count[contrast.getMappedIndex(i)] += read.getTotalCount(i);
//		
//		int[] negCount = count.clone();
//		for (int i=0; i<negCount.length; i++)
//			negCount[i]*=-1;
//		
//		for (int p=0; p<region.getNumParts(); p++) {
//			inf.addValue(region.getStart(p), count);
//			inf.addValue(region.getEnd(p), negCount);
//		}
//	}
	
	
	public void addValue(ReferenceSequence reference, int position, long[] value) throws IOException {
		check(value.length,Long.TYPE);
		getInfo(reference).addValue(position,value);
	}
	public void addValue(ReferenceSequence reference, int position, long value) throws IOException {
		check(1,Long.TYPE);
		getInfo(reference).addValue(position,value);
	}
	
	// unchecked
	public void addValueEx(ReferenceSequence reference, int position, double[] value) {
		try {
			addValue(reference,position,value);
		} catch (IOException e) {
			throw new RuntimeException("Could not add value!",e);
		}
	}
	public void addValueEx(ReferenceSequence reference, int position, double value) {
		try {
			addValue(reference,position,value);
		} catch (IOException e) {
			throw new RuntimeException("Could not add value!",e);
		}
	}
	
	public void addValueEx(ReferenceSequence reference, int position, float[] value) {
		try {
			addValue(reference,position,value);
		} catch (IOException e) {
			throw new RuntimeException("Could not add value!",e);
		}
	}
	public void addValueEx(ReferenceSequence reference, int position, float value) {
		try {
			addValue(reference,position,value);
		} catch (IOException e) {
			throw new RuntimeException("Could not add value!",e);
		}
	}
	
	public void addValueEx(ReferenceSequence reference, int position, int[] value) {
		try {
			addValue(reference,position,value);
		} catch (IOException e) {
			throw new RuntimeException("Could not add value!",e);
		}
	}
	public void addValueEx(ReferenceSequence reference, int position, int value)  {
		try {
			addValue(reference,position,value);
		} catch (IOException e) {
			throw new RuntimeException("Could not add value!",e);
		}
	}
	
	public void addCoverageEx(ReferenceGenomicRegion<? extends AlignedReadsData> rgr) {
		addCoverageEx(rgr.getReference(),rgr.getRegion(),rgr.getData());
	}
	
	public void addCoverageEx(ReferenceSequence reference, GenomicRegion region, AlignedReadsData read) {
		try {
			check(read.getNumConditions(),Integer.TYPE);
			WriterInfo inf = getInfo(reference);
			int[] count = read.getTotalCountsForConditionsInt(ReadCountMode.All);
			int[] negCount = count.clone();
			for (int i=0; i<negCount.length; i++)
				negCount[i]*=-1;
			
			for (int p=0; p<region.getNumParts(); p++) {
				inf.addValue(region.getStart(p), count);
				inf.addValue(region.getEnd(p), negCount);
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not add value!",e);
		}
	}
	
	
	
	public void addCoverageEx(ReferenceGenomicRegion<? extends AlignedReadsData> rgr, ReadCountMode mode) {
		addCoverageEx(rgr.getReference(),rgr.getRegion(),rgr.getData(),mode);
	}
	
	public void addCoverageEx(ReferenceSequence reference, GenomicRegion region, AlignedReadsData read, ReadCountMode mode) {
		try {
			check(read.getNumConditions(),mode.getType());
			WriterInfo inf = getInfo(reference);
			
			NumericArray count = read.getTotalCountsForConditions(null,mode);
			NumericArray negCount = count.copy();
			for (int i=0; i<negCount.length(); i++)
				negCount.applyInPlace(d->-d);
			
			for (int p=0; p<region.getNumParts(); p++) {
				inf.addValue(region.getStart(p), count);
				inf.addValue(region.getEnd(p), negCount);
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not add value!",e);
		}
	}
	
	
//	public void addCoverageEx(ReferenceGenomicRegion<? extends AlignedReadsData> rgr, ContrastMapping contrast) {
//		addCoverageEx(rgr.getReference(),rgr.getRegion(),rgr.getData(),contrast);
//	}
//	
//	public void addCoverageEx(ReferenceSequence reference, GenomicRegion region, AlignedReadsData read, ContrastMapping contrast) {
//		try {
//			check(contrast.getNumMergedConditions(),Integer.TYPE);
//			WriterInfo inf = getInfo(reference);
//			int[] count = new int[contrast.getNumMergedConditions()];
//			for (int i=0; i<read.getNumConditions(); i++)
//				if (contrast.getMappedIndex(i)!=-1)
//					count[contrast.getMappedIndex(i)] += read.getTotalCount(i);
//			
//			int[] negCount = count.clone();
//			for (int i=0; i<negCount.length; i++)
//				negCount[i]*=-1;
//			
//			for (int p=0; p<region.getNumParts(); p++) {
//				inf.addValue(region.getStart(p), count);
//				inf.addValue(region.getEnd(p), negCount);
//			}
//		} catch (IOException e) {
//			throw new RuntimeException("Could not add value!",e);
//		}
//	}
//	
	
	public void addValueEx(ReferenceSequence reference, int position, long[] value) {
		try {
			addValue(reference,position,value);
		} catch (IOException e) {
			throw new RuntimeException("Could not add value!",e);
		}
	}
	public void addValueEx(ReferenceSequence reference, int position, long value) {
		try {
			addValue(reference,position,value);
		} catch (IOException e) {
			throw new RuntimeException("Could not add value!",e);
		}
	}
	
	private void check(int length, Class<? extends Number> type) {
		if (cls==null) cls = type;
		else if (cls!=type) throw new RuntimeException("Do not mix types!");
		if (numCond==-1) numCond = length;
		else if (numCond!=length) throw new RuntimeException("Do not mix different conditions counts!");
	}


	private boolean referenceSorted = false;
	
	public void setReferenceSorted(boolean referenceSorted) {
		this.referenceSorted = referenceSorted;
	}

	private WriterInfo lastInfo = null;
	
	private WriterInfo getInfo(ReferenceSequence reference) throws IOException {
		WriterInfo w = perCh.get(reference);
		if (w==null) perCh.put(reference, w = new WriterInfo(file+"."+reference.toString()));
		
		if (lastInfo!=null && lastInfo!=w && referenceSorted)
			lastInfo.close();
		lastInfo = w;
		return w;
	}
	

	public void build() throws IOException {
		build(false,false);
	}
	public void build(boolean coverageMode) throws IOException {
		build(coverageMode,false);
	}
	public void build(boolean coverageMode, boolean dense) throws IOException {
		// build header: magic+list of chromosome and positions
		writer.putAsciiChars(MAGIC);
		ReferenceSequence[] refs = perCh.keySet().toArray(new ReferenceSequence[0]);
		Arrays.sort(refs);
		writer.putInt(refs.length);
		HashMap<ReferenceSequence,Long> pos = new HashMap<ReferenceSequence, Long>();
		for (ReferenceSequence r : refs) {
			Chromosome.write(Chromosome.obtain(r.getName(),r.getStrand()), writer);
			pos.put(r, writer.position());
			writer.putLong(0); // filled in in the end!
		}
		
		for (ReferenceSequence r : refs) {
			long curpos = writer.position();
			writer.position(pos.get(r));
			writer.putLong(curpos);
			writer.position(curpos);
			
			perCh.get(r).write(writer, coverageMode, dense);
		}
		
		writer.close();
		
	}
	
	
	private class WriterInfo {
		PageFileWriter writer;
		int lastPos;
		boolean sorted = true;
		int size = 0;
		int maxPos = 0;
		
		long[] maxValue = new long[numCond];
		long[] minValue = new long[numCond];
		
		WriterInfo(String file) throws IOException {
			writer = new PageFileWriter(file);
		}


		public void addValue(int position, NumericArray value) throws IOException {
			writer.putInt(position);
			for (int i=0; i<value.length(); i++)
				value.serializeElement(i, writer);
			if (position<=lastPos) 
				sorted = false;
			lastPos = position;
			size++;
			maxPos = Math.max(maxPos, position);
		}
		
		public void addValue(int position, double[] value) throws IOException {
			writer.putInt(position);
			for (int i=0; i<value.length; i++)
				writer.putDouble(value[i]);
			if (position<=lastPos) 
				sorted = false;
			lastPos = position;
			size++;
			maxPos = Math.max(maxPos, position);
		}
		
		public void addValue(int position, double value) throws IOException {
			writer.putInt(position);
			writer.putDouble(value);
			if (position<=lastPos) 
				sorted = false;
			lastPos = position;
			size++;
			maxPos = Math.max(maxPos, position);
		}
		
		public void addValue(int position, Number value) throws IOException {
			if (value instanceof Byte) addValue(position, value.byteValue());
			else if (value instanceof Short) addValue(position, value.shortValue());
			else if (value instanceof Integer) addValue(position, value.intValue());
			else if (value instanceof Long) addValue(position, value.longValue());
			else if (value instanceof Float) addValue(position, value.floatValue());
			else addValue(position, value.doubleValue());
		}
		
		public void addValue(int position, float[] value) throws IOException {
			writer.putInt(position);
			for (int i=0; i<value.length; i++)
				writer.putFloat(value[i]);
			if (position<=lastPos) 
				sorted = false;
			lastPos = position;
			size++;
			maxPos = Math.max(maxPos, position);
		}
		
		public void addValue(int position, float value) throws IOException {
			writer.putInt(position);
			writer.putFloat(value);
			if (position<=lastPos) 
				sorted = false;
			lastPos = position;
			size++;
			maxPos = Math.max(maxPos, position);
		}
		
		
		public void addValue(int position, int[] value) throws IOException {
			writer.putInt(position);
			for (int i=0; i<value.length; i++) {
				writer.putInt(value[i]);
				maxValue[i] = indexSum?maxValue[i]+value[i]:Math.max(maxValue[i],value[i]);
				minValue[i] = Math.min(maxValue[i],value[i]);
			}
			if (position<=lastPos) 
				sorted = false;
			lastPos = position;
			size++;
			maxPos = Math.max(maxPos, position);
		}
		
		public void addValue(int position, int value) throws IOException {
			writer.putInt(position);
			writer.putInt(value);
			maxValue[0] = indexSum?maxValue[0]+value:Math.max(maxValue[0],value);
			minValue[0] = Math.min(maxValue[0],value);
			
			if (position<=lastPos) 
				sorted = false;
			lastPos = position;
			size++;
			maxPos = Math.max(maxPos, position);
		}
		
		public void addValue(int position, byte value) throws IOException {
			writer.putInt(position);
			writer.put(value);
			maxValue[0] = indexSum?maxValue[0]+value:Math.max(maxValue[0],value);
			minValue[0] = Math.min(maxValue[0],value);
			
			if (position<=lastPos) 
				sorted = false;
			lastPos = position;
			size++;
			maxPos = Math.max(maxPos, position);
		}
		
		public void addValue(int position, short value) throws IOException {
			writer.putInt(position);
			writer.putShort(value);
			maxValue[0] = indexSum?maxValue[0]+value:Math.max(maxValue[0],value);
			minValue[0] = Math.min(maxValue[0],value);
			
			if (position<=lastPos) 
				sorted = false;
			lastPos = position;
			size++;
			maxPos = Math.max(maxPos, position);
		}
		
		public void addValue(int position, long[] value) throws IOException {
			writer.putInt(position);
			for (int i=0; i<value.length; i++) {
				writer.putLong(value[i]);
				maxValue[i] = indexSum?maxValue[i]+value[i]:Math.max(maxValue[i],value[i]);
			}
			if (position<=lastPos) 
				sorted = false;
			lastPos = position;
			size++;
			maxPos = Math.max(maxPos, position);
		}
		
		public void addValue(int position, long value) throws IOException {
			writer.putInt(position);
			writer.putLong(value);
			maxValue[0] = indexSum?maxValue[0]+value:Math.max(maxValue[0],value);
			minValue[0] = Math.min(maxValue[0],value);
			if (position<=lastPos) 
				sorted = false;
			lastPos = position;
			size++;
			maxPos = Math.max(maxPos, position);
		}
		
		
		public void close() throws IOException {
			if (!this.writer.isClosed())
				this.writer.close();
		}
		
		public void write(PageFileWriter writer, boolean coverageMode, boolean dense) throws IOException {
			
			PageFile read = this.writer.read(true);
			if (!sorted) read = sort(read);
			if (coverageMode) read = cumsum(read);

			if (dense)
				writer.putAsciiChar(coverageMode?'W':'D');
			else
				writer.putAsciiChar(coverageMode?'C':'S');
			writer.putInt(dense?maxPos+1:size);
			writer.putInt(numCond);
			
			long start = read.position();
		
			
			for (int i=0; i<numCond; i++) {
				NumericArray a = NumericArray.createMemory(dense?maxPos+1:size, NumericArrayType.fromType(cls));
				int bytes = NumericArrayType.fromType(cls).getBytes();
				
				if (i==0) {
					for (int index = 0; index<size;) {
						int p = read.getInt();
						a.deserializeElement(dense?p:index,read);
						
						if (!dense)
							writer.putInt(p);
						
						index++;
						if (index<size)
							read.relativePosition(bytes*(numCond-1));
					}
				}
				else {
					read.position(start+Integer.BYTES+bytes*i);
					if (dense) throw new UnsupportedOperationException("Have to read positions everytime!");
					
					for (int index = 0; index<size;) {
						a.deserializeElement(index,read);
						index++;
						if (index<size)
							read.relativePosition(Integer.BYTES+bytes*(numCond-1));
					}
				}
				
				if (maxValue[i]>0) {
					if (maxValue[i]<=Byte.MAX_VALUE && minValue[i]>=Byte.MIN_VALUE && a.getType().getBytes()>NumericArrayType.Byte.getBytes())
						a = a.convert(NumericArrayType.Byte);
					else if (maxValue[i]<=Short.MAX_VALUE && minValue[i]>=Short.MIN_VALUE && a.getType().getBytes()>NumericArrayType.Short.getBytes())
						a = a.convert(NumericArrayType.Short);
					else if (maxValue[i]<=Integer.MAX_VALUE && minValue[i]>=Integer.MIN_VALUE && a.getType().getBytes()>NumericArrayType.Integer.getBytes())
						a = a.convert(NumericArrayType.Integer);
				}
				
				long indexBytes = 0;
				long dataBytes = 0;
				long before = writer.position();
				long ind = DiskMinMaxSumIndex.create(writer, a, true, indexSum, true);
				long after = writer.position();
				indexBytes+=ind-before;
				dataBytes+=after-ind;
//				System.out.printf("%s\t%d\t%d\t%d\n",read.getPath(),size*Integer.BYTES,dataBytes,indexBytes);
				
			}
			read.close();
			new File(this.writer.getPath()).delete();
		}
		
		private int sortSize = 10_000_000;
		private PageFile sort(PageFile f) throws IOException {
			LinkedList<IntNumeric> lines = new LinkedList<IntNumeric>();
			ArrayList<PageFile> tmps = new ArrayList<PageFile>();
			
			while (!f.eof()) {
				IntNumeric l = new IntNumeric(numCond,cls);
				l.deserialize(f);
				lines.add(l);
				if (lines.size()>sortSize) 
					writeTmp(tmps,lines, f.getPath());
			}
			f.close();
			if (lines.size()>0) 
				size = writeTmp(tmps,lines, f.getPath());
			
			if (tmps.size()==0) throw new RuntimeException("Empty!");
			
			if (tmps.size()==1) {
				tmps.get(0).close();
				new File(tmps.get(0).getPath()).renameTo(new File(f.getPath()));
			}
			else {
				
				// merge all tmps
				ArrayList<Iterator<IntNumeric>> iterators = new ArrayList<Iterator<IntNumeric>>();
				for (PageFile lof : tmps) iterators.add(new Iterator<IntNumeric>() {

					@Override
					public boolean hasNext() {
						return !lof.eof();
					}

					@Override
					public IntNumeric next() {
						IntNumeric re = new IntNumeric(numCond,cls);
						try {
							re.deserialize(lof);
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
						return re;
					}
					
				});
				@SuppressWarnings("unchecked")
				MergeIterator<IntNumeric> merge = new MergeIterator<IntNumeric>(iterators.toArray(new Iterator[0]), FunctorUtils.naturalComparator());
				PageFileWriter wr = new PageFileWriter(f.getPath());
				size = 0;
				if (merge.hasNext()) {
					IntNumeric last = merge.next();
					while (merge.hasNext()) {
						IntNumeric n = merge.next();
						if (last.p==n.p) {
							last.v.add(n.v);
						} else {
							last.serialize(wr);
							last = n;
							size++;
						}
					}
					last.serialize(wr);
					size++;
				}
				wr.close();
				
				for (PageFile lof : tmps) {
					lof.close();
					new File(lof.getPath()).delete();
				}
			}
			
			return new PageFile(f.getPath());
			
		}
		private int writeTmp(List<PageFile> tmps, LinkedList<IntNumeric> lines, String pref) throws IOException {
			PageFileWriter lof = new PageFileWriter(pref+".tmp"+tmps.size());
			Collections.sort(lines);
			ListIterator<IntNumeric> lit = lines.listIterator();
			while (lit.hasNext()) {
				IntNumeric n = lit.next();
				while (lit.hasNext()) {
					IntNumeric n2 = lit.next();
					if (n.p==n2.p) {
						n.v.add(n2.v);
						lit.remove();
					} else {
						lit.previous();
						break;
					}
				}
			}
			for (IntNumeric s : lines) s.serialize(lof);
			int size = lines.size();
			lines.clear();
			tmps.add(lof.read(true));
			return size;
		}
		
		
		private PageFile cumsum(PageFile f) throws IOException {
			PageFileWriter tmp = new PageFileWriter(f.getPath()+".cumsum");
			
			IntNumeric last = null;
			while (!f.eof()) {
				IntNumeric l = new IntNumeric(numCond,cls);
				l.deserialize(f);
				if (last==null) {}
				else if (last.p<l.p) l.v.add(last.v);
				else 
					throw new RuntimeException("Not sorted!");
				last = l;
				
				l.serialize(tmp);
			}
			f.close();
			
			tmp.close();
			new File(tmp.getPath()).renameTo(new File(f.getPath()));
			
			return new PageFile(f.getPath());
			
		}
	}
	
	
	
	private static class IntNumeric implements BinarySerializable, Comparable<IntNumeric> {
		int p;
		NumericArray v;
		
		public IntNumeric(int length, Class<? extends Number> type) {
			v = NumericArray.createMemory(length, NumericArrayType.fromType(type));
		}
		@Override
		public void serialize(BinaryWriter out) throws IOException {
			out.putInt(p);
			for (int i=0; i<v.length(); i++)
				v.serializeElement(i, out);
		}
		@Override
		public void deserialize(BinaryReader in) throws IOException {
			p = in.getInt();
			for (int i=0; i<v.length(); i++)
				v.deserializeElement(i, in);
		}
		@Override
		public int compareTo(IntNumeric o) {
			return p-o.p;
		}
	}
	
	
}
