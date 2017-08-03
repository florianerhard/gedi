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

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.genomic.Genomic;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.sequence.SequenceProvider;
import gedi.riboseq.cleavage.RiboModel;
import gedi.riboseq.inference.clustering.RiboClusterInfo;
import gedi.riboseq.inference.codon.Codon;
import gedi.riboseq.inference.codon.CodonInference;
import gedi.riboseq.inference.orf.OrfInference;
import gedi.riboseq.utils.RiboUtils;
import gedi.util.FileUtils;
import gedi.util.datastructure.tree.redblacktree.IntervalTreeSet;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.program.GediProgram;
import gedi.util.program.GediProgramContext;
import gedi.util.program.parametertypes.StorageParameterType;
import gedi.util.userInteraction.progress.Progress;

public class PriceCodonInference extends GediProgram {

	public PriceCodonInference(PriceParameterSet params) {
		addInput(params.prefix);
		addInput(params.model);
		addInput(params.genomic);
		addInput(params.filter);
		addInput(params.delta);
		addInput(params.clusters);
		addInput(params.nthreads);
		addInput(params.orfinference);
		
		
		addOutput(params.codons);
	}
	
	public String execute(GediProgramContext context) throws IOException {
		
		String prefix = getParameter(0);
		File modelFile = getParameter(1);
		Genomic g = getParameter(2);
		Predicate<ReferenceGenomicRegion<AlignedReadsData>> filter = RiboUtils.parseReadFilter(getParameter(3));
		double delta = getDoubleParameter(4);
		File clusterFile = getParameter(5);
		int nthreads = getIntParameter(6);
		int chunk = 10;
		
		RiboModel[] model = RiboModel.fromFile(modelFile.getPath(), false);
		
		
		GenomicRegionStorage<RiboClusterInfo> clusters = new StorageParameterType<RiboClusterInfo>().parse(clusterFile.getPath());
		
		
		OrfInference v = getParameter(7);
		
		CodonInference ci = new CodonInference(model,g)
		.setFilter(filter)
//		.setRho(rho)
		.setRegularization(delta);
		
		context.getLog().log(Level.INFO, "Codon inference");
		AtomicInteger count = new AtomicInteger(0);
		Progress progress = context.getProgress();
		progress.init();
		progress.setCount((int)clusters.size());
		Progress uprog = progress;
		ExtendedIterator<ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>>> pit = clusters.ei() //(test==null?clusters.ei():clusters.ei(test))//"JN555585+:107973-109008")
			.parallelized(nthreads, chunk, ei->ei
				.sideEffect(cl->{
					synchronized (uprog) {
						uprog.setDescription(()->cl.toLocationString()+" n="+count.get()).incrementProgress();
					}
				})
				.map(cl->v.codonInference(ci,count.getAndIncrement(),cl.getReference(), cl.getRegion().getStart(), cl.getRegion().getEnd()))
				.removeNulls()
				);
		
		PageFileWriter tmp = new PageFileWriter(prefix+".codons.bin");
		while (pit.hasNext()) {
			ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>> n = pit.next();
			FileUtils.writeReferenceSequence(tmp, n.getReference());
			FileUtils.writeGenomicRegion(tmp, n.getRegion());
			tmp.putCInt(n.getData().size());
			for (Codon c : n.getData()) {
				FileUtils.writeGenomicRegion(tmp, c);
				FileUtils.writeDoubleArray(tmp, c.activity);
			}
		}
		tmp.close();
		progress.finish();
		
		return null;
	}
	
	

}
