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

import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.util.ArrayUtils;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;



public class VisualizationLocationMapper implements BinarySerializable {

	/**
	 * Have equal lengths, sizes is cumulative
	 */
	private int[] coords;
	private double[] sizes;
	private boolean inverted = false;
	
	/**
	 * This produces a mixture of this and the given mapper. A mapper is a piecewise linear funtion.
	 * Does only work, if start and end of both are the same.
	 * The functions are combined by their increment at each point either be combined by weighting the min and max value at each point (minToMaxWeight)
	 * or by weighting this value to m's value (thisToMWeight). Both can be combined by weight (0-> minToMax only).
	 * @param m
	 * @param minToMaxWeight
	 * @param thisToMWeight
	 * @param absToRelWeight
	 */
	public VisualizationLocationMapper mix(VisualizationLocationMapper m, double minToMaxWeight, double thisToMWeight, double weight) {
		if (m.sizes[0]!=0 || sizes[0]!=0)
			throw new RuntimeException("Start point must be 0!");
			
		int[] nc = new int[coords.length+m.coords.length];
		System.arraycopy(coords, 0, nc, 0, coords.length);
		System.arraycopy(m.coords, 0, nc, coords.length, m.coords.length);
		Arrays.sort(nc);
		int count= ArrayUtils.unique(nc);
		nc = ArrayUtils.redimPreserve(nc, count);
		
		double last_a = 0;
		double last_b = 0;
		
		double[] ns = new double[nc.length];
		for (int i=1; i<nc.length; i++) {
			double a = bpToScreen(nc[i]);
			double b = m.bpToScreen(nc[i]);
			double incr_a = a-last_a;
			double incr_b = b-last_b;
			if (!inRange(nc[i])) {
				ns[i] = incr_b;
				last_b = b;	
			} else if (!m.inRange(nc[i])) {
				ns[i] = incr_a;
				last_a = a;
			} else {
				double minMax = Math.min(incr_a,incr_b)*(1-minToMaxWeight)+Math.max(incr_a,incr_b)*minToMaxWeight;
				double thisM = incr_a*(1-thisToMWeight)+incr_b*thisToMWeight;
				ns[i] = minMax*(1-weight)+thisM*weight;
				ns[i] = Math.max(incr_a,incr_b);
				last_a = a;
				last_b = b;
			}
			
		}
		
		ArrayUtils.cumSumInPlace(ns, +1);
		VisualizationLocationMapper re = new VisualizationLocationMapper();
		re.coords = nc;
		re.sizes = ns;
		return re;
	}
	
	public void useWidth(int start, int end, double width) {
		coords = new int[]{ start, end};
		sizes = new double[] {0,width};
	}
	
	public void useWidths(GenomicRegion gene, double[] widths) {
		coords = gene.getBoundaries();
		sizes = new double[coords.length];
		for (int i=1; i<coords.length; i++)
			sizes[i] = widths[i-1];
//		ArrayUtils.normalize(sizes);
		ArrayUtils.cumSumInPlace(sizes, +1);
	}
	
	public void useWidths(GenomicRegion gene, double[] exonWidths, double[] intronWidths) {
		coords = gene.getBoundaries();
		sizes = new double[coords.length];
		for (int i=1; i<coords.length; i++)
			sizes[i] = (i&1)==1?exonWidths[i/2]:intronWidths[i/2-1];
//		ArrayUtils.normalize(sizes);
		ArrayUtils.cumSumInPlace(sizes, +1);
	}
	
	
//	public void useWidths(Map<Coordinates,Double> preferredSizes, int min, int max, double betweenSize) {
//		
//		IntervalTree<DefaultSequenceFeature<Double>> cont = new IntervalTree<DefaultSequenceFeature<Double>>();
//		for (Coordinates c : preferredSizes.keySet()) 
//			for (int p=0; p<c.getNumParts(); p++)
//				cont.add(new DefaultSequenceFeature<Double>(c.getStart(p), c.getEnd(p)-1, preferredSizes.get(c)));
//		
//		DynamicIntArray coordsCreate = new DynamicIntArray();
//		DynamicDoubleArray sizeCreate = new DynamicDoubleArray();
//		
//		coordsCreate.add(min);
//		sizeCreate.add(0);
//		
////		System.out.println(cont.toString());
//		if (cont.size()>0){
//			Iterator<Set<DefaultSequenceFeature<Double>>> it = cont.groupIterator();
//			while (it.hasNext()) {
//				IntervalTree<DefaultSequenceFeature<Double>> n = new IntervalTree<DefaultSequenceFeature<Double>>(it.next());
//				double num = 0;
//				for (DefaultSequenceFeature<Double> f : n) 
//					num = Math.max(num,1+f.getFeature()*(n.getStop()-n.getStart()+1)/(double)(f.getStop()-f.getStart()+1));
//				
//				coordsCreate.add(n.getStart());
//				coordsCreate.add(n.getStop());
//				sizeCreate.add(betweenSize);
//				sizeCreate.add(num);
//			}
//		}
//		
//		coordsCreate.add(max);
//		sizeCreate.add(betweenSize);
//		
//		coords = coordsCreate.toArray();
//		sizes = sizeCreate.toArray();
//		
////		ArrayUtils.normalize(sizes);
//		ArrayUtils.cumSumInPlace(sizes, +1);
//	}
//	
	
