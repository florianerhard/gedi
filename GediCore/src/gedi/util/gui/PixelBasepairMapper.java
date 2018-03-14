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
package gedi.util.gui;

import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.util.datastructure.collections.intcollections.IntArrayList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.IntToDoubleFunction;



public class PixelBasepairMapper {


	public static class PixelBasePairRange {
		private ReferenceSequence reference;
		private int bpStart;
		private int bpStop;
		private double pixelStart;
		private double pixelStop;
		public PixelBasePairRange(ReferenceSequence reference, int bpStart,
				int bpStop, double pixelStart, double pixelStop) {
			super();
			this.reference = reference;
			this.bpStart = bpStart;
			this.bpStop = bpStop;
			this.pixelStart = pixelStart;
			this.pixelStop = pixelStop;
		}
		public ReferenceSequence getReference() {
			return reference;
		}
		public int getBpStart() {
			return bpStart;
		}
		public int getBpStop() {
			return bpStop;
		}
		public double getPixelStart() {
			return pixelStart;
		}
		public double getPixelStop() {
			return pixelStop;
		}
		public int bpCompare(int bp) {
			if (bp>=bpStart && bp<=bpStop) return 0;
			if (bp<bpStart) return 1;
			return -1;
		}
		public int pixelCompare(double pixel) {
			if (pixel>=pixelStart && pixel<=pixelStop) return 0;
			if (pixel<pixelStart) return 1;
			return -1;
		}
		public double getPixelPerBasepair() {
			return (pixelStop-pixelStart+1)/(bpStop-bpStart+1);
		}
		public int pixelToBp(double c) {
			double p = (c-pixelStart)/(pixelStop-pixelStart+1);
			return (int) (bpStart+p*(bpStop-bpStart+1));
		}
		public double bpToPixel(int c) {
			double p = ((double)c-bpStart)/(bpStop-bpStart+1);
			return pixelStart+p*(pixelStop-pixelStart+1);
		}

	}


	/**
	 * Always in genomic direction
	 */
	private PixelBasePairRange[] ranges = new PixelBasePairRange[0];
	
	private boolean dir5to3 = false;

	private HashMap<ReferenceSequence,PixelBasePairRange[]> map = new HashMap<ReferenceSequence, PixelBasePairRange[]>();

	private PixelBasepairMapper set(PixelBasePairRange[] ranges, boolean dir5to3) {
		this.ranges = ranges;
		this.dir5to3 = dir5to3;
		HashMap<ReferenceSequence, ArrayList<PixelBasePairRange>> mapc = new HashMap<ReferenceSequence,ArrayList<PixelBasePairRange>>();
		for (int i=0; i<ranges.length; i++) 
			mapc.computeIfAbsent(ranges[i].getReference(), r->new ArrayList<PixelBasePairRange>()).add(ranges[i]);

		map = new HashMap<ReferenceSequence, PixelBasePairRange[]>();
		for (ReferenceSequence ref : mapc.keySet()) 
			map.put(ref, mapc.get(ref).toArray(new PixelBasePairRange[0]));
		return this;
	}

	private double invert(double c) {
		return getWidth()-c;
	}
	
	private double checkInvert(ReferenceSequence reference, double c) {
		if (dir5to3 && reference.getStrand()==Strand.Minus)
			return invert(c);
		return c;
	}

	public double getWidth() {
		return ranges[ranges.length-1].pixelStop+1;
	}

	public PixelBasepairMapper setSimple(ReferenceSequence reference, boolean dir5to3, int start, int end, double width) {
		PixelBasePairRange[] ranges = new PixelBasePairRange[] {
				new PixelBasePairRange(reference,start,end-1,0,width)	
		};
		return set(ranges,dir5to3);
	}

	/**
	 * Exons are scaled according to length
	 * @param reference
	 * @param region
	 * @param intron
	 * @param width
	 * @return
	 */
	public PixelBasepairMapper setIntronFixed(ReferenceSequence reference, boolean dir5to3, GenomicRegion region, double intron, double width) {

		double exonBases = region.getTotalLength();

		IntToDoubleFunction exonFun = l-> (width-intron*(region.getNumParts()-1))/exonBases*l;
		IntToDoubleFunction intronFun = l-> intron;

		return setExonIntron(reference,dir5to3, region, exonFun, intronFun);
	}
	
