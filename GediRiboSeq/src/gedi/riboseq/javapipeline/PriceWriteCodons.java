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
import gedi.riboseq.utils.RiboUtils;
import gedi.util.FileUtils;
import gedi.util.datastructure.array.MemoryFloatArray;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.array.SparseMemoryFloatArray;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.datastructure.tree.redblacktree.IntervalTreeSet;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.program.GediProgram;
import gedi.util.program.GediProgramContext;
import gedi.util.userInteraction.progress.NoProgress;
import gedi.util.userInteraction.progress.Progress;

public class PriceWriteCodons extends GediProgram {

	public PriceWriteCodons(PriceParameterSet params) {
		addInput(params.prefix);
		addInput(params.nthreads);
		addInput(params.codons);
		addInput(params.orfinference);
		addOutput(params.indices);
	}
	
	public String execute(GediProgramContext context) throws IOException {
		
		String prefix = getParameter(0);
		int nthreads = getIntParameter(1);
		int chunk = 10;
		
		OrfInference orfi = getParameter(3);
		
		
		context.getLog().log(Level.INFO, "Writing codon index");
		String uprefix = prefix;
		int unthreads = nthreads;
		int uchunk = chunk;
		
		try {
			if (new File(uprefix+".codons.cit").exists())
				new File(uprefix+".codons.cit").delete();
			
			CenteredDiskIntervalTreeStorage<SparseMemoryFloatArray> cit = new CenteredDiskIntervalTreeStorage<>(uprefix+".codons.cit",SparseMemoryFloatArray.class);
			RiboUtils.processCodons(uprefix+".codons.bin", "Viewer indices", context.getProgress(), null, unthreads, uchunk, SparseMemoryFloatArray.class, 
					ei->codonsToArray(ei, orfi.getMinActivity()),
					cit::fill);
			
		} catch (IOException e) {
			throw new RuntimeException("Could not write codon index!",e);
		}
		
		
		
		return null;
	}
	
	
	private static ExtendedIterator<ImmutableReferenceGenomicRegion<SparseMemoryFloatArray>> codonsToArray(ExtendedIterator<ImmutableReferenceGenomicRegion<IntervalTreeSet<Codon>>> ei, double minActi) {
		
		return ei.unfold(r->EI.wrap(r.getData()).<ImmutableReferenceGenomicRegion<SparseMemoryFloatArray>>map(codon->{
					GenomicRegion codReg = r.map(codon);
					SparseMemoryFloatArray a = new SparseMemoryFloatArray(codon.activity.length);
					for (int c=0; c<a.length(); c++)
						if (codon.activity[c]<minActi)
							a.setFloat(c, (float) codon.activity[c]);
					
					return new ImmutableReferenceGenomicRegion<SparseMemoryFloatArray>(r.getReference(), codReg,a);
				}
				)
		);
	}
	

}
