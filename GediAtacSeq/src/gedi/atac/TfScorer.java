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

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.UnaryOperator;

import gedi.core.data.mapper.GenomicRegionDataMapper;
import gedi.core.data.mapper.GenomicRegionDataMapping;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionPosition;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelBlockToValuesMap;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.gui.genovis.pixelMapping.PixelLocationMappingBlock;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.math.stat.binning.Binning;
import gedi.util.math.stat.binning.FixedSizeBinning;
import gedi.util.math.stat.kernel.Kernel;
import gedi.util.math.stat.kernel.PreparedIntKernel;
import gedi.util.math.stat.kernel.SingletonKernel;

@GenomicRegionDataMapping(fromType=IntervalTree.class,toType=PixelBlockToValuesMap.class)
public class TfScorer implements GenomicRegionDataMapper<IntervalTree<GenomicRegion,AlignedReadsData>,PixelBlockToValuesMap> {

	
	private int min = 35;
	private int mean = 100;
	
	
	@Override
	public PixelBlockToValuesMap map(ReferenceSequence reference,
			GenomicRegion region, PixelLocationMapping pixelMapping,
			IntervalTree<GenomicRegion, AlignedReadsData> data) {
		
		
		if (data.isEmpty()) return new PixelBlockToValuesMap(pixelMapping, 0, NumericArrayType.Double);
		
		
		PixelBlockToValuesMap re = new PixelBlockToValuesMap(pixelMapping, 2, NumericArrayType.Double);
		
		MutableReferenceGenomicRegion<Void> rgr = new MutableReferenceGenomicRegion<Void>();
		rgr.setReference(reference);
		
		Iterator<Entry<GenomicRegion, AlignedReadsData>> it = data.entrySet().iterator();
		while (it.hasNext()) {
			Entry<GenomicRegion, AlignedReadsData> n = it.next();
			int s = GenomicRegionPosition.Start.position(rgr.setRegion(n.getKey()),4);
			int e = GenomicRegionPosition.Stop.position(rgr.setRegion(n.getKey()),-4);
			
			if (!region.intersects(s, e)) continue;
			
			int l = Math.max(min,n.getKey().getTotalLength());
			
			if (region.contains(s)) {
				re.getValues(re.getBlockIndex(reference, s)).add(0, n.getValue().getTotalCountOverallFloor(ReadCountMode.Weight));
				re.getValues(re.getBlockIndex(reference, s)).add(1, Math.exp((min-l)/mean)*n.getValue().getTotalCountOverallFloor(ReadCountMode.Weight));
			} else 
				re.getValues(re.getBlockIndex(reference, region.getStart())).add(1, Math.exp((min-l)/mean)*n.getValue().getTotalCountOverallFloor(ReadCountMode.Weight));
			
			if (region.contains(e)) {
				re.getValues(re.getBlockIndex(reference, e)).add(0, n.getValue().getTotalCountOverallFloor(ReadCountMode.Weight));
				if (re.getBlockIndex(reference, e)+1<re.getBlocks().size())
					re.getValues(re.getBlockIndex(reference, e)+1).add(1, -Math.exp((min-l)/mean)*n.getValue().getTotalCountOverallFloor(ReadCountMode.Weight));
			}
		}
		
		for (int i=1; i<re.getBlocks().size(); i++)
			re.getValues(i).add(1,re.getValues(i-1).getDouble(1));
		double max0 = 0;
		double max1 = 0;
		for (int i=0; i<re.getBlocks().size(); i++) {
			max0 = Math.max(max0,re.getValues(i).getDouble(0));
			max1 = Math.max(max1,re.getValues(i).getDouble(1));
		}
		for (int i=0; i<re.getBlocks().size(); i++) {
			re.getValues(i).setDouble(0, re.getValues(i).getDouble(0)/max0);
			re.getValues(i).setDouble(1, re.getValues(i).getDouble(1)/max1);
		}
		
		
		return re;
		
		
	}

}
