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

package gedi.centeredDiskIntervalTree;

import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.util.FileUtils;
import gedi.util.io.randomaccess.ConcurrentPageFile;
import gedi.util.io.randomaccess.ConcurrentPageFileView;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * four parts:
 * 
 * 1. Header
 * MAGIC,size(i.e.number of nodes in table), region count, max position,index of lists, index of data
 * chars,int,int,int,long,long
 * 
 * 2. Nodes
 * node,offset,...
 * int,long
 * offset refers to position in list (i.e. 0 is beginning of list part)
 * 
 * 3. Lists
 * length,n1,o1,n2,o2,n1,o1,n2,o2,...
 * cshort,cint,clong,cint,clong
 * update: cint,cint,clong,cint,clong
 * n1 is the stop position of interval, n2 the start position of interval, all n1 are sorted in descending order, all n2 in ascending order
 * offsets (o1,o2) refer to position in data (i.e. 0 is beginning of data part)
 * 
 * 4. Data
 * Depends on D
 * 
 * @author erhard
 *
 * @param <D>
 */
public class CenteredDiskIntervalTree<D> {

	private ConcurrentPageFileView all;
	private ConcurrentPageFileView nodes;
	private ConcurrentPageFileView lists;
	private ConcurrentPageFileView data;
	
	
	private int size = -1;
	private int count;
	private int max;
	
	private Supplier<D> supplier;
	
	private ConcurrentPageFile parent;
	private long origEnd;
	
	
	public CenteredDiskIntervalTree(Supplier<D> supplier, ConcurrentPageFile parent, long start, long end) throws IOException {
		this.supplier = supplier;
		this.parent = parent;
		origEnd = end;
		end = start+3*Integer.BYTES+2*Long.BYTES+InternalCenteredDiskIntervalTreeBuilder.MAGIC.length();
//		System.out.println("All: "+start+"-"+end);
		all = new ConcurrentPageFileView(parent, start, end);
	}
	
	public long getStart() {
		return all.getStart();
	}
	
	public long getEnd() {
		return origEnd;
	}
	
	public long getBytes() {
		return getEnd()-getStart();
	}
	
	
	public void setSupplier(Supplier<D> supplier) {
		this.supplier = supplier;
	}
	
	private void readHeader() {
		if (nodes==null){
			
			synchronized (this) {
				
				if (nodes==null) {
					try {
						
//						System.out.println("@"+all.getStart()+"+"+all.position());
						
						if (!all.getAsciiChars(InternalCenteredDiskIntervalTreeBuilder.MAGIC.length()).equals(InternalCenteredDiskIntervalTreeBuilder.MAGIC))
							throw new RuntimeException("Wrong file format!");
						size = all.getInt();
						count = all.getInt();
						max = all.getInt();
						
						
//						System.out.println(InternalCenteredDiskIntervalTreeBuilder.MAGIC);
//						System.out.println(size);
//						System.out.println(count);
//						System.out.println(max);
//						System.out.println("@"+all.getStart()+"+"+all.position());
						
						
						long listsOffset = all.getLong();
						long dataOffset = all.getLong();
						
						long start = all.getStart()+all.position();
						long end = all.getStart()+listsOffset;
//						System.out.println("Nodes: "+start+"-"+end);
						nodes = new ConcurrentPageFileView(parent,start,end);
						start = all.getStart()+listsOffset;
						end = all.getStart()+dataOffset;
//						System.out.println("Lists: "+start+"-"+end);
						lists = new ConcurrentPageFileView(parent,start,end);
						start = all.getStart()+dataOffset;
						end = origEnd;
//						System.out.println("Data: "+start+"-"+end);
						data = new ConcurrentPageFileView(parent,start,end);
						
					} catch (IOException e) {
						throw new RuntimeException("Wrong file format!",e);
					}
				}
				
			}
			
		}
	}
	
	public int size() {
		readHeader();
		return count;
	}
	
//	public void checkList() throws IOException {
//		lists.position(0);
//		CenteredDiskIntervalTreeNode node = new CenteredDiskIntervalTreeNode();
//		
//		while (!lists.eof()) {
//			System.out.println(lists.position()+lists.getStart());
//			int len = this.lists.getCInt();
//			for (int i=0; i<len; i++) {
//				node.deserialize(this.lists);
//				System.out.println(node);
//				node.deserialize(this.lists);
//				System.out.println(node);
//			}
//		}
//	}
	

