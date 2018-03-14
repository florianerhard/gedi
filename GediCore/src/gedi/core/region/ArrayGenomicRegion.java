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
package gedi.core.region;

import gedi.util.ArrayUtils;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.datastructure.tree.redblacktree.Interval;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;

import java.io.IOException;


public class ArrayGenomicRegion implements GenomicRegion, BinarySerializable {
	
	
	private int[] coords;
	private int hash;
	
	public ArrayGenomicRegion(int... coords) {
		if (coords.length%2!=0) throw new IllegalArgumentException();
		this.coords = normalize(coords);
//		if (this.coords.length==0)
//			System.out.println();
	}
		
	public ArrayGenomicRegion(IntArrayList coords) {
		this.coords = normalize(coords);
//		if (this.coords.length==0)
//			System.out.println();
	}
	
	public ArrayGenomicRegion(GenomicRegion copy) {
		this.coords = new int[copy.getNumParts()*2];
		for (int i=0; i<copy.getNumParts(); i++) {
			coords[i*2] = copy.getStart(i);
			coords[i*2+1] = copy.getEnd(i);
		}
	}
	
	public ArrayGenomicRegion(Interval interval) {
		this.coords = new int[]{interval.getStart(),interval.getEnd()};
	}
	
	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putCInt(coords.length/2);
		for (int i=0; i<coords.length; i++)
			out.putCInt(i==0?coords[i]:(coords[i]-coords[i-1]));
	}
	
	@Override
	public void deserialize(BinaryReader in) throws IOException {
		coords = new int[in.getCInt()*2];
		for (int i=0; i<coords.length; i++)
			coords[i] = i==0?in.getCInt():(coords[i-1]+in.getCInt());
	}
	
	@Override
	public ArrayGenomicRegion toArrayGenomicRegion() {
		return new ArrayGenomicRegion(this);
	}
	
	/**
	 * Gets the internal array. Use this only if you really know what you are doing!
	 * 
	 * @return
	 */
	public int[] getCoords() {
		return coords;
	}
	
	@Override
	public String toString() {
		return toString("|");
	}
	@Override
	public int hashCode() {
		if (hash==0) 
			hash = ArrayUtils.hashCode(coords);
		return hash;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof GenomicRegion))
			return false;
		GenomicRegion c = (GenomicRegion) obj;
		
		return compareTo(c)==0;
	}
	
	private static int[] normalize(IntArrayList in) {
		if ((in.size()&1)==1) throw new RuntimeException("Number of positions not even!");
		IntArrayList c = new IntArrayList(in.size());
		for (int i=0; i<in.size(); i+=2)
			if (in.getInt(i)<in.getInt(i+1)) {
				c.add(in.getInt(i));
				c.add(in.getInt(i+1));
			}
		int[] coords = c.toIntArray();
		c.clear();
		if (coords.length>0) {
			c.add(coords[0]);
			for (int i=2; i<coords.length; i+=2)
				if (coords[i-1]<coords[i]) {
					c.add(coords[i-1]);
					c.add(coords[i]);
				}
			c.add(coords[coords.length-1]);
			coords = c.toIntArray();
		}
		return coords;
	}
	
	private static int[] normalize(int[] in) {
		if ((in.length&1)==1) throw new RuntimeException("Number of positions not even!");
		if (ArrayUtils.isStrictAscending(in)) return in;
		
		
		IntArrayList c = new IntArrayList(in.length);
		for (int i=0; i<in.length; i+=2)
			if (in[i]<in[i+1]) {
				c.add(in[i]);
				c.add(in[i+1]);
			}
		int[] coords = c.toIntArray();
		c.clear();
		if (coords.length>0) {
			c.add(coords[0]);
			for (int i=2; i<coords.length; i+=2)
				if (coords[i-1]<coords[i]) {
					c.add(coords[i-1]);
					c.add(coords[i]);
				}
			c.add(coords[coords.length-1]);
			coords = c.toIntArray();
		}
		return coords;
	}


	@Override
	public int getNumParts() {
		return coords.length/2;
	}

	@Override
	public int getStart(int part) {
		return coords[part<<1];
	}

	@Override
	public int getEnd(int part) {
		return coords[(part<<1)|1];
	}


}