	/**
	 * Exons are scaled according to length
	 * @param reference
	 * @param region
	 * @param intron
	 * @param width
	 * @return
	 */
	public PixelBasepairMapper setIntronFixed(ReferenceSequence[] reference, boolean dir5to3, GenomicRegion[] region, double intronSize, double width) {

		double intron = Math.max(intronSize, 0);
		
		long introns = 0;
		double exonBasesCreate = 0;
		for (int i=0; i<region.length; i++) {
			exonBasesCreate+=region[i].getTotalLength();
			introns += region[i].getNumParts()-1;
		}
		long intronsAndBetween = introns+reference.length-1;
		double exonBases = exonBasesCreate;
		IntToDoubleFunction exonFun = l-> (width-intron*(intronsAndBetween))/exonBases*l;
		IntToDoubleFunction intronFun = l-> intron;

		return setExonIntron(reference,dir5to3, region, exonFun, intronFun);
	}
	
	/**
	 * Exons/Introns are scaled according to length (different scale)
	 * intron=0.5 means that each intron basepair occupies half the size of an exon basepair
	 * @param reference
	 * @param region
	 * @param intron
	 * @param width
	 * @return
	 */
	public PixelBasepairMapper setIntronScaled(ReferenceSequence[] reference, boolean dir5to3, GenomicRegion[] region, double intron, double width) {

		long exons = 0;
		long introns = 0;
		for (int i=0; i<region.length; i++) {
			introns+=region[i].getEnd()-region[i].getStart()-region[i].getTotalLength();
			exons+=region[i].getTotalLength();
		}
		
		double exonWidth = ((double)exons/(exons+introns));
		double intronWidth = ((double)introns/(exons+introns))*intron;
		
		double e = width*exonWidth/(exonWidth+intronWidth);
		double i = width*intronWidth/(exonWidth+intronWidth);
		
		
		IntToDoubleFunction exonFun = l-> l*e;
		IntToDoubleFunction intronFun = l-> l*i;

		return setExonIntron(reference,dir5to3, region, exonFun, intronFun);
	}

	/**
	 * All exons have the same length
	 * @param reference
	 * @param region
	 * @param intron
	 * @param width
	 * @return
	 */
	public PixelBasepairMapper setExonIntronFixed(ReferenceSequence reference, boolean dir5to3, GenomicRegion region, double intron, double width) {


		IntToDoubleFunction exonFun = l-> (width-intron*(region.getNumParts()-1))/region.getNumParts();
		IntToDoubleFunction intronFun = l-> intron;

		return setExonIntron(reference,dir5to3, region, exonFun, intronFun);
	}

	public PixelBasepairMapper setExonIntronScaled(ReferenceSequence reference, boolean dir5to3, GenomicRegion region, double intron, double exon, double width) {

		double exonBases = region.getTotalLength();
		double intronBases = region.getEnd()-region.getStart()-exonBases;

		IntToDoubleFunction exonFun = l-> width/(exonBases+intronBases)*l*exon/(exon+intron);
		IntToDoubleFunction intronFun = l-> width/(exonBases+intronBases)*l*intron/(exon+intron);

		return setExonIntron(reference,dir5to3, region, exonFun, intronFun);
	}

	private PixelBasepairMapper setExonIntron(ReferenceSequence reference, boolean dir5to3, GenomicRegion region, IntToDoubleFunction exonFun, IntToDoubleFunction intronFun) {
		PixelBasePairRange[] ranges = new PixelBasePairRange[region.getNumParts()*2-1];
		double x = 0;

		for (int i=0; i<ranges.length; i++) {
			ranges[i] = new PixelBasePairRange(reference, region.getBoundary(i), region.getBoundary(i+1)-1, 
					x, 
					x+= (i%2==0? /*exon start*/ exonFun.applyAsDouble(region.getBoundary(i+1)-region.getBoundary(i)) : intronFun.applyAsDouble(region.getBoundary(i+1)-region.getBoundary(i)))
					);
		}
		return set(ranges,dir5to3);
	}
	
