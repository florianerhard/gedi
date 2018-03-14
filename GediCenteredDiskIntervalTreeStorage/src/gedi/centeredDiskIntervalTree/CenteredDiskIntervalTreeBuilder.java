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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.util.GeneralUtils;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;
import gedi.util.mutable.MutablePair;

public class CenteredDiskIntervalTreeBuilder<D> {

	
	private HashMap<Integer,MutablePair<ArrayList<CenteredDiskIntervalTreeNode>, ArrayList<CenteredDiskIntervalTreeNode>>> nodes;
	private int max = 0;
	private boolean appendData;
	private String magic;
	
	private String prefix;
	private String tmpFolder;
	
	private ArrayList<String> tmps = new ArrayList<String>();
	
	private int count;
	
	public CenteredDiskIntervalTreeBuilder(boolean appendData, String magic, String prefix, String tmpFolder) {
		nodes = new HashMap<Integer, MutablePair<ArrayList<CenteredDiskIntervalTreeNode>, ArrayList<CenteredDiskIntervalTreeNode>>>();
		this.magic = magic;
		this.appendData = appendData;
		
		this.prefix = prefix;
		this.tmpFolder = tmpFolder;
	}
	
	
	public void add(GenomicRegion region, long ptr) throws IOException {
		for (int p=0; p<region.getNumParts(); p++) {
			int node = computeNode(region.getStart(p), region.getStop(p));
			MutablePair<ArrayList<CenteredDiskIntervalTreeNode>, ArrayList<CenteredDiskIntervalTreeNode>> list = nodes.get(node);
			if (list==null) nodes.put(node, list = new MutablePair<ArrayList<CenteredDiskIntervalTreeNode>, ArrayList<CenteredDiskIntervalTreeNode>>(
					new ArrayList<CenteredDiskIntervalTreeNode>(),
					new ArrayList<CenteredDiskIntervalTreeNode>()
					));
			
			CenteredDiskIntervalTreeNode n1 = new CenteredDiskIntervalTreeNode(region.getStop(p), ptr);
			CenteredDiskIntervalTreeNode n2 = new CenteredDiskIntervalTreeNode(region.getStart(p), ptr);
			list.Item1.add(n1);
			list.Item2.add(n2);
			
		}
		count++;
	}
	
	public void toDisk() throws IOException {
		String fn = File.createTempFile(prefix+".DISK", ".data", new File(tmpFolder)).getPath();
		tmps.add(fn);
		PageFileWriter tmp = new PageFileWriter(fn);
		for (Integer k : nodes.keySet()) {
			tmp.putCInt(k);
			MutablePair<ArrayList<CenteredDiskIntervalTreeNode>, ArrayList<CenteredDiskIntervalTreeNode>> p = nodes.get(k);
			tmp.putCInt(p.Item1.size());
			for (CenteredDiskIntervalTreeNode n : p.Item1) 
				n.serialize(tmp);
			for (CenteredDiskIntervalTreeNode n : p.Item2) 
				n.serialize(tmp);
		}
		tmp.close();
		nodes.clear();
	}
	
	public void fromDisk() throws IOException {
		for (String t : tmps) {
			PageFile tmp = new PageFile(t);
			
			while (!tmp.eof()) {
				int k = tmp.getCInt();
				int s = tmp.getCInt();
				
				MutablePair<ArrayList<CenteredDiskIntervalTreeNode>, ArrayList<CenteredDiskIntervalTreeNode>> list = nodes.get(k);
				if (list==null) nodes.put(k, list = new MutablePair<ArrayList<CenteredDiskIntervalTreeNode>, ArrayList<CenteredDiskIntervalTreeNode>>(
						new ArrayList<CenteredDiskIntervalTreeNode>(),
						new ArrayList<CenteredDiskIntervalTreeNode>()
						));
				
				for (int i=0; i<s; i++) {
					CenteredDiskIntervalTreeNode n = new CenteredDiskIntervalTreeNode();
					n.deserialize(tmp);
					list.Item1.add(n);
				}
				
				for (int i=0; i<s; i++) {
					CenteredDiskIntervalTreeNode n = new CenteredDiskIntervalTreeNode();
					n.deserialize(tmp);
					list.Item2.add(n);
				}
			}
			tmp.close();
			new File(t).delete();
		}
		
	}
	
	
	
	public CenteredDiskIntervalTreeBuilder<D> build(String outpath) throws IOException {
		PageFileWriter out = new PageFileWriter(outpath);
		build(out);
		out.close();
		return this;
	}
	
