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

import java.io.File;
import java.io.IOException;


import gedi.core.data.reads.AlignedReadsData;
import gedi.core.genomic.Genomic;
import gedi.core.region.GenomicRegionStorage;
import gedi.riboseq.cleavage.CleavageModelEstimator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.program.GediProgram;
import gedi.util.program.GediProgramContext;
import gedi.util.userInteraction.progress.Progress;

public class PriceCollectSufficientStatistics extends GediProgram {

	
	
	public PriceCollectSufficientStatistics(PriceParameterSet params) {
		addInput(params.prefix);
		addInput(params.filter);
		addInput(params.genomic);
		addInput(params.reads);
		addInput(params.percond);
		addOutput(params.estimateData);
	}
	
	
	public String execute(GediProgramContext context) throws IOException {
		
		String prefix = getParameter(0);
		String filter = getParameter(1);
		Genomic genomic = getParameter(2);
		GenomicRegionStorage<AlignedReadsData> reads = getParameter(3);
		boolean percond = getBooleanParameter(4);
		
		CleavageModelEstimator em = new CleavageModelEstimator(genomic.getTranscripts(),reads,filter);
		em.setProgress(context.getProgress());
		
		em.setMerge(!percond);
		em.collectEstimateData(new LineOrientedFile(prefix+".summary"));
		new File(prefix+".summary").delete();
		em.writeEstimateData(new LineOrientedFile(prefix+".estimateData"));
		
		return null;
	}
	
	

}
