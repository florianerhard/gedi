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
import gedi.util.ParseUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.mutable.MutableInteger;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

public class ContrastMapping {
	
	
	private int[][] merged; // contains original indices
	private int[] mapping; // contains mapped indices

	
	private HashMap<Integer,Integer> map = new HashMap<Integer,Integer>();
	private HashMap<Integer,String> namesMap = new HashMap<Integer,String>();
	private HashMap<String,Integer> namesMapInv = new HashMap<String,Integer>();
	
	
	public ContrastMapping() {
	}
	
	public ContrastMapping(String file) throws IOException {
		MutableInteger line = new MutableInteger();
		HashMap<String,Integer> mappedToIndex = new HashMap<String, Integer>();
		new LineOrientedFile(file).lineIterator()
			.skip(1)
			.map(a->StringUtils.split(a, '\t'))
			.forEachRemaining(a->addMapping(line.N++, mappedToIndex.computeIfAbsent(a[1],x->mappedToIndex.size()), a[1]));
	}
	
	public void setMerge(String ranges) {
		for (int i : ParseUtils.parseRangePositions(ranges, -1, new IntArrayList()).toIntArray())
			addMapping(i, 0);
	}
	
	public void setMergeAll(int n) {
		for (int i=0; i<n; i++)
			addMapping(i, 0);
	}
	
	public void addMapping(int original, int merged) {
		this.merged = null;
		this.mapping = null;
		this.merged = null;
		map.put(original, merged);
		
	}
	
	
	public void addMapping(int original, int merged, String mappedName) {
		addMapping(original,merged);
		namesMap.put(merged, mappedName);
		namesMapInv.put(mappedName, merged);
	}
	
	public void removeMapping(int original) {
		map.remove(original);
		this.merged = null;
	}
	
	public String getMappedName(int mapped) {
		return namesMap.get(mapped);
	}
	
	public int getMappedIndex(String name) {
		return namesMapInv.containsKey(name)?namesMapInv.get(name):-1;
	}
	
	public int getMappedIndexOrNext(String name) {
		return namesMapInv.containsKey(name)?namesMapInv.get(name):namesMapInv.size();
	}
	
	
	public void setBinaryContrast(String nameA, int[] a, String nameB, int[] b) {
		clear();
		for (int i:a)
			addMapping(i, 0,nameA);
		for (int i:b)
			addMapping(i, 1,nameB);
	}
	
	public void clear() {
		map.clear();
		namesMap.clear();
		this.merged = null;
		this.mapping = null;
		this.merged = null;
	}
	
	/**
	 * index is new index, returns all old indices that are mapping to it
	 * @param index
	 * @return
	 */
	public int[] getMergeConditions(int index) {
		if (merged==null) build();
		return merged[index];
	}
	
	public int getMappedIndex(int original) {
		if (mapping==null) build();
		if (original>=mapping.length) return -1;
		return mapping[original];
	}
	
	public int getNumMergedConditions() {
		if (merged==null) build();
		return merged.length;
	}
	
	public int getNumOriginalConditions() {
		if (mapping==null) build();
		return mapping.length;
	}

	public void build() {
		
		int maxIndex = -1;
		for (Integer i : map.keySet())
			maxIndex = Math.max(maxIndex,i);
		
		mapping = new int[maxIndex+1];
		Arrays.fill(mapping, -1);
		for (Integer i : map.keySet())
			mapping[i] = map.get(i);
		
		int max = mapping.length==0?-1:ArrayUtils.max(mapping);
		IntArrayList[] b = new IntArrayList[max+1];
		for (int i=0; i<b.length; i++)
			b[i] = new IntArrayList();
		for (int o=0; o<mapping.length; o++)
			if (mapping[o]!=-1)
				b[mapping[o]].add(o);
		
		merged = new int[max+1][];
		for (int i=0; i<merged.length; i++)
			merged[i] = b[i].toIntArray();
	}
	
	
	@Override
	public String toString() {
		if (merged==null) build();
		StringBuilder sb = new StringBuilder();
		sb.append("Original\tMapped\tName\n");
		for (int i=0; i<mapping.length; i++)
			sb.append(i).append("\t").append(mapping[i]).append("\t").append(namesMap.get(mapping[i])).append("\n");
		return sb.toString();
	}
	
}
