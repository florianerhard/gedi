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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import gedi.core.data.reads.AlignedReadsData;
import gedi.util.StringUtils;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.datastructure.tree.redblacktree.Interval;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;

public interface GenomicRegion extends Interval, Comparable<GenomicRegion>, Iterable<GenomicRegionPart> {

	int getNumParts();
	int getStart(int part);
	int getEnd(int part);
	
	default int getStart() {
		return getStart(0);
	}
	
	default ArrayGenomicRegion toArrayGenomicRegion() {
		return new ArrayGenomicRegion(this);
	}
	
	@Override
	default GenomicRegion asRegion() {
		return this;
	}
	
	default int getStop(int p) {
		return getEnd(p)-1;
	}
	
	default int getStop() {
		return getEnd(getNumParts()-1)-1;
	}

	default GenomicRegionPart getPart(int part) {
		return new GenomicRegionPart(part, this);
	}
	
	default List<GenomicRegionPart> getParts() {
		ArrayList<GenomicRegionPart> re = new ArrayList<GenomicRegionPart>();
		for (int i=0; i<getNumParts(); i++)
			re.add(getPart(i));
		return re;
	}
	default int getEnd() {
		return getEnd(getNumParts()-1);
	}

	default boolean isEmpty() {
		return getNumParts()==0 || getTotalLength()==0;
	}

	default int getPartIndex() {
		return 0;
	}

	default GenomicRegion getGenomicRegion() {
		return this;
	}
	
	default int defaultHashCode() {
        int result = 1;
        for (int i=0; i<getNumBoundaries(); i++)
            result = 31 * result + getBoundary(i);
        return result;
	}
	
	default int[] getBoundaries() {
		int[] re = new int[getNumBoundaries()];
		for (int i=0; i<re.length; i++)
			re[i] = getBoundary(i);
		return re;
	}
	
	/**
	 * if bpos mod 2 = 0, returns getStart(bpos/2);, getEnd(bpos/2) otherwise;
	 * @param bpos
	 */
	default int getBoundary(int bpos) {
		return (bpos&1)==0?getStart(bpos>>>1):getEnd(bpos>>>1);
	}

	default int getNumBoundaries() {
		return getNumParts()*2;
	}
	
