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
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.logging.Level;

import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.data.numeric.diskrmq.DiskGenomicNumericBuilder;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.genomic.Genomic;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.sequence.SequenceProvider;
import gedi.riboseq.cleavage.RiboModel;
import gedi.riboseq.inference.clustering.RiboClusterInfo;
import gedi.riboseq.inference.codon.Codon;
import gedi.riboseq.inference.codon.CodonInference;
import gedi.riboseq.inference.orf.OrfInference;
import gedi.riboseq.inference.orf.PriceOrf;
import gedi.riboseq.inference.orf.StartCodonScorePredictor;
import gedi.riboseq.inference.orf.StartCodonTraining;
import gedi.riboseq.utils.RiboUtils;
import gedi.util.FileUtils;
import gedi.util.datastructure.array.MemoryFloatArray;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.datastructure.tree.redblacktree.IntervalTreeSet;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.program.GediProgram;
import gedi.util.program.GediProgramContext;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;

public class PriceStartPredictionTraining extends GediProgram {

	public PriceStartPredictionTraining(PriceParameterSet params) {
		addInput(params.prefix);
		addInput(params.nthreads);
		addInput(params.orfinference);
		addInput(params.trainingExamples);
		addInput(params.codons);
		addOutput(params.startmodel);
	}
	
	public String execute(GediProgramContext context) throws IOException {
		
		String prefix = getParameter(0);
		int nthreads = getIntParameter(1);
		OrfInference v = getParameter(2);
		int trainingExamples = getIntParameter(3);
		int chunk = 10;
		
		context.getLog().log(Level.INFO, "Train start prediction");
		
		StartCodonTraining startPred = new StartCodonTraining(trainingExamples);
		RiboUtils.processCodonsSink(prefix+".codons.bin", "Start prediction training", context.getProgress(), null, nthreads, chunk, PriceOrf.class, 
			ei->ei.unfold(o->v.findAnnotated(false,o).iterator()).removeNulls().filter(o->o.getData().getTotalActivityFromPredicted()>=25 && o.getData().getOrfAaLength()>=10),
			n->startPred.add(n)
			);
		
//		startPred.writeDirichletModel(50,50,1000);
//		if (true) return;
		
//		log.log(Level.INFO, "Considering "+startPred.getNumExamples()+" ORFs with meanActivity>"+startPred.getMinMean()+" with Isoform fraction>="+thres);
		context.getLog().log(Level.INFO, "Considering "+startPred.getNumExamples()+" ORFs for start training");
		
//		log.log(Level.INFO, "CV");
//		CompleteRocAnalysis roc = startPred.crossValidation(5,true,false);
//		FileUtils.writeAllText(roc.toString(), new File(prefix+".start.startscore.cv"));
//		log.log(Level.INFO, "Start: 5-fold CV AUROC="+roc.getAucFprTpr()+" AUPR="+roc.getAucPpvTpr());
//		
//		roc = startPred.crossValidation(5,false,true);
//		FileUtils.writeAllText(roc.toString(), new File(prefix+".start.rangescore.cv"));
//		log.log(Level.INFO, "Range: 5-fold CV AUROC="+roc.getAucFprTpr()+" AUPR="+roc.getAucPpvTpr());
//		
//		roc = startPred.crossValidation(5,true,true);
//		FileUtils.writeAllText(roc.toString(), new File(prefix+".start.bothscores.cv"));
//		log.log(Level.INFO, "Both: 5-fold CV AUROC="+roc.getAucFprTpr()+" AUPR="+roc.getAucPpvTpr());
		
		StartCodonScorePredictor predictor = startPred.train();
//		v.setStartCodonPredictor(predictor);
		
//		log.log(Level.INFO, "Evaluate ranks in all CDS");
//		double[] bounds = {0.3,1,Double.POSITIVE_INFINITY};
//		NumericSample[] ranks = {new NumericSample(),new NumericSample(),new NumericSample()};
//		
//		LineWriter startDetails = new LineOrientedFile(prefix+".start.ranks").write();
//		startDetails.write("Transcript\tGeom.mean\tScore\tRank\tLocation\n");
//		processCodonsSink(prefix+".codons.bin", progress, null, nthreads, chunk, PriceOrf.class, 
//				ei->ei.unfold(o->v.findAnnotated(false,o).iterator()).removeNulls().filter(o->o.getData().getTotalActivityFromPredicted()>=25 && o.getData().getOrfAaLength()>=10),
//				n->{
//					double[] scores = v.computeStartScores(n.getData(), null,true);
//					double ann = scores[n.getData().getPredictedStartAminoAcid()];
//					DoubleRanking ranking = new DoubleRanking(scores);
//					ranking.sort(false);
//					double rank = ranking.getCurrentRank(n.getData().getPredictedStartAminoAcid())/(double)scores.length;
//					double sinh = n.getData().getGeomMean(n.getData().getPredictedStartAminoAcid());
//					
//					if (!Double.isNaN(ann)) {
//						for (int r=0; r<bounds.length; r++) 
//							if (sinh<bounds[r]) {
//								ranks[r].add(rank);
//								break;
//							}
//						startDetails.writef2("%s\t%.2f\t%.2f\t%.2f\t%s\n",
//								n.getData().getTranscript(),
//								sinh,
//								ann,
//								rank,
//								n.toLocationString()
//								);
//					}
//				}
//				);
//		
//		startDetails.close();
//		
//		for (int r=0; r<bounds.length; r++) 
//			log.log(Level.INFO, "Activity<"+bounds[r]+" AUECDF="+ranks[r].ecdf().integral(0,1));
		
		
		
		PageFileWriter fm = new PageFileWriter(prefix+".start.model");
		predictor.serialize(fm);
		fm.close();
		
		
		
		
		return null;
	}
	

}
