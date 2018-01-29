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
import gedi.riboseq.inference.orf.NoiseModel;
import gedi.riboseq.inference.orf.OrfInference;
import gedi.riboseq.inference.orf.PriceOrf;
import gedi.riboseq.inference.orf.StartCodonScorePredictor;
import gedi.riboseq.inference.orf.StartCodonTraining;
import gedi.riboseq.utils.RiboUtils;
import gedi.util.FileUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.MemoryFloatArray;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.datastructure.collections.doublecollections.DoubleIterator;
import gedi.util.datastructure.tree.redblacktree.IntervalTreeSet;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.program.GediProgram;
import gedi.util.program.GediProgramContext;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;

public class PriceOrfInference extends GediProgram {

	public PriceOrfInference(PriceParameterSet params) {
		addInput(params.prefix);
		addInput(params.nthreads);
		addInput(params.orfinference);
		addInput(params.codons);
		addInput(params.startmodel);
		addInput(params.noisemodel);
		
		addOutput(params.pvals);
		addOutput(params.orfstsv);
		addOutput(params.orfsbin);
		
	}
	
	public String execute(GediProgramContext context) throws IOException {
		
		String prefix = getParameter(0);
		int nthreads = getIntParameter(1);
		OrfInference v = getParameter(2);
		int chunk = 10;
		
		
		
		
		PageFile fm = new PageFile(prefix+".start.model");
		StartCodonScorePredictor predictor = new StartCodonScorePredictor(); 
		predictor.deserialize(fm);
		fm.close();
		v.setStartCodonPredictor(predictor);
		
		
		fm = new PageFile(prefix+".noise.model");
		NoiseModel m = new NoiseModel();
		m.deserialize(fm);
		
		v.setNoiseModel(m);
		fm.close();
		
		
		
		String[] conditions = v.getConditions();

		
		context.getLog().log(Level.INFO, "Infer ORFs");
		LineWriter tab = new LineOrientedFile(prefix+".orfs.tsv").write();
		PriceOrf.writeTableHeader(tab, conditions);
		
		PageFileWriter tmp = new PageFileWriter(prefix+".orfs.bin");
		DoubleArrayList pvals = new DoubleArrayList();
		RiboUtils.processCodonsSink(prefix+".codons.bin", "ORF inference", context.getProgress(), ()->"Cache: "+StringUtils.toString(v.getNoiseModel().getCacheSize()),nthreads, chunk, PriceOrf.class, 
				ei->ei.demultiplex(o->v.inferOrfs(o).ei()),
				n->{
					try {
//							if (n.getData().getExpP()>v.getTestThreshold() && n.getData().getAbortiveP()>v.getTestThreshold()) {
								n.toMutable().serialize(tmp);
								pvals.add(n.getData().getCombinedP());
//							}
							n.getData().writeTableLine(tab, n);
					} catch (IOException e) {
						throw new RuntimeException("Could not write ORFs!",e);
					}
					
				});
		tab.close();
		tmp.close();
		
		PageFileWriter pvf = new PageFileWriter(getOutputFile(0).getAbsolutePath()); 
		DoubleIterator dit = pvals.iterator();
		while( dit.hasNext()) pvf.putDouble(dit.nextDouble());
		pvf.close();
		
		
		return null;
	}
	

}