	public Spliterator<MutableReferenceGenomicRegion<D>> spliterator(ReferenceSequence ref) {
//		return iterateIntersectingRegions(ref, new ArrayGenomicRegion(0,Integer.MAX_VALUE));
		try {
			readHeader();
			ConcurrentPageFileView file = new ConcurrentPageFileView(data);
			
			return new Spliterator<MutableReferenceGenomicRegion<D>>() {
				MutableReferenceGenomicRegion<D> mrgr = new MutableReferenceGenomicRegion<D>();
				@Override
				public boolean tryAdvance(Consumer<? super MutableReferenceGenomicRegion<D>> action) {
					try {
						if (file.eof()) return false;
						
//						int parts = file.getInt();
//						long offset = file.getStart()+file.position();
						GenomicRegion re = getRegion(file);//new PageGenomicRegion(new PageFile(file.getPath(), offset, offset+parts*2*Integer.BYTES));
						D d = supplier.get();
						FileUtils.deserialize(d,file);// dummy to advance
						
						action.accept(mrgr.set(ref, re, d));
						return true;
						
					} catch (IOException e) {
						throw new RuntimeException("Cannot read entry!",e);
					}
				}
	
				@Override
				public Spliterator<MutableReferenceGenomicRegion<D>> trySplit() {
					return null;
				}
	
				@Override
				public long estimateSize() {
					readHeader();
					return count;
				}
	
				@Override
				public int characteristics() {
					return DISTINCT|NONNULL|ORDERED|IMMUTABLE|SIZED;
				}
				
			};
		} catch (IOException e) {
			throw new RuntimeException("Cannot read file!",e);
		}
	}

	protected GenomicRegion getRegion(ConcurrentPageFileView file) throws IOException {
		int[] re = new int[file.getCInt()*2];
		for (int i=0; i<re.length; i++) {
			re[i] = file.getCInt();
			if (i>0) re[i]+=re[0];
		}
		return new ArrayGenomicRegion(re);
	}

	public int getNumRegions() {
		readHeader();
		return count;
	}
	
	
	public <C extends Collection<GenomicRegion>> C getIntersectingRegions(int start, int stop, C re) throws IOException {
		
		HashSet<Long> data = new HashSet<Long>();
		findAllOffsets(start, stop, data);
		
		for (Long l : data) {
			this.data.position(l);
			GenomicRegion reg = getRegion(this.data);
			re.add(reg);
		}
		
		return re;
	}
	
	public <C extends Map<GenomicRegion,D>> C getIntersectingRegions(int start, int stop, C re) throws IOException {
		HashSet<Long> data = new HashSet<Long>();
		findAllOffsets(start, stop, data);
		
		
		for (Long l : data) {
			this.data.position(l);
			GenomicRegion reg = getRegion(this.data);
			D d = supplier.get();
			FileUtils.deserialize(d,this.data);
			
			re.put(reg, d);
		}
		
		return re;
	}
	
	public <C extends Collection<GenomicRegion>> C getIntersectingRegions(GenomicRegion region, C re) throws IOException {
		HashSet<Long> data = new HashSet<Long>();
		for (int i=0; i<region.getNumParts(); i++) 
			findAllOffsets(region.getStart(i), region.getStop(i), data);
		
		for (Long l : data) {
			this.data.position(l);
			GenomicRegion reg = getRegion(this.data);
			re.add(reg);
		}
		return re;
	}
	
