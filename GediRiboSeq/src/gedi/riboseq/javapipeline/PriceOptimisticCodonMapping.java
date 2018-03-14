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
import java.util.function.ToDoubleFunction;
import java.util.logging.Level;

import gedi.app.extension.ExtensionContext;
import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.data.numeric.diskrmq.DiskGenomicNumericBuilder;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.genomic.Genomic;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.GenomicRegionStorageCapabilities;
import gedi.core.region.GenomicRegionStorageExtensionPoint;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
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
import gedi.util.datastructure.array.SparseMemoryFloatArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.datastructure.tree.redblacktree.IntervalTreeSet;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.randomaccess.PageFile;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.math.stat.testing.MultipleTestingCorrection;
import gedi.util.mutable.MutableInteger;
import gedi.util.program.GediProgram;
import gedi.util.program.GediProgramContext;
import gedi.util.program.parametertypes.StorageParameterType;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;

public class PriceOptimisticCodonMapping extends GediProgram {

	public PriceOptimisticCodonMapping(PriceParameterSet params) {
	
		addInput(params.prefix);
		addInput(params.model);
		addInput(params.genomic);
		addInput(params.filter);
		addInput(params.delta);
		addInput(params.clusters);
		addInput(params.nthreads);
		addInput(params.orfinference);
		addInput(params.orfstmp);
		
		setRunFlag(params.opt);
		
		addOutput(params.optcodons);
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
		
		File afile = getParameter(8);
		
		MemoryIntervalTreeStorage<?> allowed = new CenteredDiskIntervalTreeStorage<>(afile.getAbsolutePath()).toMemory();
		
		CodonInference ci = new CodonInference(model,g)
		.setFilter(filter)
		.setRegularization(delta)
		.setAllowedOrfs(allowed,0.0);
		
		
		context.getLog().log(Level.INFO, "Optimistically map codons to identified or annotated ORFs");
		
		
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
		
		ExtendedIterator<ImmutableReferenceGenomicRegion<SparseMemoryFloatArray>> ait = codonsToArray(pit, v.getMinActivity());
		
		if (new File(prefix+".opt.codons.cit").exists())
			new File(prefix+".opt.codons.cit").delete();
		
		CenteredDiskIntervalTreeStorage<SparseMemoryFloatArray> cit = new CenteredDiskIntervalTreeStorage<>(prefix+".opt.codons.cit",SparseMemoryFloatArray.class);
		cit.fill(ait);
		
		progress.finish();
		
		context.getLog().log(Level.INFO, "Optimistically map codons to identified or annotated ORFs");
		
		
		try {
			PriceCodonViewerIndices.writeViewerIndices(prefix+".opt", cit,context.getProgress());
			
		} catch (IOException e) {
			throw new RuntimeException("Could not write viewer index!",e);
		}
		
		
		return null;
	}
	
	private static ExtendedIterator<ImmutableReferenceGenomicRegion<SparseMemoryFloatArray>> codonsToArray(ExtendedIterator<ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>>> ei, double minActi) {
		
		return ei.unfold(r->EI.wrap(r.getData()).<ImmutableReferenceGenomicRegion<SparseMemoryFloatArray>>map(codon->{
					GenomicRegion codReg = r.map(codon);
					SparseMemoryFloatArray a = new SparseMemoryFloatArray(codon.activity.length);
					for (int c=0; c<a.length(); c++)
						if (codon.activity[c]>=minActi)
							a.setFloat(c, (float) codon.activity[c]);
					
					return new ImmutableReferenceGenomicRegion<SparseMemoryFloatArray>(r.getReference(), codReg,a);
				}
				)
		);
	}

}
