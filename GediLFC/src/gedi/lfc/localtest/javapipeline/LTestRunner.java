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
package gedi.lfc.localtest.javapipeline;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.ScriptException;

import cern.colt.bitvector.BitVector;
import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.data.annotation.Transcript;
import gedi.core.data.numeric.diskrmq.DiskGenomicNumericBuilder;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.genomic.Genomic;
import gedi.core.region.GenomicRegionPosition;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.feature.AlignedReadsDataToFeatureProgram;
import gedi.core.region.feature.GenomicRegionFeatureProgram;
import gedi.core.region.feature.features.AbsolutePosition;
import gedi.core.region.feature.features.AnnotationFeature;
import gedi.core.region.feature.output.FeatureStatisticOutput;
import gedi.core.region.feature.special.Downsampling;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.lfc.localtest.LocalCoverageTest;
import gedi.lfc.localtest.LocalTestResult;
import gedi.util.ArrayUtils;
import gedi.util.FileUtils;
import gedi.util.FunctorUtils.DemultiplexIterator;
import gedi.util.SequenceUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.datastructure.dataframe.DataFrame;
import gedi.util.datastructure.tree.redblacktree.IntervalTreeSet;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.randomaccess.PageFileWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializableSerializer;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.LineWriter;
import gedi.util.io.text.tsv.formats.Csv;
import gedi.util.mutable.MutableDouble;
import gedi.util.program.GediParameter;
import gedi.util.program.GediProgram;
import gedi.util.program.GediProgramContext;
import gedi.util.r.RRunner;
import gedi.util.userInteraction.progress.Progress;

public class LTestRunner extends GediProgram {

	
	
	public LTestRunner(LTestParameterSet params) {
		addInput(params.nthreads);
		addInput(params.genomic);
		addInput(params.reads);
		addInput(params.minlen);
		addInput(params.downsampling);
		
		addInput(params.prefix);
		
		addOutput(params.rmqFile);
		addOutput(params.outTable);
	}
	
	
	
	public String execute(GediProgramContext context) throws IOException {
		
		int nthreads = getIntParameter(0);
		Genomic genomic = getParameter(1);
		GenomicRegionStorage<AlignedReadsData> reads = getParameter(2);
		int minlen = getIntParameter(3);
		Downsampling downsampling = getParameter(4);
				
		context.getLog().info("Running tests for each gene...");
		
		LineWriter writer = new LineOrientedFile(getOutputFile(1).getPath()).write();
		writer.write("Location\tGene\tp value\n");
		
		LocalCoverageTest collector = new LocalCoverageTest(genomic, reads, minlen, downsampling);
		
		DiskGenomicNumericBuilder rmq = new DiskGenomicNumericBuilder(getOutputFile(0).getPath(), false);
		
		genomic.getGenes().ei()
			.parallelized(nthreads, 5, ei->ei.map(collector::test))
			.progress(context.getProgress(), (int)genomic.getGenes().size(), (r)->"Writing "+r.getGene())
			.filter(g->g.getLocalPvalues().size()>0 && !Double.isNaN(g.getPvalue()))
			.sideEffect(lt->writer.writef2("%s\t%s\t%.5g\n", lt.getGene().toLocationString(),lt.getGene().getData(), lt.getPvalue()))
			.unfold(lt->lt.getLocalPvalues().ei())
			.forEachRemaining(r->rmq.addValueEx(r.getReference(), r.getRegion().getStart(), -Math.log10(r.getData().N)));
	
		writer.close();
		
		context.getLog().info("Finishing index...");
		rmq.build();
		
		return null;
	}


	
	
}
