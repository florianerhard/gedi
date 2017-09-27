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

package gedi.core.data.mapper;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.function.UnaryOperator;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
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
public class PatternMatrix<D> implements GenomicRegionDataMapper<IntervalTree<GenomicRegion,D>,PixelBlockToValuesMap>, BinningProvider{

	
	private Binning binning = null;
	private PreparedIntKernel basepairKernel = new SingletonKernel().prepare();
	private PreparedIntKernel lengthKernel = new SingletonKernel().prepare();
	
	private ToDoubleFunction<D> valueFunction = d->1;
	
	private GenomicRegionPosition position = GenomicRegionPosition.FivePrime;
	private int offset = 0;
	private Strand strand;
	
	
	public PatternMatrix(int min, int max, Strand strand) {
		binning = new FixedSizeBinning(min, max+1, max-min+1);
		this.strand = strand;
	}
	
	public PatternMatrix(int min, int max, int bins, Strand strand) {
		binning = new FixedSizeBinning(min, max+1, bins);
		this.strand = strand;
	}
	
	public void setValueFunction(ToDoubleFunction<D> valueFunction) {
		this.valueFunction = valueFunction;
	}
	
	public void setBasepairKernel(Kernel basepairKernel) {
		this.basepairKernel = basepairKernel.prepare();
	}
	
	public void setLengthKernel(Kernel lengthKernel) {
		this.lengthKernel = lengthKernel.prepare();
	}
	
	public void setReadPosition(GenomicRegionPosition position) {
		setReadPosition(position, 0);
	}
	
	public void setReadPosition(GenomicRegionPosition position, int offset) {
		this.position = position;
		this.offset = offset;
	}
	
	public Binning getBinning() {
		return binning;
	}
	
	@Override
	public PixelBlockToValuesMap map(ReferenceSequence reference,
			GenomicRegion region, PixelLocationMapping pixelMapping,
			IntervalTree<GenomicRegion, D> data) {
		
		
		if (data.isEmpty()) return new PixelBlockToValuesMap(pixelMapping, 0, NumericArrayType.Double);
		
		
		PixelBlockToValuesMap re = new PixelBlockToValuesMap(pixelMapping, binning.getBins(), NumericArrayType.Double);
		
		Iterator<Entry<GenomicRegion, D>> it = data.entrySet().iterator();
		while (it.hasNext()) {
			Entry<GenomicRegion, D> n = it.next();
			int p = position.position(reference.toStrand(strand), n.getKey(), offset);
			int len = n.getKey().getTotalLength();
			double value = valueFunction.applyAsDouble(n.getValue());
			if (region.contains(p)) {
				int block = re.getBlockIndex(reference, p);
				
				addToBlock(len,basepairKernel.applyAsDouble(0),re.getValues(block), value);
				
				int lblock = block;
				for (int wp=p-1; wp>=basepairKernel.getMinAffectedIndex(p); wp--) {
					// identify block
					while (lblock>=0 && !re.getBlock(lblock).containsBp(wp)) lblock--;
					if (lblock>=0)
						addToBlock(len,basepairKernel.applyAsDouble(wp-p),re.getValues(lblock), value);
					else 
						break;
				}
				
				int rblock = block;
				for (int wp=p+1; wp<=basepairKernel.getMaxAffectedIndex(p); wp++) {
					// identify block
					while (rblock<re.size() && !re.getBlock(rblock).containsBp(wp)) rblock++;
					if (rblock<re.size())
						addToBlock(len,basepairKernel.applyAsDouble(wp-p),re.getValues(rblock), value);
					else break;
				}
				
			}
		}
		
		return re;
		
		
	}

	private void addToBlock(int len, double weight, NumericArray re, double value) {
		
		for (int wl=lengthKernel.getMinAffectedIndex(len); wl<=lengthKernel.getMaxAffectedIndex(len); wl++) {
			double lweight = lengthKernel.applyAsDouble(wl-len);
			int bin = binning.applyAsInt(wl);
			if (bin>=0 && bin<re.length()) {
				re.add(bin, weight*lweight*value);
			}
		}
		
	}
	
	

}
