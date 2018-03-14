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
package gedi.diskintervaltree;


import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.datastructure.collections.longcollections.LongArrayList;
import gedi.util.datastructure.tree.redblacktree.Interval;
import gedi.util.datastructure.tree.redblacktree.IntervalComparator;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;

import java.io.File;
import java.io.IOException;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.Spliterators;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;



public class DiskIntervalTree<T extends BinarySerializable> extends AbstractSet<GenomicRegion> implements NavigableSet<GenomicRegion>, Interval {
	/**
	 * The number of entries in the tree
	 */
	private int size = 0;

	private String info;

	private static final int OFFSET_min = 0;
	private static final int OFFSET_max = 4;
	private static final int OFFSET_start = 8;
	private static final int OFFSET_stop = 12;
	private static final int OFFSET_parent = 16;
	private static final int OFFSET_mask = 24;
	private static final int OFFSET_children = 25;
	private static final int SIZE_ptr = 8;


	
	
	private static class BinIndex implements BinarySerializable {

		private int binsize;
		private int min;
		private int max;
		
		private long[] offsets; // deepest node that fully contains the given bin 
		
		
		public void set(int binsize, int min, int max) {
			this.binsize = binsize;
			this.min = min;
			this.max = max;
			this.offsets = new long[(int)Math.ceil((max-min)/(double)binsize)];
		}
		
		@Override
		public void serialize(BinaryWriter out) throws IOException {
			out.putInt(binsize);
			out.putInt(min);
			out.putInt(max);
			for (int i=0; i<offsets.length; i++)
				out.putLong(offsets[i]);
		}
	
		@Override
		public void deserialize(BinaryReader in) throws IOException {
			set(in.getInt(),in.getInt(),in.getInt());
			for (int i=0; i<offsets.length; i++)
				offsets[i] = in.getLong();
		}

		public int getBin(int start, int stop) {
			int a = getBin(start);
			int b = getBin(stop);
			return a==b?a:-1;
		}
		
		public int getBin(int index) {
			int re = (index-min)/binsize;
			if (re>offsets.length) return -1;
			return re;
		}
		