	/**
	 * Gets the position within the (imaginary) array, that holds alternating start and end positions
	 * @param pos
	 * @return
	 */
	default int binarySearch(int pos) {
		int low = 0;
		int high = getNumParts()*2-1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			int midVal = getBoundary(mid);

			if (midVal < pos)
				low = mid + 1;
			else if (midVal > pos)
				high = mid - 1;
			else
				return mid; // key found
		}
		return -(low + 1);  // key not found.
	}

	default boolean isIntronic(int p) {
		return p>=getStart() && p<getEnd() && !contains(p);
	}
	
	default boolean contains(int pos) {
		if (getNumParts()==1) return pos>=getStart() && pos<getEnd();
		int i = binarySearch(pos);
		if (i>=0) return (i%2)==0; // hit start position
		i = -i-1;
		return (i%2)==1; // between start and end
	}

	default GenomicRegionPart getEnclosingPart(int pos) {
		int i = binarySearch(pos);
		if (i>=0) {// hit start position
			if ((i%2)!=0) 
				return null;
			return getPart(i/2);
		}
		i = -i-1;
		if ((i%2)!=1) // between start and end
			return null;
		return getPart(i/2);
	}

	/**
	 * if not, returns a negative number; -1 means before, -2 means in first intron,...
	 * @param pos
	 * @return
	 */
	default int getEnclosingPartIndex(int pos) {
		if (getNumParts()==1) {
			if (pos<getStart()) return -1;
			if (pos<getEnd()) return 0;
			return -2;
		}
		int i = binarySearch(pos);
		if (i>=0) {// hit start position
			if ((i%2)!=0) 
				return -i/2-2;
			return i/2;
		}
		i = -i-1;
		if ((i%2)!=1) // between start and end
			return -i/2-1;
		return i/2;
	}

	default boolean contains(GenomicRegion co) {
		int c = 0;
		int len = this.getNumParts()*2;
		int colen = co.getNumParts()*2;
		for (int t=0; t<len && c<colen; c+=2) {
			for (;t<len && getBoundary(t+1)<co.getBoundary(c+1); t+=2);
			if (t>=len) return false;
			if (co.getBoundary(c)<getBoundary(t)) return false;
		}
		if (c<colen)
			return false;
		return true;
	}

	/**
	 * Coordinate inner boundaries must exactly match!
	 * @param co
	 * @return
	 */
	default boolean containsUnspliced(GenomicRegion co) {
		return contains(co) && isIntronConsistent(co);//induce(co).getNumParts()==1;
	}
	
	default boolean containsUnspliced(GenomicRegion[] co) {
		for (GenomicRegion r : co)
			if (!containsUnspliced(r))
				return false;
		return true;
	}


	default boolean isSingleton() {
		return getNumParts()==1;
	}


	default int getLength(int part) {
		return getEnd(part)-getStart(part);
	}
	default int getIntronLength(int upstreamPart) {
		return getStart(upstreamPart+1)-getEnd(upstreamPart);
	}

	default int getTotalLength() {
		int re = 0;
		for (int i=0; i<getNumParts(); i++)
			re+=getLength(i);
		return re;
	}
	default String toRegionString() {
		return toString("|");
	}
	default String toString(String sep) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<getNumParts(); i++) {
			if (i>0)
				sb.append(sep);
			sb.append(getStart(i));
			sb.append('-');
			sb.append(getEnd(i));
		}
		return sb.toString();
	}
	
	default String toString(AlignedReadsData ard) {
		StringBuilder sb = new StringBuilder();
		int l=0;
		for (int i=0; i<getNumParts(); i++) {
			if (i>0)
				sb.append(ard.isReadPairGap(l, 0)?"#":"|");
			sb.append(getStart(i));
			sb.append('-');
			sb.append(getEnd(i));
			l+=getLength(i);
		}
		return sb.toString();
	}
	
	/**
	 * Two regions itersect, if any of the parts intersect
	 * @param co
	 * @return
	 */
	default boolean intersects(GenomicRegion co) {
		if (co.getNumParts()==1) return intersects(co.getStart(),co.getEnd());
		
		int len = getNumParts()*2;
		int colen = co.getNumParts()*2;
		int t = 0;
		int c = 0;
		while (t<len && c<colen) {
			int is = Math.max(getBoundary(t),co.getBoundary(c));
			int ie = Math.min(getBoundary(t+1),co.getBoundary(c+1));
			if (is<ie) {
				return true;
			}
			if (getBoundary(t+1)<co.getBoundary(c+1))
				t+=2;
			else
				c+=2;
		}
		return false;
	}


	default boolean intersects(int start, int end) {
		int si=getEnclosingPartIndex(start);
		if (si>=0) return true;
		int ei=getEnclosingPartIndex(end-1);
		return ei>=0 || si!=ei;
	}


	default String getSizes(char sep) {
		return StringUtils.concat(String.valueOf(sep), getLengths());
	}

	default String getEnds(char sep, boolean relative) {
		StringBuilder sb = new StringBuilder();
		for (int i=1; i<getNumParts()*2; i+=2) {
			if (i>1) sb.append(sep);
			sb.append(relative?getBoundary(i)-getStart():getBoundary(i));
		}
		return sb.toString();
	}

	default String getStarts(char sep, boolean relative) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<getNumParts()*2; i+=2) {
			if (i>1) sb.append(sep);
			sb.append(relative?getBoundary(i)-getStart():getBoundary(i));
		}
		return sb.toString();
	}

	default int[] getLengths() {
		int[] re = new int[getNumParts()];
		for (int i=0; i<re.length; i++) {
			re[i] = getLength(i);
		}
		return re;
	}

	default int[] getEnds() {
		int[] re = new int[getNumParts()];
		for (int i=0; i<re.length; i++) {
			re[i] = getEnd(i);
		}
		return re;
	}

	default int[] getStarts() {
		int[] re = new int[getNumParts()];
		for (int i=0; i<re.length; i++) {
			re[i] = getStart(i);
		}
		return re;
	}

	default int[] getStartsRelative() {
		int[] re = new int[getNumParts()];
		for (int i=1; i<re.length; i++) {
			re[i] = getStart(i)-getStart(0);
		}
		return re;
	}

	
	/**
	 * Coord is given with respect to the induced coordinate system (i.e. if this is 0-10|20-30, the induced 
	 * coordinate system is 0-20). They are transformed to the coordinate system of this (i.e. if coord=5-15, it
	 * will become 5-10|20-25)
	 * Does this inplace and returns coord!
	 * @param coord
	 */
	default int map(int coord) {
		int len = getNumParts();
		if (coord<0)
			throw new IllegalArgumentException("coord <0!");
		for (int i=0; i<len; i++) {
			coord-=getLength(i);
			if (coord<0) 
				return getEnd(i)+coord;
		}
		throw new IllegalArgumentException("coord is to long!");		
	}

	default int mapMaybeOutside(int coord) {
		if (coord<0)
			return getStart()+coord;
		if (coord>=getTotalLength())
			return getEnd()+coord-getTotalLength();
		
		return map(coord);
	}
	
	/**
	 * Coord are given w.r.t. to the same coordinates system as this. Returns the fraction of positions covered
	 * by coord of all positions of this.
	 * @param coord
	 * @return
	 */
	default double getCoverage(GenomicRegion coord) {
		return induce(coord).getTotalLength()/(double)getTotalLength();
	}

	/**
	 * Two regions are intron consistent, if no base contained in one is in an intron of the other
	 * i.e. they may be orthogonal or even disjunct!
	 * @param co
	 * @return
	 */
	default boolean isIntronConsistent(GenomicRegion co) {
		return !intersects(co.invert()) && !co.intersects(invert());
	}


	/**
	 * Format: s1-e1|...|sn-en
	 * @param s
	 * @return
	 */
	public static ArrayGenomicRegion parse(String s) {
		if (s==null || s.length()==0) return null;
		String[] p = new String[2];
		String[] pairs = StringUtils.split(s, '|');
		if (pairs.length==1 && StringUtils.isInt(pairs[0])) {
			int po = Integer.parseInt(pairs[0]);
			return new ArrayGenomicRegion(po,po+1);
		}
		IntArrayList re = new IntArrayList(2*pairs.length);
		for(int i=0; i<pairs.length; i++) {
			StringUtils.split(pairs[i], '-',p);
			if (p[1]==null || !StringUtils.isInt(p[0]) || !StringUtils.isInt(p[1])) return null;
			re.add(Integer.parseInt(p[0].replace(",", "")));
			re.add(Integer.parseInt(p[1].replace(",", "")));
		}
		return new ArrayGenomicRegion(re);
	}


	/**
	 * Returns a new object!
	 * @param co
	 * @return
	 */
	default ArrayGenomicRegion union(GenomicRegion co) {
		int len = getNumParts()*2;
		int colen = co.getNumParts()*2;
		
		IntArrayList re = new IntArrayList(len+colen);
		int t = 0;
		int c = 0;
		while (t<len && c<colen) {
			int is = Math.min(getBoundary(t),co.getBoundary(c));
			int ie = getBoundary(t)==is?getBoundary(t+1):co.getBoundary(c+1);
			while (t<len && c<colen && Math.max(getBoundary(t), co.getBoundary(c))<Math.min(getBoundary(t+1), co.getBoundary(c+1))) {// overlapping 
				ie = Math.max(getBoundary(t+1), co.getBoundary(c+1));
				boolean tp = ie==getBoundary(t+1);
				boolean cp = ie==co.getBoundary(c+1);
				if (tp) 
					c+=2;
				if (cp) 
					t+=2;
				if (tp&&cp) break;
			}
			re.add(is); re.add(ie);
			if (t<len && getBoundary(t)<ie)
				t+=2;
			if (c<colen && co.getBoundary(c)<ie)
				c+=2;
		}
		while (t<len) {
			re.add(getBoundary(t++));
			re.add(getBoundary(t++)); 
		}
		while (c<colen) {
			re.add(co.getBoundary(c++));
			re.add(co.getBoundary(c++)); 
		}
		return new ArrayGenomicRegion(re);
	}

	/**
	 * Returns a new object!
	 * @param co
	 * @return
	 */
	default ArrayGenomicRegion intersect(GenomicRegion co) {
		int len = getNumParts()*2;
		int colen = co.getNumParts()*2;
		IntArrayList re = new IntArrayList(getNumParts()*2);
		int t = 0;
		int c = 0;
		while (t<len && c<colen) {
			int is = Math.max(getBoundary(t),co.getBoundary(c));
			int ie = Math.min(getBoundary(t+1),co.getBoundary(c+1));
			if (is<ie) {
				re.add(is); re.add(ie);
			}
			if (getBoundary(t+1)<co.getBoundary(c+1))
				t+=2;
			else
				c+=2;
		}
		return new ArrayGenomicRegion(re);
	}
	
	/**
	 * Returns new object!
	 * @param co
	 * @return
	 */
	default ArrayGenomicRegion subtract(GenomicRegion co) {
		int len = getNumParts()*2;
		int colen = co.getNumParts()*2;
		IntArrayList re = new IntArrayList(len);
		int t = 0;
		int c = 0;
		int s = Integer.MIN_VALUE;
		while (t<len && c<colen) {
			int is = Math.max(getBoundary(t),co.getBoundary(c));
			int ie = Math.min(getBoundary(t+1),co.getBoundary(c+1));
			if (is<ie) {
				if (s==Integer.MIN_VALUE && getBoundary(t)<co.getBoundary(c)) s= getBoundary(t);
				if (s!=Integer.MIN_VALUE) {
					re.add(s); re.add(is);
				}
				s = ie;
				if (getBoundary(t+1)<co.getBoundary(c+1)) 
					s=Integer.MIN_VALUE;
			} else if (getBoundary(t+1)<co.getBoundary(c+1)) {
				re.add(s==Integer.MIN_VALUE?getBoundary(t):s);
				re.add(getBoundary(t+1));
				s = Integer.MIN_VALUE;
			}

			if (getBoundary(t+1)<co.getBoundary(c+1))
				t+=2;
			else
				c+=2;
		}
		for (; t<len; t+=2) {
			re.add(s==Integer.MIN_VALUE?getBoundary(t):s);
			re.add(getBoundary(t+1));
			s=Integer.MIN_VALUE;
		}
		return new ArrayGenomicRegion(re);
	}



	/**
	 * Coord are given with respect to the induced coordinate system (i.e. if this is 0-10|20-30, the induced 
	 * coordinate system is 0-20). They are transformed to the coordinate system of this (i.e. if coord=5-15, it
	 * will become 5-10|20-25)
	 * @param coord
	 */
	default ArrayGenomicRegion map(GenomicRegion coord) {
		if (coord.getTotalLength()==0) return new ArrayGenomicRegion();
		if (getTotalLength()<coord.getEnd())
			throw new IllegalArgumentException("coords are to long!");

		int len = getNumParts()*2;
		int colen = coord.getNumParts()*2;
		IntArrayList create = new IntArrayList(colen*2);
		int p = 0;
		int j = 0;
		for (int i=0; i<len && j<colen; i+=2) {
			int np = p+getBoundary(i+1)-getBoundary(i);
			while (j<colen && np>coord.getBoundary(j)) {
				create.add(getBoundary(i)+coord.getBoundary(j)-p);
				while (j+1<colen && np<coord.getBoundary(j+1)) {
					create.add(getBoundary(i+1));
					i+=2;
					create.add(getBoundary(i));
					p = np;
					np = p+getBoundary(i+1)-getBoundary(i);
				}
				create.add(getBoundary(i)+coord.getBoundary(j+1)-p);
				j+=2;
			}
			p = np;
		}
		return new ArrayGenomicRegion(create);
	}
	
	default GenomicRegion mapMaybeOutside(GenomicRegion coord) {
		if (coord.getStart()>=0 && coord.getEnd()<=getTotalLength()) return map(coord);
		ArrayGenomicRegion before = coord.intersect(new ArrayGenomicRegion(coord.getStart(),0));
		ArrayGenomicRegion after = coord.intersect(new ArrayGenomicRegion(getTotalLength(),coord.getEnd()));
		ArrayGenomicRegion in = coord.intersect(new ArrayGenomicRegion(0,getTotalLength()));
		return before.translate(getStart()).union(map(in)).union(after.translate(getEnd()-getTotalLength()));
	}

	/**
	 * Coord are given w.r.t. to the same coordinate system as this. They are mapped to the coordinate
	 * system induced by this (if coord is not contained in this, the intersection is calculated beforehand!)
	 * @param coord
	 */
	default ArrayGenomicRegion induce(GenomicRegion coord) {
		int len = getNumParts()*2;
		int colen = coord.getNumParts()*2;
		IntArrayList re = new IntArrayList(len);
		int t = 0;
		int c = 0;
		int p = 0;
		while (t<len && c<colen) {
			int is = Math.max(getBoundary(t),coord.getBoundary(c));
			int ie = Math.min(getBoundary(t+1),coord.getBoundary(c+1));
			if (is<ie) {
				re.add(p+is-getBoundary(t)); re.add(p+ie-getBoundary(t));
			}
			if (getBoundary(t+1)<coord.getBoundary(c+1)) {
				p+=getBoundary(t+1)-getBoundary(t);
				t+=2;
			} else
				c+=2;

		}
		return new ArrayGenomicRegion(re);
	}
	
	/**
	 * Same as induce, but pos may be before start or after stop of this.
	 * @param pos
	 * @return
	 */
	default int induceMaybeOutside(int pos) {
		if (pos<getStart()) //before start -> negative position
			return pos-getStart();
		if (pos>getStop())
			return getTotalLength()+pos-getEnd();
		if (!contains(pos)) {
			throw new IllegalArgumentException("Position is not contained!");
		}
		return induce(pos);
	}

	/**
	 * Coord are given w.r.t. to the same coordinate system as this. They are mapped to the coordinate
	 * system induced by this (if coord is not contain in this, the intersection is calculated beforehand!)
	 * Does this inplace and returns coord!
	 * @param coord
	 */
	default int induce(int pos) {
		if (!contains(pos))
			throw new IllegalArgumentException("Position is not contained!");
		int re = 0;
		for (int i=0; i<getNumParts(); i++) {
			if (getEnd(i)>pos) return re+pos-getStart(i);
			re+=getLength(i);
		}
		throw new IllegalArgumentException("Position is not contained!");
	}


	default int getLengthBefore(int pos, boolean inclusive) {
		return induce(pos)+(inclusive?1:0);
	}

	default int getLengthAfter(int pos, boolean inclusive) {
		return getTotalLength()-1-induce(pos)+(inclusive?1:0);
	}


	@Override
	default int compareTo(GenomicRegion o) {
		if (isEmpty()&&o.isEmpty()) return 0;
		if (isEmpty()) return -1;
		if (o.isEmpty()) return 1;
		
		int re = Integer.compare(getStart(),o.getStart());
		if (re==0) re = Integer.compare(getStop(),o.getStop());
		if (re==0) {
			int n = Math.min(getNumParts(),o.getNumParts())*2;
			for (int i=0; i<n; i++) {
				int r = Integer.compare(getBoundary(i),o.getBoundary(i));
				if (r!=0)
					return r;
			}
			re = getNumParts()-o.getNumParts();
		}
		return re;
	}

	/**
	 * Extends the first and last part by front and end, respectively
	 * @param front
	 * @param end
	 * @return
	 */
	default GenomicRegion extendFront(int front) {
		if (front==0) return this;
		if (front<0) 
			return subtract(map(new ArrayGenomicRegion(0,-front)));
		else
			return union(new ArrayGenomicRegion(getStart()-front,getStart()));
	}
	
	/**
	 * Extends the first and last part by front and end, respectively
	 * @param front
	 * @param end
	 * @return
	 */
	default GenomicRegion extendBack(int end) {
		if (end==0) return this;
		if (end<0) 
			return subtract(map(new ArrayGenomicRegion(getTotalLength()+end,getTotalLength())));
		else
			return union(new ArrayGenomicRegion(getEnd(),getEnd()+end));
	}

	/**
	 * Moves these coordinates by p positions.
	 * @param p
	 */
	default GenomicRegion translate(int p) {
		if (p==0) return this;
		IntArrayList re = new IntArrayList(this.getNumParts()*2);
		for (int i=0; i<getNumParts(); i++) {
			re.add(getStart(i)+p);
			re.add(getEnd(i)+p);
		}
		return new ArrayGenomicRegion(re);
	}
	
	default GenomicRegion pep2dna() {
		IntArrayList re = new IntArrayList(this.getNumParts()*2);
		for (int i=0; i<getNumParts(); i++) {
			re.add(getStart(i)*3);
			re.add(getEnd(i)*3);
		}
		return new ArrayGenomicRegion(re);
	}
	
	default GenomicRegion dna2pep() {
		IntArrayList re = new IntArrayList(this.getNumParts()*2);
		for (int i=0; i<getNumParts(); i++) {
			re.add(getStart(i)/3);
			re.add(getEnd(i)/3);
		}
		return new ArrayGenomicRegion(re);
	}

	/**
	 * Extend all parts and normalizes.
	 * 
	 * @param p
	 */
	default ArrayGenomicRegion extendAll(int front, int end) {
		IntArrayList coords = new IntArrayList(getNumParts()*2);
		for (int i=0; i<getNumParts(); i++) {
			coords.add(getStart(i)-front);
			coords.add(getEnd(i)+end);
		}
		return new ArrayGenomicRegion(coords);
	}

	/**
	 * Gets the coordinates of all missing parts (e.g. introns)
	 * @return a new coordinates!
	 */
	default ArrayGenomicRegion invert() {
		if (getNumParts()==0) return new ArrayGenomicRegion();
		IntArrayList re = new IntArrayList((getNumParts()-1)*2);
		for (int i=0; i<getNumParts()-1; i++){
			re.add(getEnd(i));
			re.add(getStart(i+1));
		}
		return new ArrayGenomicRegion(re);
	}

	/**
	 * Gets the coordinates of all missing parts (e.g. introns)
	 * @return a new coordinates!
	 */
	default ArrayGenomicRegion invert(int outerStart, int outerEnd) {
		IntArrayList re = new IntArrayList((getNumParts()+1)*2);
		re.add(outerStart);
		if (!isEmpty()) {
			re.add(getStart());
			for (int i=0; i<getNumParts()-1; i++){
				re.add(getEnd(i));
				re.add(getStart(i+1));
			}
			re.add(getEnd());
		}
		re.add(outerEnd);
		return new ArrayGenomicRegion(re);
	}

	
	/**
	 * Reverts this coordinates for the given length.
	 * @param length
	 * @return
	 */
	default ArrayGenomicRegion reverse(int length) {
		IntArrayList coords = new IntArrayList(getNumParts()*2);
		for (int i=getNumParts()-1; i>=0; i--) {
			coords.add(length-getEnd(i));
			coords.add(length-getStart(i));
		}
		return new ArrayGenomicRegion(coords);
	}

	/**
	 * Extracts part of the given sequence corresponding to this.
	 * @param s
	 * @return
	 */
	default String extractSequence(String s) {
		StringBuilder sb = new StringBuilder();
		for (int p=0; p<getNumParts(); p++) 
			sb.append(s.substring(getStart(p),getEnd(p)));
		return sb.toString();
	}

	default double[] extractArray(double[] a) {
		double[] re = new double[getTotalLength()];
		int index = 0;
		for (int p=0; p<getNumParts(); p++) {
			System.arraycopy(a, getStart(p), re, index, getLength(p));
			index+=getLength(p);
		}
		return re;
	}
	
	default int[] extractArray(int[] a) {
		int[] re = new int[getTotalLength()];
		int index = 0;
		for (int p=0; p<getNumParts(); p++) {
			System.arraycopy(a, getStart(p), re, index, getLength(p));
			index+=getLength(p);
		}
		return re;
	}
	
	default <T> T[] extractArray(T[] a) {
		T[] re = (T[]) Array.newInstance(a.getClass().getComponentType(), getTotalLength());
		int index = 0;
		for (int p=0; p<getNumParts(); p++) {
			System.arraycopy(a, getStart(p), re, index, getLength(p));
			index+=getLength(p);
		}
		return re;
	}

	public static class StartPositionComparator implements Comparator<GenomicRegion> {

		@Override
		public int compare(GenomicRegion o1, GenomicRegion o2) {
			return o1.getStart()-o2.getStart();
		}
		
	}

	default ArrayGenomicRegion removeIntrons() {
		return new ArrayGenomicRegion(getStart(),getEnd());
	}

	
	default ExtendedIterator<GenomicRegionPart> iterator() {
		return new ExtendedIterator<GenomicRegionPart>() {
			int index = 0;
			@Override
			public boolean hasNext() {
				return index<getNumParts();
			}

			@Override
			public GenomicRegionPart next() {
				return getPart(index++);
			}
			
		};
	}
	
	
	/**
	 * if the start of o is left of this, its a negative number (induced on o)
	 * otherwise, it's a positive number (induced on this)
	 * @param o
	 * @return
	 */
	default int getStartDistance(GenomicRegion o) {
		if (o.getStart()<getStart())
			return -o.induceMaybeOutside(getStart());
		return induceMaybeOutside(o.getStart());
	}
	
	
	default int getDistance(GenomicRegion o) {
		if (intersects(o)) return 0;
		
		// identify the two numbers in here and o, that are closest!
		int min = Integer.MAX_VALUE;
		for (int i=0; i<getNumBoundaries(); i++) {
			int p = getBoundary(i);
			int oi = o.binarySearch(p);
			if (oi>=0) return 0; // end hit start
			oi = -oi-1;
			if (oi==o.getNumBoundaries()) oi--;
			if (oi<o.getNumBoundaries()-1 && o.getBoundary(oi+1)<o.getBoundary(oi))
				oi++;
			min = Math.min(min,Math.abs(p-o.getBoundary(oi)));
		}
		
		return min;
	}

	/**
	 * if the end of o is left of this, its a negative number (induced on this)
	 * otherwise, it's a positive number (induced on o)
	 * @param o
	 * @return
	 */
	default int getStopDistance(GenomicRegion o) {
		if (o.getStop()<getStop())
			return induceMaybeOutside(o.getStop())-getTotalLength();
		return o.getTotalLength()-o.induceMaybeOutside(getStop());
	}
	
	default GenomicRegion getIntron(int i) {
		return new ArrayGenomicRegion(getEnd(i),getStart(i+1));
	}
	
	default ExtendedIterator<GenomicRegion> introns() {
		return EI.seq(1, getNumParts()).map(i->getIntron(i-1));
	}

	
	


}
