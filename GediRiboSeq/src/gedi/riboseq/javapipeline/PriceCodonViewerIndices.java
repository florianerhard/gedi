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

public class PriceCodonViewerIndices extends GediProgram {

	public PriceCodonViewerIndices(PriceParameterSet params) {
		addInput(params.prefix);
		addInput(params.indices);
		
		addOutput(params.rmq);
	}
	
	public String execute(GediProgramContext context) throws IOException {
		
		String prefix = getParameter(0);
		
		context.getLog().log(Level.INFO, "Writing viewer index");
		String uprefix = prefix;
		
		try {
			CenteredDiskIntervalTreeStorage<SparseMemoryFloatArray> cit = new CenteredDiskIntervalTreeStorage<>(uprefix+".codons.cit");
			writeViewerIndices(uprefix, cit,context.getProgress());
			
		} catch (IOException e) {
			throw new RuntimeException("Could not write viewer index!",e);
		}
		
		
		
		return null;
	}
	
	
	
	private static void writeViewerIndicesOld(String prefix, CenteredDiskIntervalTreeStorage<SparseMemoryFloatArray> cit, Progress progress) throws IOException {
		
		int numCond = cit.getRandomRecord().length();
		
		DiskGenomicNumericBuilder codonOut = new DiskGenomicNumericBuilder(prefix+".codons.rmq");
		codonOut.setReferenceSorted(true);
		
		DiskGenomicNumericBuilder[] perCondCodonOut = new DiskGenomicNumericBuilder[numCond];
		for (int i=0; i<perCondCodonOut.length; i++) {
			perCondCodonOut[i] = new DiskGenomicNumericBuilder(prefix+"."+i+".codons.rmq");
			perCondCodonOut[i].setReferenceSorted(true);
		}
		
		float[] data = new float[3];
		for (ImmutableReferenceGenomicRegion<SparseMemoryFloatArray> c : cit.ei().progress(progress,(int)cit.size(),(r)->"Writing rmq "+r.toLocationString()).loop()) {
			double sum = c.getData().evaluate(NumericArrayFunction.Sum);
			setRmq(codonOut, c.getReference(), 
					c.getRegion().map(0),0,
					sum, data);
			setRmq(codonOut, c.getReference(), 
					c.getRegion().map(1),1,
					sum, data);
			setRmq(codonOut, c.getReference(), 
					c.getRegion().map(2),2,
					sum, data);
		}
		codonOut.build();
		for (int i=0; i<perCondCodonOut.length; i++) {
			for (ImmutableReferenceGenomicRegion<SparseMemoryFloatArray> c : cit.ei().progress(progress,(int)cit.size(),(r)->"Writing rmq "+r.toLocationString()).loop()) {
				double v = c.getData().getFloat(i);
				if (v>=0.01) {
					setRmq(perCondCodonOut[i], c.getReference(), 
							c.getRegion().map(0),0,
							v, data);
					setRmq(perCondCodonOut[i], c.getReference(), 
							c.getRegion().map(1),1,
							v, data);
					setRmq(perCondCodonOut[i], c.getReference(), 
							c.getRegion().map(2),2,
							v, data);
				}
			}
			perCondCodonOut[i].build();
		}
		
	}
	
	static void writeViewerIndices(String prefix, CenteredDiskIntervalTreeStorage<SparseMemoryFloatArray> cit, Progress progress) throws IOException {
		
		int numCond = cit.getRandomRecord().length();
		
		DiskGenomicNumericBuilder codonOut = new DiskGenomicNumericBuilder(prefix+".codons.rmq");
		codonOut.setReferenceSorted(true);
		
		
		DiskGenomicNumericBuilder[] perCondCodonOut = new DiskGenomicNumericBuilder[numCond];
		for (int i=0; i<perCondCodonOut.length; i++) {
			perCondCodonOut[i] = new DiskGenomicNumericBuilder(prefix+"."+i+".codons.rmq");
			perCondCodonOut[i].setReferenceSorted(true);
		}
		
		float[] data = new float[3];
		for (ImmutableReferenceGenomicRegion<SparseMemoryFloatArray> c : cit.ei().progress(progress,(int)cit.size(),(r)->"Writing rmq "+r.toLocationString()).loop()) {
			double sum = c.getData().evaluate(NumericArrayFunction.Sum);
			if (sum>=0.01) {
				setRmq(codonOut, c.getReference(), 
						c.getRegion().map(0),0,
						sum, data);
			}
		}
		codonOut.build();
		
		for (int i=0; i<perCondCodonOut.length; i++) {
			for (ImmutableReferenceGenomicRegion<SparseMemoryFloatArray> c : cit.ei().progress(progress,(int)cit.size(),(r)->"Writing rmq "+r.toLocationString()).loop()) {
				double v = c.getData().getFloat(i);
				if (v>=0.01) {
					setRmq(perCondCodonOut[i], c.getReference(), 
							c.getRegion().map(0),0,
							v, data);
					
				}
			}
			perCondCodonOut[i].build();
		}
		
	}
	
	private static void setRmq(DiskGenomicNumericBuilder codon, ReferenceSequence ref, int genomic, int offset, double act, float[] buff) {
		buff[(genomic-offset+3)%3] = (float)act;
		codon.addValueEx(ref, genomic, buff);
		buff[(genomic-offset+3)%3] = 0;
	}

}
