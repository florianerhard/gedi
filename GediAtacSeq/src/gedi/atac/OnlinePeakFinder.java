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
package gedi.atac;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.DoubleBinaryOperator;
import java.util.stream.StreamSupport;

import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.commons.math3.special.Gamma;
import org.rosuda.REngine.Rserve.RserveException;

import gedi.core.data.mapper.GenomicRegionDataMapper;
import gedi.core.data.mapper.GenomicRegionDataMapping;
import gedi.core.data.mapper.GenomicRegionDataSink;
import gedi.core.data.numeric.DenseGenomicNumericProvider;
import gedi.core.data.numeric.GenomicNumericProvider;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionPart;
import gedi.gui.genovis.pixelMapping.PixelBlockToValuesMap;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.gui.genovis.pixelMapping.PixelLocationMappingBlock;
import gedi.util.ArrayUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.math.stat.kernel.Kernel;
import gedi.util.mutable.MutablePair;
import gedi.util.mutable.MutableTuple;
import gedi.util.r.R;
import gedi.util.r.RConnect;

@GenomicRegionDataMapping(fromType={PixelBlockToValuesMap.class,IntervalTree.class},toType=IntervalTree.class)
public class OnlinePeakFinder implements GenomicRegionDataMapper<MutablePair<PixelBlockToValuesMap,IntervalTree<GenomicRegion,AlignedReadsData>>,IntervalTree<GenomicRegion, Double>>{

	private double factor = 3;
	
	public void setFactor(double factor) {
		this.factor = factor;
	}

	R r;
	
	public OnlinePeakFinder() throws RserveException {
		r = RConnect.R();
	}

	@Override
	public IntervalTree<GenomicRegion, Double> map(ReferenceSequence reference,
			GenomicRegion region, PixelLocationMapping pixelMapping,
			MutablePair<PixelBlockToValuesMap,IntervalTree<GenomicRegion,AlignedReadsData>> data) {
		int bases = 0;
		int bins = data.Item1.size();
		
		int[] c = new int[bins];
		
		for (int b=0; b<data.Item1.size(); b++) {
			PixelLocationMappingBlock bl = data.Item1.getBlock(b);
			bases+=bl.getStopBp()+1-bl.getStartBp();
			
			c[b] = (int) NumericArrayFunction.Sum.applyAsDouble(data.Item1.getValues(b));
		}

		System.out.printf("Bases=%d\tBins=%d\tInsertions=%d\tSites=%d\n",bases,bins, ArrayUtils.sum(c),Arrays.stream(c).filter(v->v>0).count());
		
		IntArrayList hist = new IntArrayList();
		for (int i:c) hist.increment(i);
		
		try {
			r.eval("layout(t(1:2))");
			r.set("h", hist.toIntArray());
			r.eval("barplot(h,names.arg=1:length(h))");
			
		} catch (RserveException e) {
		}

		hist.clear();
		for (GenomicRegion d : data.Item2.keySet())
			hist.increment(d.getTotalLength(),data.Item2.get(d).getTotalCountOverallFloor(ReadCountMode.Weight));
		
		try {
			
			r.set("h", hist.toIntArray());
			r.eval("barplot(h,names.arg=1:length(h))");
			
		} catch (RserveException e) {
		}
		
		
		GenomicRegion re = new ArrayGenomicRegion();
		
		double mean = ArrayUtils.sum(c)/c.length;
		// find consecutive ranges that are >=factor*mean
		int sizeBefore;
		
		do {
			sizeBefore = re.getTotalLength();
			
			double min = mean*factor;
			int start = -1;
			double sum = 0;
			for (int i=0; i<c.length; i++) {
				if (c[i]>=min) {
					// in range
					if (start==-1) start = i;
				}
				else {
					// not in range
					if (start>-1) {
						for (; start-1>=0 && c[start-1]>mean; start--);
						for (; i+1<c.length && c[i+1]>mean; i++);
						re = re.union(new ArrayGenomicRegion(start,i));
						start = -1;
					}
					sum+=c[i];
				}
			}
			
			if (start>-1) {
				for (; start-1>=0 && c[start-1]>mean; start--);
				re = re.union(new ArrayGenomicRegion(start,c.length));
			}
			
			// compute new mean
			mean = sum/(c.length-re.getTotalLength());
			
		} while (sizeBefore<re.getTotalLength());
		
		
		
		IntervalTree<GenomicRegion,Double> re2 = new IntervalTree<GenomicRegion, Double>(data.Item2.getReference());
		for (GenomicRegionPart p : re) 
			re2.put(new ArrayGenomicRegion(data.Item1.getBlock(p.getStart()).getStartBp(),data.Item1.getBlock(p.getStop()).getStopBp()+1), max(c,p.getStart(),p.getStop()+1));
		
		
		return re2;
		
	}


	private double max(int[] c, int start, int end) {
		int re = c[start];
		for (int i=start+1; i<end; i++)
			re = Math.max(re,c[i]);
		return re;
	}
	
	
}

	