		public long getOffset(int index) {
			int re = (index-min)/binsize;
			if (re>offsets.length) return -1;
			return offsets[re];
		}
		
	}
	
	
	public static <T extends BinarySerializable> void create(String info, 
			GenomicRegionStorage<?> storage, ReferenceSequence reference, BiFunction<ReferenceSequence,GenomicRegion,T> datagetter, long size,
			String path, Class<T> cls, BinaryOperator<? super T> aggregator, int...binsizes) throws IOException {

		if (size>Integer.MAX_VALUE) throw new RuntimeException();
		int s = (int) size;
		
		int[] min = new int[s];
		int[] max = new int[s];
		Object[] agg = new Object[s]; 
		
		// build tree structure data in memory
		Stack<int[]> stack = new Stack<int[]>();
		stack.add(new int[] {0,s,-1});// left, right, parentindex
		int root = s/2;
		int[] parent = new int[s];
		while (!stack.isEmpty()) {
			int[] in = stack.pop();
			int m = (in[0]+in[1])/2;
			parent[m] = in[2];
			if (m-in[0]>0) stack.push(new int[] {in[0],m,m});
			if (in[1]-m-1>0) stack.push(new int[] {m+1,in[1],m});
		}
		int[] left = new int[s];
		int[] right = new int[s];
		Arrays.fill(left, -1);
		Arrays.fill(right, -1);
		for (int i=0; i<parent.length; i++) {
			if (parent[i]==-1) continue;
			if (parent[i]<i) right[parent[i]]=i;
			else left[parent[i]]=i;
		}
		// determine min/max/agg
		Iterator<? extends GenomicRegion> it = Spliterators.iterator(storage.iterateGenomicRegions(reference));
		int di=0; 
		while (it.hasNext()) {
			GenomicRegion d = it.next();
			min[di] = d.getStart();
			max[di] = d.getStop();
			agg[di] = datagetter.apply(reference, d);
			di++;
		}
		IntArrayList dfs = new IntArrayList();
		dfs.add(-root-1);
		while (!dfs.isEmpty()) {
			int n = dfs.removeLast();
			if (n<0) { // walking down
				n = -n-1;
				dfs.add(n);
				if (left[n]>-1) dfs.add(-left[n]-1);
				if (left[n]>-1) dfs.add(-right[n]-1);
			}
			else { // walking up
				int l = left[n];
				int r = right[n];
				min[n] = Math.min(min[n],Math.min(l>-1?min[l]:min[n],r>-1?min[r]:min[n]));
				max[n] = Math.max(max[n],Math.max(l>-1?max[l]:max[n],r>-1?max[r]:max[n]));
				min[n] = Math.min(min[n],Math.min(l>-1?min[l]:min[n],r>-1?min[r]:min[n]));
				
				if (l>-1)
					agg[n] = aggregator.apply((T)agg[n],(T)agg[l]);
				if (r>-1)
					agg[n] = aggregator.apply((T)agg[n],(T)agg[r]);
				
			}
		}

		File file = new File(path);
		file.getParentFile().mkdirs();
		PageFileWriter out = new PageFileWriter(path);
		
		// Header
		out.putInt(info.length());
		out.putChars(info);
		out.putInt(s);
		out.putInt(cls.getName().length());
		out.putChars(cls.getName());
		long rootOffsetOffset = out.position();
		out.putLong(-1L); // root offset
		
		// Bin indices
		out.putInt(binsizes.length);
		long binOffset = out.position();
		BinIndex[] binInd = new BinIndex[binsizes.length];
		for (int i=0; i<binInd.length; i++) {
			binInd[i] = new BinIndex();
			binInd[i].set(binsizes[i], min[root], max[root]);
			binInd[i].serialize(out);
		}
		
		
		// tree
		long treeOffset = out.position();
		
		long[] offsets = new long[s];
		
		it = Spliterators.iterator(storage.iterateGenomicRegions(reference));
		di=0; 
		while (it.hasNext()) {
			GenomicRegion d = it.next();
			
			offsets[di] = out.position();
			
			int l = left[di];
			int r = right[di];
			int p = parent[di];
			
			out.putInt(min[di]);
			out.putInt(max[di]);
			out.putInt(d.getStart());
			out.putInt(d.getStop());
			out.putLong(-1);
			int mask = set(SPLICE,set(LEFT,set(RIGHT,set(DATA,set(AGGREGATED,0,datagetter.apply(reference, d)!=agg[di]),datagetter.apply(reference, d)!=null),r!=-1),l!=-1),!d.isSingleton());
			out.putByte(mask);

			if (l!=-1) out.putLong(-1);
			if (r!=-1) out.putLong(-1);

			if (!d.isSingleton()) {
				out.putInt(d.getNumParts());
				for (int c=1; c<d.getNumBoundaries()-1; c++)
					out.putInt(d.getBoundary(c));
			}
			
			T da = datagetter.apply(reference, d);
			if (da!=null) da.serialize(out);
			if (da!=agg[di]) ((T)agg[di]).serialize(out);
			
			di++;
		}
		
		out.position(rootOffsetOffset);
		out.putLong(offsets[root]);
		out.position(treeOffset);
		
		for (di=0; di<s; di++){
			out.position(offsets[di]+OFFSET_parent);
			
			int l = left[di];
			int r = right[di];
			int p = parent[di];
			
//			System.out.printf("%d/%d %d -> %d\n",di,data.size(),p==-1?-1:offsets[p],out.getFilePointer());
			out.putLong(p==-1?-1:offsets[p]);
			out.relativePosition(1);
			
			if (l!=-1) out.putLong(offsets[l]);
			if (r!=-1) out.putLong(offsets[r]);
		}
		
		// Bin indices
		out.position(binOffset);
		for (int i=0; i<binInd.length; i++) {
			Arrays.fill(binInd[i].offsets,-1);
			dfs.clear();
			dfs.add(root);
			while (!dfs.isEmpty()) {
				int n = dfs.removeLast();
				int bin = binInd[i].getBin(min[n],max[n]);
				if (bin!=-1) 
					binInd[i].offsets[bin] = offsets[n];
				else {
					if (left[n]>-1) dfs.add(left[n]);
					if (right[n]>-1) dfs.add(right[n]);
				}
			}
			binInd[i].serialize(out);
		}
		
		out.close();

	}


