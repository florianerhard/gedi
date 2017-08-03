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
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.GenomicRegionStorageCapabilities;
import gedi.core.region.GenomicRegionStorageExtensionPoint;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
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

public class PriceReassignCodons extends GediProgram {

	public PriceReassignCodons(PriceParameterSet params) {
		addInput(params.prefix);
		addInput(params.orfinference);
		addInput(params.nthreads);
		addInput(params.orfstmp);
		addInput(params.codons);
		addInput(params.indices);
		
		addOutput(params.orfs);
	}
	
	public String execute(GediProgramContext context) throws IOException {
		
		String prefix = getParameter(0);
		OrfInference v = getParameter(1);
		int nthreads = getIntParameter(2);
		File tmpFile = getParameter(3);
		int chunk = 10;
		
		
		GenomicRegionStorage<PriceOrf> tmp = new StorageParameterType<PriceOrf>().parse(tmpFile.getPath());
		
		
		context.getLog().log(Level.INFO, "Reassign codons");
		GenomicRegionStorage<PriceOrf> out2 = GenomicRegionStorageExtensionPoint.getInstance().get(new ExtensionContext().add(String.class, prefix+".orfs").add(Class.class, PriceOrf.class), GenomicRegionStorageCapabilities.Disk, GenomicRegionStorageCapabilities.Fill);
		RiboUtils.processCodons(prefix+".codons.bin", "Reassign codons", context.getProgress(), null, nthreads, chunk, PriceOrf.class, 
				ei->ei.demultiplex(o->v.redistributeCodons(o, tmp.ei(o))),
				out2::fill);
		
		GenomicRegionStorage<AlignedReadsData> reads = v.getReads();
		if (reads.getMetaData()!=null && reads.getMetaData().isObject())
			out2.setMetaData(reads.getMetaData());
		
		
		if (v.getCheckOrfNames().size()>0) {
			CenteredDiskIntervalTreeStorage<MemoryFloatArray> cod = new CenteredDiskIntervalTreeStorage<>(prefix+".codons.cit");
			ToDoubleFunction<ImmutableReferenceGenomicRegion<?>> total = r->cod.ei(r).mapToDouble(c->c.getData().sum()).sum();
			
			for (String name : v.getCheckOrfNames()) 
				v.iterateChecked(name).map(r->String.format("%s\t%s\t%s\t%s\t%.0f\t%d",
						r.getReference()+":"+r.map(r.getRegion().getTotalLength()-1),
						r.toLocationString(),
						r.getData().Item1.toString(),
						r.getData().Item2.toString(),
						total.applyAsDouble(r),
						r.getRegion().getTotalLength()/3-1
						)).print("Stopid\tLocation\tData\tStatus\tExpression\taaLength",prefix+"."+name+".checked.tsv");
			
			out2.ei().map(r->String.format("%s\t%s\t%s\t%s\t%.0f\t%d",
					r.getReference()+":"+r.map(r.getRegion().getTotalLength()-1),
					r.toLocationString(),
					"",
					"Detected",
					total.applyAsDouble(r),
					r.getRegion().getTotalLength()/3-1
					)).print("Stopid\tLocation\tData\tStatus\tExpression\taaLength",prefix+".PRICE.checked.tsv");
		}
		
		
		return null;
	}
	

}