	public <C extends Map<GenomicRegion,D>> C getIntersectingRegions(GenomicRegion region, C re) throws IOException {
		
		HashSet<Long> data = new HashSet<Long>();
		for (int i=0; i<region.getNumParts(); i++) 
			findAllOffsets(region.getStart(i), region.getStop(i), data);
		
		
		for (Long l : data) {
			this.data.position(l);
			GenomicRegion reg = getRegion(this.data);
			D d = supplier.get();
			
			FileUtils.deserialize(d,this.data);
			
			re.put(reg, d);
		}
		
		return re;
	}
	
	
	public Spliterator<MutableReferenceGenomicRegion<D>> iterateIntersectingRegions(ReferenceSequence reference, GenomicRegion region) {
		readHeader();
		
		HashSet<Long> data = new HashSet<Long>();
		try {
			for (int i=0; i<region.getNumParts(); i++) 
				findAllOffsets(region.getStart(i), region.getStop(i), data);
		} catch (IOException e) {
			throw new RuntimeException("Could not find intersecting regions!",e);
		}
		long[] offs = new long[data.size()];
		int index = 0;
		for (Long l : data) {
			offs[index++] = l;
		}
		
		return new Spliterator<MutableReferenceGenomicRegion<D>>() {
			MutableReferenceGenomicRegion<D> re = new MutableReferenceGenomicRegion<D>();
			int index;
			@Override
			public boolean tryAdvance(
					Consumer<? super MutableReferenceGenomicRegion<D>> action) {
				if (index>=offs.length) {
					return false;
				}
				
				try {
					CenteredDiskIntervalTree.this.data.position(offs[index++]);
					GenomicRegion reg = getRegion(CenteredDiskIntervalTree.this.data);
					D d = supplier.get();
					FileUtils.deserialize(d,CenteredDiskIntervalTree.this.data);
					
					action.accept(re.set(reference, reg, d));
					return true;
				} catch (IOException e) {
					throw new RuntimeException("Could not find intersecting regions!",e);
				}
				
			}

			@Override
			public Spliterator<MutableReferenceGenomicRegion<D>> trySplit() {
				return null;
			}

			@Override
			public long estimateSize() {
				return data.size();
			}

			@Override
			public int characteristics() {
				return CONCURRENT|DISTINCT|IMMUTABLE|NONNULL|ORDERED|SIZED|SORTED;
			}
			
		};
	}
	
	
	private void findAllOffsets(int start, int stop, HashSet<Long> data) throws IOException {
		readHeader();
		
		HashSet<Long> left = new HashSet<Long>();
		HashSet<Long> right = new HashSet<Long>();
		HashSet<Long> all = new HashSet<Long>();
		
		computeNodeCreateTransient(start, stop,left,right,all);
		
		
		CenteredDiskIntervalTreeNode node = new CenteredDiskIntervalTreeNode();
		for (Long l : all) {
			this.lists.position(l);
			int len = this.lists.getCInt();
			for (int i=0; i<len; i++) {
				node.deserialize(this.lists);
				data.add(node.getPtr());
				node.deserialize(this.lists);
			}
		}
		
		for (Long l : left) {
			this.lists.position(l);
			int len = this.lists.getCInt();
			for (int i=0; i<len; i++) {
				node.deserialize(this.lists);
				if (node.getNode()<start) break;
				data.add(node.getPtr());
				node.deserialize(this.lists);
			}
		}
		
		for (Long l : right) {
			this.lists.position(l);
			int len = this.lists.getCInt();
			for (int i=0; i<len; i++) {
				node.deserialize(this.lists);
				node.deserialize(this.lists);
				if (node.getNode()>stop) break;
				data.add(node.getPtr());
			}
		}
	}
	
	
	public <C extends Collection<GenomicRegion>> C getContainedConsistentRegions(int start, int stop, C re) throws IOException {
		
		HashSet<Long> data = new HashSet<Long>();
		findAllOffsetsContained(start, stop, data);
		
		for (Long l : data) {
			this.data.position(l);
			GenomicRegion reg = getRegion(this.data);
			if (reg.getNumParts()==1 && reg.getStart()>=start && reg.getStop()<=stop)
				re.add(reg);
		}
		
		return re;
	}
	
	public <C extends Map<GenomicRegion,D>> C getContainedConsistentRegions(int start, int stop, C re) throws IOException {
		
		HashSet<Long> data = new HashSet<Long>();
		findAllOffsetsContained(start, stop, data);
		
		
		for (Long l : data) {
			this.data.position(l);
			GenomicRegion reg = getRegion(this.data);
			if (reg.getNumParts()==1 && reg.getStart()>=start && reg.getStop()<=stop){
				D d = supplier.get();
				FileUtils.deserialize(d,this.data);
				re.put(reg, d);
			}
		}
		
		return re;
	}
	
	public <C extends Collection<GenomicRegion>> C getContainedConsistentRegions(GenomicRegion region, C re) throws IOException {
		HashSet<Long> data = new HashSet<Long>();
		for (int i=0; i<region.getNumParts(); i++) 
			findAllOffsetsContained(region.getStart(i), region.getStop(i), data);
		
		for (Long l : data) {
			this.data.position(l);
			GenomicRegion reg = getRegion(this.data);
			if (region.containsUnspliced(reg))
				re.add(reg);
		}
		
		return re;
	}
	