	private static final int LEFT = 0;
	private static final int RIGHT = 1;
	private static final int SPLICE = 2;
	private static final int DATA = 3;
	private static final int AGGREGATED = 4;

	private static boolean has(int pos, int mask) {
		return (mask>>pos & 1) ==1;
	}

	private static int set(int pos, int mask, boolean val) {
		if (val) return mask | 1<<pos;
		else return mask & ~(1<<pos);
	}

	private long parent(long offset) throws IOException {
		file.position(offset+OFFSET_parent);
		return file.getLong();
	}
	private long left(long offset) throws IOException {
		file.position(offset+OFFSET_mask);
		int mask = file.getByte();
		if (!has(LEFT,mask)) return -1;
		return file.getLong();
	}

	private long right(long offset) throws IOException {
		file.position(offset+OFFSET_mask);
		int mask = file.getByte();
		if (!has(RIGHT,mask)) return -1;
		if (has(LEFT,mask)) file.relativePosition(SIZE_ptr);
		return file.getLong();
	}
	
	private int readMin(long index) throws IOException {
		file.position(index+OFFSET_min);
		return file.getInt();
	}
	
	private int readMax(long index) throws IOException {
		file.position(index+OFFSET_max);
		return file.getInt();
	}

	private int readStart(long index) throws IOException {
		file.position(index+OFFSET_start);
		return file.getInt();
	}

	private int readStop(long index) throws IOException {
		file.position(index+OFFSET_stop);
		return file.getInt();
	}

	private DiskIntervalGenomicRegion<T> readRecord(long index) throws IOException {
		file.position(index+4+4);
		int start = file.getInt();
		int stop = file.getInt();
		file.relativePosition(SIZE_ptr); // parent ptr
		int mask = file.getByte();
		if (has(LEFT,mask)) 
			file.relativePosition(SIZE_ptr);
		if (has(RIGHT,mask)) 
			file.relativePosition(SIZE_ptr);

		int[] coord = null;
		if (has(SPLICE,mask)) {
			coord = new int[file.getInt()*2];
			coord[0] = start;
			coord[coord.length-1] = stop+1;
			for (int i=1; i<coord.length-1; i++)
				coord[i] = file.getInt();
		}
		
		T data = null;
		if (has(DATA,mask)) {
			try {
				data = cls.newInstance();
			} catch (Exception e) {
				throw new RuntimeException("Could not instantiate interval class!",e);
			}
			data.deserialize(file);
		}
		if (has(AGGREGATED,mask)) {
			try {
				T agg= cls.newInstance();
				agg.deserialize(file);
			} catch (Exception e) {
				throw new RuntimeException("Could not instantiate interval class!",e);
			}
		}
		return coord==null?new DiskIntervalGenomicRegion<T>(start, stop+1, data):new DiskIntervalGenomicRegion<T>(coord, data);
	}


	private DiskIntervalGenomicRegion<T> readAggregatedRecord(long index) throws IOException {
		file.position(index);
		int start = file.getInt();
		int stop = file.getInt();
		file.relativePosition(4+4+SIZE_ptr); // start, stop, parent ptr
		int mask = file.getByte();
		if (has(LEFT,mask)) 
			file.relativePosition(SIZE_ptr);
		if (has(RIGHT,mask)) 
			file.relativePosition(SIZE_ptr);

		if (has(SPLICE,mask)) {
			int size = file.getInt()*2;
			file.relativePosition(4*(size-2));
		}
		
		T data = null;
		if (has(DATA,mask)) {
			try {
				data = cls.newInstance();
			} catch (Exception e) {
				throw new RuntimeException("Could not instantiate interval class!",e);
			}
			data.deserialize(file);
		}
		if (has(AGGREGATED,mask)) {
			try {
				data = cls.newInstance();
				data.deserialize(file);
			} catch (Exception e) {
				throw new RuntimeException("Could not instantiate interval class!",e);
			}
		}
		return new DiskIntervalGenomicRegion<T>(start, stop+1, data);
	}

