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
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;

import javax.script.ScriptException;

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.DefaultAlignedReadsData;
import gedi.core.genomic.Genomic;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.riboseq.cleavage.CleavageModelEstimator;
import gedi.riboseq.cleavage.RiboModel;
import gedi.riboseq.inference.codon.Codon;
import gedi.riboseq.inference.codon.ReadsXCodonMatrix;
import gedi.util.SequenceUtils;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.functions.EI;
import gedi.util.math.stat.RandomNumbers;
import gedi.util.program.GediProgram;
import gedi.util.program.GediProgramContext;
import gedi.util.userInteraction.progress.Progress;

public class PriceDetermineDelta extends GediProgram {

	
	int m = 1000;
	int o = 100;
	int f = 1;
	
	public PriceDetermineDelta(PriceParameterSet params) {
		addInput(params.model);
		addOutput(params.delta);
		setRunFlag(params.inferDelta);
	}
	
	public String execute(GediProgramContext context) throws IOException {
		
		
		File modelFile = getParameter(0);
		RiboModel model = RiboModel.fromFile(modelFile.getPath(), false)[0];
		
		
		int R = 1000;
		ReferenceSequence ref = Chromosome.obtain("chr1+");
		
		RandomNumbers rnd = new RandomNumbers();
		
		char[] seq = new char[12+100];
		for (int i=0; i<seq.length; i++)
			seq[i] = SequenceUtils.nucleotides[rnd.getUnif(0, 4)];

		Progress progress = context.getProgress();
		progress.init().setCount(R);
		double s = 0;
		for (int r=0; r<R; r++) {
			
			IntervalTree<GenomicRegion, DefaultAlignedReadsData> simu = new IntervalTree<GenomicRegion, DefaultAlignedReadsData>(null);
			for (int p=0; p<=9; p+=3) 
				for (int i=0; i<m; i++)
					model.generateRead(simu , p, ss->seq[ss+50]);
			
			for (int i=0; i<o; i++)
				model.generateRead(simu , f+3, ss->seq[ss+50]);

			s -= getLambda(model,f+3,EI.wrap(simu.keySet()).map(reg->new ImmutableReferenceGenomicRegion<>(ref,reg,simu.get(reg))));
			double us = s;
			double ur = r;
			progress.incrementProgress().setDescription(()->"Delta = "+(us/(ur+1)));
		}
		progress.finish();
		
		double delta = s/R;
		
		setOutput(0, delta);
//		LineWriter data = new LineOrientedFile(prefix+".lambda.data").write().writeLine("Major count\tOff frame count\tOff frame pos\tLambda");
//		data.writef2("%d\t%df\t%d\t%.5f\n",m,o,f,s);
//		data.close();
		
		
		return null;
	}
	
	
	private static double getLambda(RiboModel model, int p, Iterator<? extends ReferenceGenomicRegion<AlignedReadsData>> reads) {
		ReadsXCodonMatrix m = new ReadsXCodonMatrix(model, -1);
		int readcount = m.addAll(reads);
		m.finishReads();
		
		if (readcount==0) throw new RuntimeException("No reads");
		
		int cond = m.checkConditions();
		if (cond==-2) // no reads at all
			throw new RuntimeException("No reads");
		
		if (cond<0) throw new RuntimeException("Inconsistent conditions!");
		double threshold = 1E-2;
		int maxIter = 1000;
		
		int iters = 0;
		double lastDifference;
		do  {
			m.computeExpectedReadsPerCodon();
			m.computePriorReadProbabilities();
			m.computeExpectedCodonPerRead();
			iters++;
			lastDifference = m.computeExpectedCodons();
		} while (lastDifference>threshold && iters<maxIter);
	
		for (Codon c : m.getCodons())
			if (c.getStart()==p) return m.regularize3(c);
		throw new RuntimeException("No codon");
		
	}
	

}