	/** 
	 * Probably in the middle of the file!
	 * @param out
	 * @return
	 * @throws IOException
	 */
	public CenteredDiskIntervalTreeBuilder<D> build(PageFileWriter out) throws IOException {
		fromDisk();
		
		long start = out.position();
		
//		System.out.println("@"+out.getStart()+"+"+start);
//		System.out.println(magic);
//		System.out.println(nodes.size());
//		System.out.println(count);
//		System.out.println(max);
//		System.out.println("@"+out.getStart()+"+"+out.position());
		
		// header
		out.putAsciiChars(magic);
		out.putInt(nodes.size());
		out.putInt(count);
		out.putInt(max);
		long space = out.position();
		out.putLong(0); // reserve space for pointer to lists begin
		out.putLong(0); // reserve space for pointer to data begin
		
		Integer[] sort = nodes.keySet().toArray(new Integer[0]);
		Arrays.sort(sort);
		
//		System.out.println("nodes:"+out.position()+" logical:"+(out.position()-start));
		
		
		// first, write node lists to tmp file to determine its size:
		PageFileWriter tmpLists = out.createTempWriter(".tmplists");
		
		for (int i=0; i<sort.length; i++) {
			int node = sort[i];
			MutablePair<ArrayList<CenteredDiskIntervalTreeNode>, ArrayList<CenteredDiskIntervalTreeNode>> list = nodes.get(node);
			list.Item1.sort((a,b)->Integer.compare(b.getNode(),a.getNode()));
			list.Item2.sort((a,b)->Integer.compare(a.getNode(),b.getNode()));
			
			out.putInt(node);
			out.putLong(tmpLists.position());
			
//			System.out.println("Node: "+node+"\t"+tmpLists.position());
			
//			System.out.print("List: "+list.Item1.size()+" "+list.Item2.size()+"\n");
			tmpLists.putCInt(list.Item1.size());
			
			for (int l=0; l<list.Item1.size(); l++) {
				CenteredDiskIntervalTreeNode p1 = list.Item1.get(l);
				CenteredDiskIntervalTreeNode p2 = list.Item2.get(l);
				p1.serialize(tmpLists);
				p2.serialize(tmpLists);
//				System.out.print(" "+p1+","+p2);
			}
//			System.out.println();
		}
		tmpLists.close();
		
		nodes.clear();
		
//		System.out.println("lists:"+out.position()+" logical:"+(out.position()-start));
		
		long listBegin = out.position();
		PageFile lists = tmpLists.read(true);
		while (!lists.eof()) {
			out.put(lists.get());
		}
		lists.close();
//		System.err.println("deleting "+lists.getPath());
		new File (lists.getPath()).delete();
//		System.out.println("data:"+out.position()+" logical:"+(out.position()-start));
		long cont = out.position();
		out.position(space);
		out.putLong(listBegin-start);
		out.putLong(cont-start);
		out.position(cont);
		return this;
	}
	
	
	private CenteredDiskIntervalTreeBuilder<D> buildOld(PageFileWriter out) throws IOException {
		
		long start = out.position();
		
		// header
		out.putAsciiChars(magic);
		out.putInt(nodes.size());
		out.putInt(count);
		out.putInt(max);
		long space = out.position();
		out.putLong(0); // reserve space for pointer to data begin
		
		// nodes
		Integer[] sort = nodes.keySet().toArray(new Integer[0]);
		Arrays.sort(sort);
		
//		System.out.println(start);
		long offset = out.position()+sort.length*(Integer.BYTES+Long.BYTES);
		
		for (int i=0; i<sort.length; i++) {
			int node = sort[i];
			MutablePair<ArrayList<CenteredDiskIntervalTreeNode>, ArrayList<CenteredDiskIntervalTreeNode>> list = nodes.get(node);
			list.Item1.sort((a,b)->Integer.compare(b.getNode(),a.getNode()));
			list.Item2.sort((a,b)->Integer.compare(a.getNode(),b.getNode()));
			
			out.putInt(node);
			out.putLong(offset-start);
//			System.out.println(node+"\t"+(offset-start));
			
			int entry = list.Item1.size()*(Integer.BYTES+Long.BYTES)*2+Short.BYTES;
			offset += entry;
		}
		
		for (int i=0; i<sort.length; i++) {
			int node = sort[i];
			MutablePair<ArrayList<CenteredDiskIntervalTreeNode>, ArrayList<CenteredDiskIntervalTreeNode>> list = nodes.get(node);
			out.putShort(GeneralUtils.checkedIntToShort(list.Item1.size()));
			
			for (CenteredDiskIntervalTreeNode p : list.Item1) {
				p.ptr-=start;
				p.serialize(out);
			}
			for (CenteredDiskIntervalTreeNode p : list.Item2) {
				p.ptr-=start;
				p.serialize(out);
			}
//			System.out.println(node);
//			System.out.println(list.Item1);
//			System.out.println(list.Item2);
			
			// restore, if intended to be reused!
			for (CenteredDiskIntervalTreeNode p : list.Item1) {
				p.ptr+=start;
			}
			for (CenteredDiskIntervalTreeNode p : list.Item2) {
				p.ptr+=start;
			}
		}
		
		if (appendData) {
			long cont = out.position();
			out.position(space);
			out.putLong(cont);
			out.position(cont);
		}
		return this;
	}
	
	
	
	private int computeNode(int l, int u) {
		if (u>max) max = (int) Math.pow(2, Math.ceil(Math.log(u)/Math.log(2)));
		
		int node = max/2;
		
		for (int step=Math.abs(node/2); step>=1; step/=2) {
			if (u<node)node-=step;
			else if (node<l)node+=step;
			else break;
		}
		return node;
	}


	
}