	public <C extends Map<GenomicRegion,D>> C getContainedConsistentRegions(GenomicRegion region, C re) throws IOException {
		
		HashSet<Long> data = new HashSet<Long>();
		for (int i=0; i<region.getNumParts(); i++) 
			findAllOffsetsContained(region.getStart(i), region.getStop(i), data);
		
		
		for (Long l : data) {
			this.data.position(l);
			GenomicRegion reg = getRegion(this.data);
			if (region.containsUnspliced(reg)) {
				D d = supplier.get();
				FileUtils.deserialize(d,this.data);
				re.put(reg, d);
			}
		}
		
		return re;
	}
	
	public boolean contains(GenomicRegion region) throws IOException {
		HashSet<Long> data = new HashSet<Long>();
		for (int i=0; i<region.getNumParts(); i++) 
			findAllOffsetsContained(region.getStart(i), region.getStop(i), data);
		
		for (Long l : data) {
			this.data.position(l);
			GenomicRegion reg = getRegion(this.data);
			if (region.equals(reg)) return true;
		}
		
		return false;
	}

	
	public D getData(GenomicRegion region) throws IOException {
		HashSet<Long> data = new HashSet<Long>();
		for (int i=0; i<region.getNumParts(); i++) 
			findAllOffsetsContained(region.getStart(i), region.getStop(i), data);
		
		for (Long l : data) {
			this.data.position(l);
			GenomicRegion reg = getRegion(this.data);
			if (region.equals(reg)) {
				D d = supplier.get();
				FileUtils.deserialize(d,this.data);
				return d;
			}
		}

		
		return null;
	}

	
	private void findAllOffsetsContained(int start, int stop, HashSet<Long> data) throws IOException {
		HashSet<Long> all = new HashSet<Long>();
		getAllPtr(start,stop,all);

		
		CenteredDiskIntervalTreeNode node = new CenteredDiskIntervalTreeNode();
		for (Long l : all) {
			this.lists.position(l);
			int len = this.lists.getCInt();
			for (int i=0; i<len; i++) {
				node.deserialize(this.lists);
				data.add(node.getPtr());
				node.deserialize(this.lists);
			}
		}
		
		
	}
	
	
	private int computeNodeCreateTransient(int l, int u, Set<Long> left, Set<Long> right, Set<Long> all) throws IOException {
		
		int node = max/2;
		int step;
		
		for (step=Math.abs(node/2); step>=1; step/=2) {
			if (u<node){
				getPtr(node,right);
				node-=step;
			}
			else if (node<l){
				getPtr(node,left);
				node+=step;
			}
			else break;
		}

		int lnode = node;
		int rnode = node;
		for (; (lnode!=l || rnode!=u) && step>=1; step/=2) {
			if (l<lnode){
				lnode-=step;
			}
			else if (lnode<l) {
				getPtr(lnode,left);
				lnode+=step;
			}
			if (u<rnode){
				getPtr(rnode,right);
				rnode-=step;
			}
			else if (rnode<u) {
				rnode+=step;
			}
		}
		
		getAllPtr(l,u,all);
		
		
		return node;
	}

	private final static int SIZE = 12;
	private void getAllPtr(int l, int u, Set<Long> all) throws IOException {
		long low = 0;
        long high = nodes.size()/SIZE - 1;
        while (low <= high) {
        	long mid = (low + high) >>> 1;
			int midVal = nodes.getInt(mid*SIZE);
            int cmp = Integer.compare(midVal, l);

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else {
                // key found
            	low = mid;
                break;
            }
        }
        while (true) {
        	if (low*SIZE>=nodes.size()) return;
        	nodes.position(low++*SIZE);
        	int n = nodes.getInt();
        	if (n>u) return;
        	long lo = nodes.getLong();
        	all.add(lo);
        }
	}

	private void getPtr(int node, Set<Long> set) throws IOException {
		
		long low = 0;
        long high = nodes.size()/SIZE - 1;

        while (low <= high) {
            long mid = (low + high) >>> 1;
			int midVal = nodes.getInt(mid*SIZE);
            int cmp = Integer.compare(midVal, node);

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else {
                long ptr = nodes.getLong(mid*SIZE+Integer.BYTES); // key found
                set.add(ptr);
                return;
            }
        }
        
	}

	
}