	private PageFile file;
	private BinIndex[] binIndices;

	private Class<T> cls;



	public Class<T> getType() {
		return cls;
	}
	
	private long root;

	public DiskIntervalTree(String path) throws IOException, ClassNotFoundException {
		file=new PageFile(path);
		char[] str = new char[file.getInt()];
		for (int i=0; i<str.length; i++)
			str[i] = file.getChar();
		this.info = new String(str);
		size = file.getInt();
		str = new char[file.getInt()];
		for (int i=0; i<str.length; i++)
			str[i] = file.getChar();
		cls = (Class<T>) Class.forName(new String(str));

		root = file.getLong();
		binIndices = new BinIndex[file.getInt()];
		for (int i=0; i<binIndices.length; i++) {
			binIndices[i] = new BinIndex();
			binIndices[i].deserialize(file);
		}
	}
	
	
	public Collection<DiskIntervalGenomicRegion<T>> getIntervals() {
		return new DiskOrderingCollection();
	}
	
	
	private class DiskOrderingCollection extends AbstractCollection<DiskIntervalGenomicRegion<T>> {

		@Override
		public Iterator<DiskIntervalGenomicRegion<T>> iterator() {
			return new DiskOrderingIterator();
		}

		@Override
		public int size() {
			return size;
		}
		
	}
	
	private class DiskOrderingIterator implements Iterator<DiskIntervalGenomicRegion<T>> {

		private long expectedOffset;
		private long length;
		
		public DiskOrderingIterator() {
			try {
				expectedOffset = getFirstOffset();
				file.position(expectedOffset);
				length = file.size();
			} catch (IOException e) {
				throw new RuntimeException("Cannot iterate!",e);
			}
		}
		
		@Override
		public boolean hasNext() {
			checkConcurrentOperation();
			return expectedOffset<length;
		}

		@Override
		public DiskIntervalGenomicRegion<T> next() {
			try {
				checkConcurrentOperation();
				DiskIntervalGenomicRegion<T> rec = readRecord(expectedOffset);
				expectedOffset = file.position();
				return rec;
			} catch (IOException e) {
				throw new RuntimeException("Cannot iterate!",e);
			}
		}
		
		private void checkConcurrentOperation() {
			if (file.position()!=expectedOffset)
				throw new ConcurrentModificationException();
		}

		@Override
		public void remove() {
		}
		
	}
	
	
	public void close() throws IOException {
		file.close();
	}
	
	public String getInfo() {
		return info;
	}

	
	public void checkIntegrity() throws IOException {
		long length = file.size();
		file.position(getFirstOffset());
		int entry = 0;
		while (file.position()<length) {
			long offset = file.position();
			DiskIntervalGenomicRegion<T> rec = readRecord(offset);
			System.out.printf("Entry %d\t%s",entry++,rec);
			long nextoffset = file.position();
			if (left(offset)==-1)
				System.out.printf("\t-");
			else if (parent(left(offset))!=offset) 
				System.out.printf("\tleft parent pointer wrong (%d != %d; %d) ",parent(left(offset)), offset, left(offset));
			else
				System.out.printf("\tok");
			if (right(offset)==-1)
				System.out.printf("\t-");
			else if (parent(right(offset))!=offset) 
				System.out.printf("\tright parent pointer wrong (%d != %d; %d) ",parent(right(offset)), offset, right(offset));
			else
				System.out.printf("\tok");
			file.position(nextoffset);
			System.out.println();
		}	
	}
	

	private long getFirstOffset() throws IOException {
		long p = root;
		if (p != -1)
			while (left(p) != -1) {
				p = left(p);
			}
		return p;
	}

