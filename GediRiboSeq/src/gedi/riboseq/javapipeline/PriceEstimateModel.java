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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.ScriptException;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.genomic.Genomic;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.riboseq.cleavage.CleavageModelEstimator;
import gedi.riboseq.cleavage.RiboModel;
import gedi.util.ArrayUtils;
import gedi.util.datastructure.dataframe.DataFrame;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.io.text.tsv.formats.Csv;
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
		addInput(params.percond);
		addInput(params.reads);
		
		addOutput(params.model);
	}
	
	public String execute(GediProgramContext context) throws IOException {
		
		String prefix = getParameter(0);
		int nthreads = getIntParameter(1);
		File maxPos = getParameter(2);
		File estimateData = getParameter(3);
		int repeats = getIntParameter(4);
		int maxiter = getIntParameter(5);
		boolean plot = getBooleanParameter(6);
		boolean percond = getBooleanParameter(7);
		GenomicRegionStorage<AlignedReadsData> reads = getParameter(8);
		
		DataFrame df = Csv.toDataFrame(maxPos.getPath(), true, 0, null);
		
		int[] pos = df.getIntegerColumn(0).getRaw().toIntArray();
		double[] sum = null;
		double[][] mat = new double[df.columns()-1][];
		for (int c=1; c<df.columns(); c++) {
			mat[c-1] = ArrayUtils.restrict(df.getDoubleColumn(c).getRaw().toDoubleArray(),ind->pos[ind]<=-10 && pos[ind]>=-17);
			sum = ArrayUtils.add(sum, mat[c-1]);
		}
		int[] posr = ArrayUtils.restrict(pos,ind->pos[ind]<=-10 && pos[ind]>=-17);

		int[] maxpos = new int[mat.length+1];
		for (int c=0; c<mat.length; c++) {
			maxpos[c] = -posr[ArrayUtils.argmax(mat[c])];
		}
		maxpos[maxpos.length-1] = -posr[ArrayUtils.argmax(sum)];
		
		String[] condNames = reads.getMetaDataConditions();
		
		CleavageModelEstimator em = new CleavageModelEstimator(null,reads,(Predicate<ReferenceGenomicRegion<AlignedReadsData>>)null);
		em.setProgress(context.getProgress());
		em.setMaxiter(maxiter);
		em.setRepeats(repeats);
		em.setMerge(!percond);
		em.readEstimateData(new LineOrientedFile(estimateData.getPath()));

		PageFileWriter model = new PageFileWriter(prefix+".model");
		
		if (percond) 
			for (int cond=0; cond<mat.length; cond++)
				estimate(context,model,em,cond,condNames[cond],maxpos[cond],nthreads,plot,prefix);
		else
			estimate(context,model,em,-1,"Merged",maxpos[maxpos.length-1],nthreads,plot,prefix);
		
		
		model.close();
		
		return null;
	}

	private void estimate(GediProgramContext context, PageFileWriter model, CleavageModelEstimator em, int cond, String condName, int maxPos,int nthreads, boolean plot, String prefix) throws IOException {
		em.setMaxPos(maxPos);
		context.getLog().info("Using maxpos="+maxPos+" ("+condName+")");
		
		context.getLog().info("Estimate parameters");
		double ll = em.estimateBoth(Math.max(0, cond),nthreads);
		context.getLog().info(String.format("LL=%.6g",ll));
//		if (plot)
//			em.plotProbabilities(prefix,prefix+".png");

		RiboModel m = em.getModel();
		
		m.serialize(model);
		
		if (plot) {
			
			double[] pl = em.getModel().getPl();
			double[] pr = em.getModel().getPr();
			double u = em.getModel().getU();
			
			prefix = prefix+"."+condName;
			LineWriter out = new LineOrientedFile(prefix+".model.tsv").write();
			out.writeLine("Parameter\tPosition\tValue");
			for (int i=0; i<pl.length; i++) 
				out.writef("Upstream\t%d\t%.7f\n", i, pl[i]);
			for (int i=0; i<pr.length; i++) 
				out.writef("Downstream\t%d\t%.7f\n", i, pr[i]);
			out.writef("Untemplated addition\t0\t%.7f\n", u);
			out.close();
			
			try {
				context.getLog().info("Running R scripts for plotting");
				RRunner r = new RRunner(prefix+".ribomodel.R");
				r.set("prefix",prefix);
				r.addSource(getClass().getResourceAsStream("/resources/R/ribomodel.R"));
				r.run(true);
			} catch (Throwable e) {
				context.getLog().log(Level.SEVERE, "Could not plot!", e);
			}
		}
		
	}
	
	

}