	/**
	 * Between reference sequences: intron(-1) is evaluated
	 * @param reference
	 * @param dir5to3
	 * @param region
	 * @param exonFun
	 * @param intronFun
	 * @return
	 */
	private PixelBasepairMapper setExonIntron(ReferenceSequence[] reference, boolean dir5to3, GenomicRegion[] region, IntToDoubleFunction exonFun, IntToDoubleFunction intronFun) {
		int n = 0;
		for (int i=0; i<region.length; i++)
			n+=region[i].getNumParts()*2-1;
		
		PixelBasePairRange[] ranges = new PixelBasePairRange[n];
		double x = 0;

		int index = 0;
		for (int i=0; i<region.length; i++) {
			for (int p=0; p<region[i].getNumBoundaries()-1; p++) {
				ranges[index++] = new PixelBasePairRange(reference[i], region[i].getBoundary(p), region[i].getBoundary(p+1)-1, 
						x, 
						x+= (p%2==0? /*exon start*/ exonFun.applyAsDouble(region[i].getBoundary(p+1)-region[i].getBoundary(p)) : intronFun.applyAsDouble(region[i].getBoundary(p+1)-region[i].getBoundary(p)))
						);
			}
			x+=intronFun.applyAsDouble(-1);
		}
		return set(ranges,dir5to3);
	}


	public boolean inRange(ReferenceSequence reference, int c) {
		if (!map.containsKey(reference))
			return false;

		PixelBasePairRange[] ranges = map.get(reference);
		return c>=ranges[0].getBpStart() && c<=ranges[ranges.length-1].getBpStop();
	}

