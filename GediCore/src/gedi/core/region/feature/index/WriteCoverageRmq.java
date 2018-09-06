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

import java.io.File;
import java.io.IOException;
import java.util.Set;

import gedi.core.data.numeric.diskrmq.DiskGenomicNumericBuilder;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.feature.GenomicRegionFeature;
import gedi.core.region.feature.features.AbstractFeature;
import gedi.util.FileUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.dynamic.DynamicObject;

public class WriteCoverageRmq extends AbstractFeature<Void> {

	private String file;
	private NumericArray buffer;
	
	public WriteCoverageRmq(String file) {
		this.file = file;
	}

	@Override
	public GenomicRegionFeature<Void> copy() {
		WriteCoverageRmq re = new WriteCoverageRmq(file);
		re.copyProperties(this);
		return this;
	}

	private DiskGenomicNumericBuilder bui;
	
	@Override
	public void begin() {
		if (program.getThreads()>1) throw new RuntimeException("Can only be run with 1 thread!");
		try {
			bui = new DiskGenomicNumericBuilder(file);
		} catch (IOException e) {
			throw new RuntimeException("Cannot write file!",e);
		}
	}
	@SuppressWarnings({ "unchecked" })
	@Override
	protected void accept_internal(Set<Void> t) {
		buffer = program.dataToCounts(referenceRegion.getData(), buffer);
		if (buffer.sum()>0)
			bui.addCoverageEx(referenceRegion.getReference(),referenceRegion.getRegion(),buffer);
	}
	
	@Override
	public void end() {
		try {
			bui.build(true);
		} catch (IOException e) {
			throw new RuntimeException("Cannot write file!",e);
		}
	}

}
