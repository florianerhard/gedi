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
import java.util.Collections;
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
import gedi.riboseq.inference.orf.NoiseModel.SingleNoiseModel;
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

public class PriceNoiseTraining extends GediProgram {

	public PriceNoiseTraining(PriceParameterSet params) {
		addInput(params.prefix);
		addInput(params.nthreads);
		addInput(params.orfinference);
		addInput(params.codons);
		addOutput(params.noisemodel);
	}
	
	public String execute(GediProgramContext context) throws IOException {
		
		String prefix = getParameter(0);
		int nthreads = getIntParameter(1);
		OrfInference v = getParameter(2);
		int chunk = 10;
		
		context.getLog().log(Level.INFO, "Calibrate noise model");
		
		ArrayList<SingleNoiseModel> total = new ArrayList<>();
		RiboUtils.processCodonsSink(prefix+".codons.bin", "Noise model", context.getProgress(), null, nthreads, chunk, SingleNoiseModel.class, 
			ei->ei.map(o->new ImmutableReferenceGenomicRegion<SingleNoiseModel>(o.getReference(),o.getRegion(),v.computeNoise(o))),
			n->{
				if (n.getData()!=null)
					total.add(n.getData());
			});
		
		Collections.sort(total);
		NoiseModel noise = new NoiseModel(total.toArray(new SingleNoiseModel[0]));
		
		PageFileWriter fm = new PageFileWriter(prefix+".noise.model");
		noise.serialize(fm);
		fm.close();
		
		
		
		return null;
	}
	

}