	private static int binarySearchBp(PixelBasePairRange[] a, int bp) {
		int low = 0;
		int high = a.length - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			PixelBasePairRange midVal = a[mid];
			int cmp = midVal.bpCompare(bp);
			if (cmp < 0)
				low = mid + 1;
			else if (cmp > 0)
				high = mid - 1;
			else
				return mid; // key found
		}
		return -(low + 1);  // key not found.
	}
	
	private static int binarySearchPixel(PixelBasePairRange[] a, double pixel) {
		int low = 0;
		int high = a.length - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;
			PixelBasePairRange midVal = a[mid];
			int cmp = midVal.pixelCompare(pixel);
			if (cmp < 0)
				low = mid + 1;
			else if (cmp > 0)
				high = mid - 1;
			else
				return mid; // key found
		}
		return -(low + 1);  // key not found.
	}

	/**
	 * Maps a genomic location to a pixel coordinate
	 * @param c
	 * @return
	 */
	public double bpToPixel(ReferenceSequence reference, int c) {
		if (!map.containsKey(reference))
			return -1;

		PixelBasePairRange[] ranges = map.get(reference);
		if (ranges==null) return -1;
		
		
		int p = binarySearchBp(ranges, c);
		if (p==-1)
			return checkInvert(reference, ranges[0].pixelStart-1);
		if (p==-ranges.length-1) 
			// smaller than start or bigger than end
			return checkInvert(reference, ranges[ranges.length-1].pixelStop+1);

		if (p>=0)
			return checkInvert(reference,ranges[p].bpToPixel(c));
		throw new RuntimeException();
	}
	
	public PixelBasePairRange[] getRanges(ReferenceSequence reference) {
		return map.get(reference);
	}
	
	/**
	 * Maps a genomic location to a pixel coordinate
	 * @param c
	 * @return
	 */
	public double bpToPixel(PixelBasePairRange[] ranges, ReferenceSequence reference, int c) {
		if (ranges==null) return -1;
		
		
		int p = binarySearchBp(ranges, c);
		if (p==-1)
			return checkInvert(reference, ranges[0].pixelStart-1);
		if (p==-ranges.length-1) 
			// smaller than start or bigger than end
			return checkInvert(reference, ranges[ranges.length-1].pixelStop+1);

		if (p>=0)
			return checkInvert(reference,ranges[p].bpToPixel(c));
		throw new RuntimeException();
	}
	
	/**
	 * Maps a genomic location to a pixel coordinate
	 * @param c
	 * @return
	 */
	public double bpToPixel(PixelBasePairRange range, ReferenceSequence reference, int c) {
		if (range==null) return -1;
		return checkInvert(reference,range.bpToPixel(c));
	}


	/**
	 * Maps a pixel coordinate to a genomic location; closest pixel if not within the genomic region
	 * @param c
	 * @return
	 */
	public int pixelToBp(double c) {
		int p = binarySearchPixel(ranges, c);
		if (p==-1){
			return ranges[0].bpStart;
		}
		if (p==-ranges.length-1) 
			// smaller than start or bigger than end
			return ranges[ranges.length-1].bpStop;

		if (p>=0)
			return ranges[p].pixelToBp(checkInvert(ranges[p].getReference(),c));
		
		p = -p-1;
		
		int a = ranges[p-1].pixelToBp(checkInvert(ranges[p-1].getReference(),c));
		int b = ranges[p].pixelToBp(checkInvert(ranges[p].getReference(),c));
		
		return (a+b)/2;
	}
	
	/**
	 * Maps a pixel coordinate to a genomic location; closest pixel if not within the genomic region
	 * @param c
	 * @return
	 */
	public ReferenceSequence pixelToReferenceSequence(double c) {
		int p = binarySearchPixel(ranges, c);
		if (p==-1){
			return ranges[0].reference;
		}
		if (p==-ranges.length-1) 
			// smaller than start or bigger than end
			return ranges[ranges.length-1].reference;

		if (p>=0)
			return ranges[p].reference;
		
		p = -p-1;
		
		return (ranges[p-1].reference.equals(ranges[p].reference))?ranges[p].reference:null;
	}

	/**
	 * Maps a pixel coordinate to a genomic location; -1 if not within the genomic region or given reference
	 * @param c
	 * @return
	 */
	public int pixelToBp(double c, ReferenceSequence ref) {
		int p = binarySearchPixel(ranges, c);
		if (p==-1)
			return -1;
		if (p==-ranges.length-1) 
			// smaller than start or bigger than end
			return -1;

		if (p>=0) {
			if (!ranges[p].reference.equals(ref))
				return -1;
			
			return ranges[p].pixelToBp(checkInvert(ranges[p].getReference(),c));
		}
		return -1;
	}

	
	public long getTotalBasepairs() {
		long total = 0;
		for (int i=0; i<ranges.length; i+=2)
			total+=ranges[i].getBpStop()-ranges[i].getBpStart()+1;
		return total;
	}
	
	public double getPixelsPerBasePair() {
		if (ranges.length==0) return Double.POSITIVE_INFINITY;
		return getWidth()/getTotalBasepairs();
	}

	public double getPixelPerBasepair(ReferenceSequence reference, int bp) {
		if (!map.containsKey(reference))
			return -1;

		PixelBasePairRange[] ranges = map.get(reference);
		
		
		int p = binarySearchBp(ranges, bp);
		if (p==-1)
			return -1;
		if (p==-ranges.length-1) 
			// smaller than start or bigger than end
			return -1;

		if (p>=0)
			return ranges[p].getPixelPerBasepair();
		throw new RuntimeException();
	}
	
	public double getPixelPerBasepair(double pixel) {

		int p = binarySearchPixel(ranges, pixel);
		if (p==-1)
			return -1;
		if (p==-ranges.length-1) 
			// smaller than start or bigger than end
			return -1;

		if (p>=0)
			return ranges[p].getPixelPerBasepair();
		throw new RuntimeException();
	}

	public boolean is5to3() {
		return dir5to3;
	}

	public GenomicRegion getRegion(ReferenceSequence reference) {
		PixelBasePairRange[] re = map.get(reference);
		if (re==null) return new ArrayGenomicRegion();
		IntArrayList re2 = new IntArrayList();
		for (int i=0; i<re.length; i++) {
			re2.add(re[i].bpStart);
			re2.add(re[i].bpStop+1);
		}
		return new ArrayGenomicRegion(re2);
	}



}
