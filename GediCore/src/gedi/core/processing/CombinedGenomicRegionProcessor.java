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
package gedi.core.processing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.region.MutableReferenceGenomicRegion;

public class CombinedGenomicRegionProcessor extends ArrayList<GenomicRegionProcessor> implements GenomicRegionProcessor {

	public CombinedGenomicRegionProcessor() {
	}

	
	public CombinedGenomicRegionProcessor(Collection<? extends GenomicRegionProcessor> fill) {
		super(fill);
	}

	
	public CombinedGenomicRegionProcessor(GenomicRegionProcessor... fill) {
		super(Arrays.asList(fill));
	}

	
	@Override
	public void begin(ProcessorContext context) throws Exception {
		for (GenomicRegionProcessor p : this)
			p.begin(context);
	}

	@Override
	public void beginRegion(MutableReferenceGenomicRegion<?> region,
			ProcessorContext context) throws Exception {
		for (GenomicRegionProcessor p : this)
			p.beginRegion(region, context);
	}
	
	@Override
	public void read(MutableReferenceGenomicRegion<?> region,
			MutableReferenceGenomicRegion<AlignedReadsData> read,
			ProcessorContext context) throws Exception {
		for (GenomicRegionProcessor p : this)
			p.read(region, read, context);
	}
	
	@Override
	public void value(MutableReferenceGenomicRegion<?> region, int position,
			double[] values, ProcessorContext context) throws Exception {
		for (GenomicRegionProcessor p : this)
			p.value(region, position, values, context);
	}
	
	
	@Override
	public void endRegion(MutableReferenceGenomicRegion<?> region,
			ProcessorContext context) throws Exception {
		for (GenomicRegionProcessor p : this)
			p.endRegion(region, context);
	}


	@Override
	public void end(ProcessorContext context) throws Exception {
		for (GenomicRegionProcessor p : this)
			p.end(context);
	}

	
	

}