	private long getLastOffset() throws IOException {
		long p = root;
		if (p != -1)
			while (right(p) != -1) {
				p = right(p);
			}
		return p;
	}

	private long getOffset(GenomicRegion k) throws IOException {
		long p = root;
		while (p != -1) {
			int cmp = compareOffsets(k, p);
			if (cmp < 0)
				p = left(p);
			else if (cmp > 0)
				p = right(p);
			else
				return p;
		}
		return -1;
	}

	private long getLowerOffset(GenomicRegion k) throws IOException {
		long p = root;
		while (p != -1) {
			int cmp = compareOffsets(k, p);
			if (cmp > 0) {
				long r = right(p);
				if (r != -1)
					p = r;
				else
					return p;
			} else {
				long l = left(p);
				if (l != -1) {
					p = l;
				} else {
					long parent = parent(p);
					long ch = p;
					while (parent != -1 && ch == left(parent)) {
						ch = parent;
						parent = parent(parent);
					}
					return parent;
				}
			}
		}
		return -1;
	}


	private long getHigherOffset(GenomicRegion k) throws IOException {
		long p = root;
		while (p != -1) {
			int cmp = compareOffsets(k, p);
			if (cmp < 0) {
				long l = left(p);
				if (l != -1)
					p = l;
				else
					return p;
			} else {
				long r = right(p);
				if (r != -1) {
					p = r;
				} else {
					long parent = parent(p);
					long ch = p;
					while (parent != -1 && ch == right(parent)) {
						ch = parent;
						parent = parent(parent);
					}
					return parent;
				}
			}
		}
		return -1;
	}

	private long getCeilingOffset(GenomicRegion k) throws IOException {
		long p = root;
		while (p != -1) {
			int cmp = compareOffsets(k, p);
			if (cmp < 0) {
				long l = left(p);
				if (l != -1)
					p = l;
				else
					return p;
			} else if (cmp > 0){
				long r = right(p);
				if (r != -1) {
					p = r;
				} else {
					long parent = parent(p);
					long ch = p;
					while (parent != -1 && ch == right(parent)) {
						ch = parent;
						parent = parent(parent);
					}
					return parent;
				}
			} else return p;
		}
		return -1;
	}

	private long getFloorOffset(GenomicRegion k) throws IOException { 
		long p = root;
		while (p != -1) {
			int cmp = compareOffsets(k, p);
			if (cmp > 0) {
				long r = right(p);
				if (r != -1)
					p = r;
				else
					return p;
			} else if (cmp < 0) {
				long l = left(p);
				if (l != -1) {
					p = l;
				} else {
					long parent = parent(p);
					long ch = p;
					while (parent != -1 && ch == left(parent)) {
						ch = parent;
						parent = parent(parent);
					}
					return parent;
				}
			}else
				return p;
		}
		return -1;
	}
	
	
	private int compareOffsets(GenomicRegion o1, long o2) throws IOException {
		int re = o1.getStart()-readStart(o2);
		if (re==0)
			re = o1.getStop()-readStop(o2);
		if (re==0) {
			GenomicRegion i1 = o1;
			DiskIntervalGenomicRegion<T> i2 = readRecord(o2);
			re = i1.compareTo(i2);
		}
		return re;
	}

	private int compareOffsets(long o1, long o2) throws IOException {
		if (o1==o2)
			return 0;

		int re = readStart(o1)-readStart(o2);
		if (re==0)
			re = readStop(o1)-readStop(o2);
		if (re==0) {
			DiskIntervalGenomicRegion<T> i1 = readRecord(o1);
			DiskIntervalGenomicRegion<T> i2 = readRecord(o2);
			re = i1.compareTo(i2);
		}
		return re;
	}

	/**
     * Returns the successor of the specified Entry, or null if no such.
	 * @throws IOException 
     */
    private long successorOffset(long t) throws IOException {
        if (t == -1)
            return -1;
        else if (right(t) != -1) {
            long p = right(t);
            while (left(p) != -1)
                p = left(p);
            return p;
        } else {
            long p = parent(t);
            long ch = t;
            while (p != -1 && ch == right(p)) {
                ch = p;
                p = parent(p);
            }
            return p;
        }
    }

