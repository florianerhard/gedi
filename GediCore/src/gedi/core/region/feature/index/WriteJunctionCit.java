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
import gedi.util.datastructure.array.MemoryDoubleArray;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.functions.IterateIntoSink;

public class WriteJunctionCit extends AbstractFeature<Void> {

	private String file;
	private NumericArray buffer;
	
	public WriteJunctionCit(String file) {
		this.file = file;
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
	}
	
	@Override
	protected void accept_internal(Set<Void> t) {
		GenomicRegion region = referenceRegion.getRegion();
		if (region.getNumParts()>1) {
			buffer = program.dataToCounts(referenceRegion.getData(), buffer);
			int l = 0;
			for (int i=0; i<region.getNumParts()-1; i++) {
				if (region instanceof MissingInformationIntronInformation && ((MissingInformationIntronInformation)region).isMissingInformationIntron(i)){}
				else {
					if (!(referenceRegion.getData() instanceof AlignedReadsData) || !((AlignedReadsData) referenceRegion.getData()).isReadPairGap(l,0)) {
						
						ArrayGenomicRegion reg = new ArrayGenomicRegion(region.getEnd(i),region.getStart(i+1));
						buffer = program.dataToCounts(referenceRegion.getData(), buffer);
						
						MemoryDoubleArray in = mem.getData(referenceRegion.getReference(), reg);
						if (in==null) in = new MemoryDoubleArray(buffer.length());
						in.add(buffer);
						mem.add(referenceRegion.getReference(), reg, in);
					}
				}
				l+=region.getLength(i);
			}
		}
	}
	
	@Override
	public void end() {
		GenomicRegionStorage<MemoryDoubleArray> bui = GenomicRegionStorageExtensionPoint.getInstance().get(MemoryDoubleArray.class, file, GenomicRegionStorageCapabilities.Disk, GenomicRegionStorageCapabilities.Fill);
		bui.fill(mem);
	}

}
