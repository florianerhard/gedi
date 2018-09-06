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
package gedi.macoco.javapipeline;

import java.util.HashMap;
import java.util.function.BiConsumer;

import gedi.core.data.annotation.Transcript;
import gedi.core.genomic.Genomic;
import gedi.core.reference.Strandness;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.math.stat.inference.EquivalenceClassCountEM;
import gedi.util.program.GediProgramContext;

public class EquivalenceClassesMaximumLikelihood extends EstimateTpm {



	public EquivalenceClassesMaximumLikelihood(MacocoParameterSet params) {
		super(params,params.emMLTable);
	}



	@Override
	protected void estimate(GediProgramContext context, Genomic genomic, MemoryIntervalTreeStorage<Transcript> trans, Strandness strand, int cond, String condName, double[] eff, String[][] E, double[] counts,
			BiConsumer<String, Double> transUnnorm) {
		
		HashMap<String, Double> traToEffl = trans.ei().index(r->r.getData().getTranscriptId(), r->eff[r.getRegion().getTotalLength()]);

		EquivalenceClassCountEM<String> algo = new EquivalenceClassCountEM<String>(E, counts, traToEffl::get);
		algo.compute(2000, 10000, (t,a)->{
			transUnnorm.accept(t, a/traToEffl.get(t));
		});
		
	}
}
