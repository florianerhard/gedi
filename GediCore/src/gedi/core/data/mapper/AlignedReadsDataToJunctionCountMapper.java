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


import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.MissingInformationIntronInformation;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;

@GenomicRegionDataMapping(fromType=IntervalTree.class,toType=IntervalTree.class)
public class AlignedReadsDataToJunctionCountMapper implements GenomicRegionDataMapper<IntervalTree<GenomicRegion,AlignedReadsData>, IntervalTree<GenomicRegion,NumericArray>>{

	private Strand strand = Strand.Independent;
	private ReadCountMode readCountMode = ReadCountMode.Weight;
	
	public void setReadCountMode(ReadCountMode readCountMode) {
		this.readCountMode = readCountMode;
	}
	
	public void setStrand(Strand strand) {
		this.strand = strand;
	}

	@Override
	public IntervalTree<GenomicRegion,NumericArray> map(ReferenceSequence reference,
			GenomicRegion region,PixelLocationMapping pixelMapping,
			IntervalTree<GenomicRegion, AlignedReadsData> data) {
		if (data.isEmpty()) return new IntervalTree<GenomicRegion,NumericArray>(data.getReference());
		
		int numCond = data.firstEntry().getValue().getNumConditions();
		
		IntervalTree<GenomicRegion,NumericArray> re = new IntervalTree<GenomicRegion,NumericArray>(data.getReference());

		data.entrySet().iterator().forEachRemaining(e->{
			if (e.getKey().getNumParts()>1) {
				int l=!strand.isMinus()?0:e.getKey().getTotalLength();
				for (int i=0; i<e.getKey().getNumParts()-1; i++) {
					if (!strand.isMinus())
						l+=e.getKey().getLength(i);
					else
						l-=e.getKey().getLength(i);
					if (e.getKey() instanceof MissingInformationIntronInformation && ((MissingInformationIntronInformation)e.getKey()).isMissingInformationIntron(i)){}
					else {
						AlignedReadsData val = e.getValue();
						if (!val.isReadPairGap(l,0)) {
							ArrayGenomicRegion reg = new ArrayGenomicRegion(e.getKey().getEnd(i),e.getKey().getStart(i+1));
							NumericArray a = re.computeIfAbsent(reg, k->NumericArray.createMemory(numCond, NumericArrayType.Double));
							val.addTotalCountsForConditions(a, readCountMode);
						}
					}
				}
			}
		});
		return re;
	}



}