	public void useIntronFixed(GenomicRegion region, int width, int intronWidth) {
		if (width<=0) {
			setEmpty(region);
			return;
		}
		
		double exonTotal = width-intronWidth*(region.getNumParts()-1);
		if (exonTotal<0) {
			useIntronFixed(region, width, 0);
		}
		
		coords = region.getBoundaries();
		sizes = new double[coords.length];
		double total = region.getTotalLength();
		for (int i=1; i<coords.length; i++) {
			sizes[i] = ((i&1)==1?exonTotal*(coords[i]-coords[i-1])/total:intronWidth);
		}
		ArrayUtils.cumSumInPlace(sizes, +1);
	}
	
	private void setEmpty(GenomicRegion gene) {
		coords = gene.getBoundaries();
		sizes = new double[gene.getNumBoundaries()];
	}

	public void useFixedScale(GenomicRegion gene, double widthExon, double widthIntron) {
		coords = gene.getBoundaries();
		sizes = new double[coords.length];
		for (int i=1; i<coords.length; i++)
			sizes[i] = (i&1)==1?widthExon:widthIntron;
//		ArrayUtils.normalize(sizes);
		ArrayUtils.cumSumInPlace(sizes, +1);
	}
	
	/**
	 * scales<0 are treated as absolute distance.
	 * @param gene
	 * @param scaleExon
	 * @param scaleIntron
	 */
	public void useGenomicScale(GenomicRegion gene, double scaleExon, double scaleIntron) {
		coords = gene.getBoundaries();
		sizes = new double[coords.length];
		for (int i=1; i<coords.length; i++) {
			double scale = ((i&1)==1?scaleExon:scaleIntron);
			sizes[i] = scale<0?-scale:(coords[i]-coords[i-1])*scale;
		}
//		ArrayUtils.normalize(sizes);
		ArrayUtils.cumSumInPlace(sizes, +1);
	}
	
	public void setWidth(double width) {
		for (int i=0; i<sizes.length; i++)
			sizes[i]*=width/sizes[sizes.length-1];
	}
	
	public double getWidth() {
		return sizes[sizes.length-1];
	}
	
	public boolean inRange(int c) {
		int p = Arrays.binarySearch(coords, c);
		return !(p==-1 || p==-coords.length-1);
	}
	
	/**
	 * Maps a genomic location to a pixel coordinate
	 * @param c
	 * @return
	 */
	public double bpToScreen(int c) {
		int p = Arrays.binarySearch(coords, c);
		if (p==-1)
			return inverted?getWidth()+1:-1;
		if (p==-coords.length-1) 
			// smaller than start or bigger than end
			return inverted?-1:getWidth()+1;

		if (p>=0)
			return inverted?getWidth()-sizes[p]:sizes[p];
		p=-p-2;
		
		double r = (c-coords[p])/(double)(coords[p+1]-coords[p]);
		
		r = sizes[p]*(1-r)+sizes[p+1]*r;
		return inverted?getWidth()-r:r;
	}

	/**
	 * Maps a pixel coordinate to a genomic location; -1 if not within the genomic region
	 * @param c
	 * @return
	 */
	public int screenToBp(double c) {
		if (inverted) c = getWidth()-c;
		int p = Arrays.binarySearch(sizes, c);
		if (p==-1)
			return -1;
		if (p==-coords.length-1) 
			// smaller than start or bigger than end
			return -1;

		if (p>=0) {
			if (p%2==0)
				return coords[p];
			return -1;
		}
		p=-p-2;
		if (p%2==1) return -1;
		
		double r = (c-sizes[p])/(double)(sizes[p+1]-sizes[p]);
		
		r = coords[p]*(1-r)+coords[p+1]*r;
		return (int)r;
	}

	
	public void setInverted(boolean b) {
		inverted = b;
	}
	
	public boolean isInverted() {
		return inverted;
	}
	
	public GenomicRegion getRegion() {
		return new ArrayGenomicRegion(coords);
	}

	public double getPixelsPerBasePair() {
		int total = 0;
		for (int i=0; i<coords.length; i+=2)
			total+=coords[i+1]-coords[i];
		return sizes[sizes.length-1]/total;
	}

	public double getPixelPerBasepair(int bp) {
		double re = Math.abs(bpToScreen(bp+1)-bpToScreen(bp));
		return re;
	}

	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putCInt(coords.length);
		for (int i=0; i<coords.length; i++)
			out.putCInt(coords[i]);
		for (int i=0; i<coords.length; i++)
			out.putDouble(sizes[i]);
		out.putByte(inverted?1:0);
	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
		coords = new int[in.getCInt()];
		sizes = new double[coords.length];
		for (int i=0; i<coords.length; i++)
			coords[i] = in.getCInt();
		for (int i=0; i<coords.length; i++)
			sizes[i] = in.getDouble();
		inverted = in.getByte()==1;
	}
	


}