    /**
     * Returns the predecessor of the specified Entry, or null if no such.
     * @throws IOException 
     */
    private long predecessorOffset(long t) throws IOException {
    	if (t == -1)
            return -1;
        else if (left(t) != -1) {
            long p = left(t);
            while (right(p) != -1)
                p = right(p);
            return p;
        } else {
            long p = parent(t);
            long ch = t;
            while (p != -1 && ch == left(p)) {
                ch = p;
                p = parent(p);
            }
            return p;
        }
    }
    

    @Override
    public String toString() {
    	return info+" ("+size+")";
    }

	public Comparator<? super GenomicRegion> comparator() {
		return new IntervalComparator();
	}

	@Override
	public DiskIntervalGenomicRegion<T> first() {
		try {
			return readRecord(getFirstOffset());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public DiskIntervalGenomicRegion<T> last() {
		try {
			return readRecord(getLastOffset());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size==0;
	}

	@Override
	public boolean contains(Object o) {
		if (!(o instanceof GenomicRegion)) return false;
		try {
			return getOffset((GenomicRegion) o)!=-1;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	public DiskIntervalGenomicRegion<T> get(Object o) {
		if (!(o instanceof GenomicRegion)) return null;
		if (o instanceof DiskIntervalGenomicRegion)
			return ((DiskIntervalGenomicRegion<T>)o);
		try {
			long offset = getOffset((GenomicRegion) o);
			if (offset==-1) return null;
			return readRecord(offset);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean add(GenomicRegion e) {
		throw new RuntimeException("Not implemented!");
	}

	@Override
	public boolean remove(Object o) {
		throw new RuntimeException("Not implemented!");
	}

	@Override
	public boolean addAll(Collection<? extends GenomicRegion> c) {
		throw new RuntimeException("Not implemented!");
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new RuntimeException("Not implemented!");
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new RuntimeException("Not implemented!");
	}

	@Override
	public void clear() {
		throw new RuntimeException("Not implemented!");
	}

	@Override
	public DiskIntervalGenomicRegion<T> lower(GenomicRegion e) {
		try {
			return readRecord(getLowerOffset(e));
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}
	}

	@Override
	public DiskIntervalGenomicRegion<T> floor(GenomicRegion e) {
		try {
			return readRecord(getFloorOffset(e));
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}
	}

	@Override
	public DiskIntervalGenomicRegion<T> ceiling(GenomicRegion e) {
		try {
			return readRecord(getCeilingOffset(e));
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}
	}

	@Override
	public DiskIntervalGenomicRegion<T> higher(GenomicRegion e) {
		try {
			return readRecord(getHigherOffset(e));
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}
	}

	@Override
	public DiskIntervalGenomicRegion<T> pollFirst() {
		throw new RuntimeException("Not implemented!");
	}

	@Override
	public DiskIntervalGenomicRegion<T> pollLast() {
		throw new RuntimeException("Not implemented!");
	}

	@Override
	public Iterator<GenomicRegion> iterator() {
		try {
			return new AscendingIterator(getFirstOffset());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	/**
	 * Base class for TreeMap Iterators
	 */
	private abstract class DiskIntervalIterator implements Iterator<GenomicRegion> {
		long next;
		long lastReturned;

		DiskIntervalIterator(long first) {
			lastReturned = -1;
			next = first;
		}

		public final boolean hasNext() {
			return next != -1;
		}

		final long nextOffset() throws IOException {
			long e = next;
			if (e == -1)
				throw new NoSuchElementException();
			next = successorOffset(e);
			lastReturned = e;
			return e;
		}

		final  long prevOffset() throws IOException {
			long e = next;
			if (e == -1)
				throw new NoSuchElementException();
			next = predecessorOffset(e);
			lastReturned = e;
			return e;
		}

		public void remove() {
			throw new RuntimeException("Not implemented!");
		}
	}

	private class AscendingIterator extends DiskIntervalIterator {
		AscendingIterator(long first) {
			super(first);
		}
		public DiskIntervalGenomicRegion<T> next() {
			try {
				return readRecord(nextOffset());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	final class DescendingIterator extends DiskIntervalIterator {
		DescendingIterator(long first) {
			super(first);
		}
		public DiskIntervalGenomicRegion<T> next() {
			try {
				return readRecord(prevOffset());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public NavigableSet<GenomicRegion> descendingSet() {
		throw new RuntimeException("Not implemented!");
	}

	@Override
	public Iterator<GenomicRegion> descendingIterator() {
		try {
			return new DescendingIterator(getLastOffset());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public NavigableSet<GenomicRegion> subSet(
			GenomicRegion fromElement, boolean fromInclusive,
			GenomicRegion toElement, boolean toInclusive) {
		throw new RuntimeException("Not implemented!");
	}

	@Override
	public NavigableSet<GenomicRegion> headSet(
			GenomicRegion toElement, boolean inclusive) {
		throw new RuntimeException("Not implemented!");
	}

	@Override
	public NavigableSet<GenomicRegion> tailSet(
			GenomicRegion fromElement, boolean inclusive) {
		throw new RuntimeException("Not implemented!");
	}

	@Override
	public SortedSet<GenomicRegion> subSet(GenomicRegion fromElement,
			GenomicRegion toElement) {
		throw new RuntimeException("Not implemented!");
	}

	@Override
	public SortedSet<GenomicRegion> headSet(GenomicRegion toElement) {
		throw new RuntimeException("Not implemented!");
	}

	@Override
	public SortedSet<GenomicRegion> tailSet(GenomicRegion fromElement) {
		throw new RuntimeException("Not implemented!");
	}


	public <C extends Collection<? super DiskIntervalGenomicRegion<T>>> C getIntervalsIntersecting(int start, int stop, C re) {
		LongArrayList stack = new LongArrayList();
		
//		for (BinIndex bin : binIndices)
//			if (bin.getBin(start, stop)!=-1) {
//				stack.add(bin.getOffset(start));
//				break;
//			}
		
		if (root!=-1 && stack.isEmpty())
			stack.add(root);
		
		try{
		while (!stack.isEmpty()) {
			long n = stack.removeLast();
			if (start<=readMax(n)) {
				int a = readStart(n);
				int b = readStop(n);
				if (a<=stop && b>=start)
					addChecked(re,start, stop, readRecord(n));
				
				if (left(n)!= -1)
					stack.add(left(n));
				if (stop>=a && right(n) != -1)
					stack.add(right(n));
			}
		}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return re;
	}
	
	public <C extends Collection<? super DiskIntervalGenomicRegion<T>>> C getIntervalsIntersecting(GenomicRegion reg, C re) {
		LongArrayList stack = new LongArrayList();
		
//		for (BinIndex bin : binIndices)
//			if (bin.getBin(start, stop)!=-1) {
//				stack.add(bin.getOffset(start));
//				break;
//			}
		
		if (root!=-1 && stack.isEmpty())
			stack.add(root);
		int start = reg.getStart();
		int stop = reg.getStop();
		
		try{
		while (!stack.isEmpty()) {
			long n = stack.removeLast();
			if (start<=readMax(n)) {
				int a = readStart(n);
				int b = readStop(n);
				if (a<=stop && b>=start)
					addChecked(re,reg, readRecord(n));
				
				if (left(n)!= -1)
					stack.add(left(n));
				if (stop>=a && right(n) != -1)
					stack.add(right(n));
			}
		}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return re;
	}
	
	private <C extends Collection<? super DiskIntervalGenomicRegion<T>>> void addChecked(C coll, int start, int stop, DiskIntervalGenomicRegion<T> co) {
		if (co.intersects(start, stop+1))
			coll.add(co);
	}
	
	private <C extends Collection<? super DiskIntervalGenomicRegion<T>>> void addChecked(C coll, GenomicRegion reg, DiskIntervalGenomicRegion<T> co) {
		if (co.intersects(reg))
			coll.add(co);
	}
	
	public <C extends Collection<? super DiskIntervalGenomicRegion<T>>> C getIntervalsIntersecting(int start, int stop, int resolution, C re) {
		LongArrayList stack = new LongArrayList();
//		
//		for (BinIndex bin : binIndices)
//			if (bin.getBin(start, stop)!=-1) {
//				stack.add(bin.getOffset(start));
//				break;
//			}
		
		if (root!=-1 && stack.isEmpty())
			stack.add(root);
		
		try{
		while (!stack.isEmpty()) {
			long n = stack.removeLast();
			if (start<=readMax(n)) {
				int a = readStart(n);
				int b = readStop(n);
				if (a<=stop && b>=start) {
					int min = readMin(n);
					int max = readMax(n);
					if (max-min<resolution){  
						addChecked(re,start, stop, readAggregatedRecord(n));
						continue;
					}
					re.add(readRecord(n));
				}
				
				if (left(n)!= -1)
					stack.add(left(n));
				if (stop>=a && right(n) != -1)
					stack.add(right(n));
			}
		}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return re;
	}
	
	@Override
	public int getStart() {
		return first().getStart();
	}


	@Override
	public int getStop() {
		try {
			return readMax(root);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Iterates over overlapping groups of intervals
	 * @return
	 */
	public Iterator<IntervalTree<DiskIntervalGenomicRegion<T>,T>> groupIterator() {
		return groupIterator(0);
	}

	/**
	 * Iterates over overlapping groups of intervals; overlap with a tolerance means that two intervals overlap as long as their distance is smaller or equal to the given distance
	 * @return
	 */
	public Iterator<IntervalTree<DiskIntervalGenomicRegion<T>,T>> groupIterator(int tolerance) {
		return new GroupIterator(tolerance);
	}

	


	private class GroupIterator implements Iterator<IntervalTree<DiskIntervalGenomicRegion<T>,T>> {

		private AscendingIterator it;
		private DiskIntervalGenomicRegion<T> first;
		private long firstOffset;
		private int intervalMax;

		private IntervalTree<DiskIntervalGenomicRegion<T>,T> next;
		private int tolerance;

		public GroupIterator(int tolerance) {
			this.tolerance = tolerance;
			try {
				it = new AscendingIterator(getFirstOffset());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			first = it.hasNext()?it.next():null;
			firstOffset = it.lastReturned;
			intervalMax = first!=null?first.getStop():-1;
		}

		@Override
		public boolean hasNext() {
			lookAhead();
			return next!=null;
		}

		@Override
		public IntervalTree<DiskIntervalGenomicRegion<T>,T> next() {
			lookAhead();
			IntervalTree<DiskIntervalGenomicRegion<T>,T> re = next;
			next = null;
			return re;
		}

		private void lookAhead() {
			if (next==null && first!=null) {
				while (it.hasNext()) {
					DiskIntervalGenomicRegion<T> current = it.next();
					if (intervalMax+tolerance<current.getStart() && !wouldNotSplit(intervalMax,current.getStart())) {// new group
						next = new IntervalTree<DiskIntervalGenomicRegion<T>,T>(null);
						AscendingIterator cit = new AscendingIterator(firstOffset);
						while (cit.hasNext()) {
							DiskIntervalGenomicRegion<T> n = cit.next();
							if (it.lastReturned==cit.lastReturned) break;
							next.add(n);
						}
						first = current;
						firstOffset = it.lastReturned;
						intervalMax = Math.max(current.getStop(), intervalMax);
						return;
					}
					intervalMax = Math.max(current.getStop(), intervalMax);
				}
				next = new IntervalTree<DiskIntervalGenomicRegion<T>,T>(null);
				AscendingIterator cit = new AscendingIterator(firstOffset);
				while (cit.hasNext()) {
					DiskIntervalGenomicRegion<T> n = cit.next();
					next.add(n);
				}
				first = null;
			}
		}

		private boolean wouldNotSplit(int start, int stop) {
			return false;
		}

		@Override
		public void remove() {}

	}


}