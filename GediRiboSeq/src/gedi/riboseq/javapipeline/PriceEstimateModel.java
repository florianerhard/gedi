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
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;

import javax.script.ScriptException;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.genomic.Genomic;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.riboseq.cleavage.CleavageModelEstimator;
import gedi.riboseq.cleavage.RiboModel;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.program.GediProgram;
import gedi.util.program.GediProgramContext;
import gedi.util.r.RRunner;
import gedi.util.userInteraction.progress.Progress;

public class PriceEstimateModel extends GediProgram {

	public PriceEstimateModel(PriceParameterSet params) {
		addInput(params.prefix);
		addInput(params.nthreads);
		addInput(params.maxPos);
		addInput(params.estimateData);
		addInput(params.repeats);
		addInput(params.maxiter);
		addInput(params.plot);
		addOutput(params.model);
	}
	
	public String execute(GediProgramContext context) throws IOException {
		
		String prefix = getParameter(0);
		int nthreads = getIntParameter(1);
		int maxPos = getIntParameter(2);
		File estimateData = getParameter(3);
		int repeats = getIntParameter(4);
		int maxiter = getIntParameter(5);
		boolean plot = getBooleanParameter(6);
		
		CleavageModelEstimator em = new CleavageModelEstimator(null,null,(Predicate<ReferenceGenomicRegion<AlignedReadsData>>)null);
		em.setProgress(context.getProgress());
		em.setMaxiter(maxiter);
		em.setRepeats(repeats);
		
		
		em.readEstimateData(new LineOrientedFile(estimateData.getPath()));
		
		em.setMaxPos(maxPos);
		context.getLog().info("Using maxpos="+maxPos);
		
		context.getLog().info("Estimate parameters");
		double ll = em.estimateBoth(0,nthreads);
		context.getLog().info(String.format("LL=%.6g",ll));
//		if (plot)
//			em.plotProbabilities(prefix,prefix+".png");

		RiboModel m = em.getModel();
		
		PageFileWriter model = new PageFileWriter(prefix+".model");
		m.serialize(model);
		model.close();
		
		if (plot) {
			
			double[] pl = em.getModel().getPl();
			double[] pr = em.getModel().getPr();
			double u = em.getModel().getU();
			
			LineWriter out = new LineOrientedFile(prefix+".model.tsv").write();
			out.writeLine("Parameter\tPosition\tValue");
			for (int i=0; i<pl.length; i++) 
				out.writef("Upstream\t%d\t%.7f\n", i, pl[i]);
			for (int i=0; i<pr.length; i++) 
				out.writef("Downstream\t%d\t%.7f\n", i, pr[i]);
			out.writef("Untemplated addition\t0\t%.7f\n", u);
			out.close();
			
			context.getLog().info("Running R scripts for plotting");
			RRunner r = new RRunner(prefix+".ribomodel.R");
			r.set("prefix",prefix);
			r.addSource(getClass().getResourceAsStream("/resources/R/ribomodel.R"));
			r.run(true);
		}
		
		return null;
	}
	
	

}
