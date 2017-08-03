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

package gedi.riboseq.javapipeline;

import java.io.IOException;
import java.util.function.Predicate;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.genomic.Genomic;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.riboseq.inference.clustering.RiboClusterBuilder;
import gedi.riboseq.utils.RiboUtils;
import gedi.util.program.GediProgram;
import gedi.util.program.GediProgramContext;

public class PriceClusterReads extends GediProgram {

	public PriceClusterReads(PriceParameterSet params) {
		addInput(params.prefix);
		addInput(params.reads);
		addInput(params.filter);
		addInput(params.genomic);
		addInput(params.nthreads);
		addOutput(params.clusters);
	}
	
	public String execute(GediProgramContext context) throws IOException {
		
		String prefix = getParameter(0);
		GenomicRegionStorage<AlignedReadsData> reads = getParameter(1);
		String filter = getParameter(2);
		Genomic genomic = getParameter(3);
		int nthreads = getIntParameter(4);
		
		Predicate<ReferenceGenomicRegion<AlignedReadsData>> ffilter = RiboUtils.parseReadFilter(filter);
		
		RiboClusterBuilder clb = new RiboClusterBuilder(prefix, reads, ffilter, genomic.getTranscripts(), 1, 5, context.getProgress(), nthreads);
		clb.setContinueMode(true);
		clb.setPredicate(r->genomic.getSequenceNames().contains(r.getReference().getName()));
		clb.build();
		
		return null;
	}
	
	

}
