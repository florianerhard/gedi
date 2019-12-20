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
package gedi.core.region.feature.index;

import java.util.Set;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.GenomicRegionStorageCapabilities;
import gedi.core.region.GenomicRegionStorageExtensionPoint;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MissingInformationIntronInformation;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.features.AbstractFeature;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.ArrayUtils;
import gedi.util.datastructure.array.MemoryDoubleArray;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.decorators.NumericArraySlice;
import gedi.util.functions.IterateIntoSink;

public class WriteJunctionCit extends AbstractFeature<Void> {

	private String file;
	private NumericArray buffer;
	private String condition;
	private int conditionIndex;
	
	public WriteJunctionCit(String file) {
		this.file = file;
	}
	
	public WriteJunctionCit(String file, String condition) {
		this.file = file;
		this.condition = condition;
	}


	@Override
	public GenomicRegionFeature<Void> copy() {
		WriteJunctionCit re = new WriteJunctionCit(file);
		re.copyProperties(this);
		return re;
	}

	private MemoryIntervalTreeStorage<MemoryDoubleArray> mem;
	@Override
	public void begin() {
		if (program.getThreads()>1) throw new RuntimeException("Can only be run with 1 thread!");
		mem = new MemoryIntervalTreeStorage<>(MemoryDoubleArray.class);
		
		if (condition!=null) {
			conditionIndex = ArrayUtils.linearSearch(program.getLabels(),condition);
			if (conditionIndex==-1) throw new RuntimeException("Could not find "+condition+" in labels!");
		}
	}
	
	@Override
	protected void accept_internal(Set<Void> t) {
		GenomicRegion region = referenceRegion.getRegion();
		if (region.getNumParts()>1) {
			int l=!referenceRegion.getReference().isMinus()?0:referenceRegion.getRegion().getTotalLength();
			for (int i=0; i<region.getNumParts()-1; i++) {
				if (!referenceRegion.getReference().isMinus())
					l+=region.getLength(i);
				else
					l-=region.getLength(i);
				
				if (region instanceof MissingInformationIntronInformation && ((MissingInformationIntronInformation)region).isMissingInformationIntron(i)){}
				else {
					if (!(referenceRegion.getData() instanceof AlignedReadsData) || !((AlignedReadsData) referenceRegion.getData()).isReadPairGap(l,0)) {
						
						ArrayGenomicRegion reg = new ArrayGenomicRegion(region.getEnd(i),region.getStart(i+1));
						buffer = program.dataToCounts(referenceRegion.getData(), buffer);
						if (buffer.sum()>0) {
							MemoryDoubleArray in = mem.getData(referenceRegion.getReference(), reg);
							if (condition==null) {
								if (in==null) in = new MemoryDoubleArray(buffer.length());
								in.add(buffer);
							} else {
								if (in==null) in = new MemoryDoubleArray(1);
								in.add(0,buffer.getDouble(0));
							}
							if (in.sum()>0)
								mem.add(referenceRegion.getReference(), reg, in);
						}
					}
				}
			}
			
		}
	}
	
	@Override
	public void end() {
		GenomicRegionStorage<MemoryDoubleArray> bui = GenomicRegionStorageExtensionPoint.getInstance().get(MemoryDoubleArray.class, file, GenomicRegionStorageCapabilities.Disk, GenomicRegionStorageCapabilities.Fill);
		bui.fill(mem);
	}

}
