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
package executables;

import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.AlignedReadsDataFactory;
import gedi.core.data.reads.AlignedReadsDataMerger;
import gedi.core.data.reads.DefaultAlignedReadsData;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.util.FileUtils;
import gedi.util.FunctorUtils;
import gedi.util.FunctorUtils.ParallellIterator;
import gedi.util.StringUtils;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.functions.IterateIntoSink;
import gedi.util.io.randomaccess.serialization.ReferenceGenomicRegionSerializer;
import gedi.util.orm.Orm;
import gedi.util.userInteraction.progress.ConsoleProgress;
import gedi.util.userInteraction.progress.ProgressManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class SplitCIT {

	public static void main(String[] args) throws IOException, InterruptedException {
		
		boolean progress = args.length>0 && args[0].equals("-p");
		int inp = progress?1:0;

		if (inp>=args.length) {
			usage();
			System.exit(1);
		}
		
		CenteredDiskIntervalTreeStorage<DefaultAlignedReadsData> in = new CenteredDiskIntervalTreeStorage<>(args[inp]); 
		HashMap<String,CenteredDiskIntervalTreeStorage<DefaultAlignedReadsData>> outs = new HashMap<>();  
		HashMap<String,IterateIntoSink<ImmutableReferenceGenomicRegion<DefaultAlignedReadsData>>> sinks = new HashMap<>();  
		
		ProgressManager pman = new ProgressManager();
		// sink = new IterateIntoSink<>(r->out.fill(r,progress?new ConsoleProgress(System.err,pman):null));
		
		ReferenceSequence[] refs = in.getReferenceSequences().toArray(new ReferenceSequence[0]);
		Arrays.sort(refs);
		for (ReferenceSequence r : refs) {
			
			if (progress)
				System.err.println("Processing "+r);
			in.ei(r)
				.iff(progress, ei->ei.progress(new ConsoleProgress(System.err,pman),(int)in.size(r),rr->rr.toLocationString()))
				.forEachRemaining(rr->{
						try {
							String name = rr.getReference().getName();
							int under =name.lastIndexOf('_');
							if (under==-1) throw new RuntimeException("Can only split cit based on xxx_yyy chromosomes!");
							String pref = StringUtils.trim(name.substring(0, under),'_');
							name = name.substring(under+1);
							rr = new ImmutableReferenceGenomicRegion<>(Chromosome.obtain(name,rr.getReference().getStrand()), rr.getRegion(), rr.getData());
							CenteredDiskIntervalTreeStorage<DefaultAlignedReadsData> out = outs.computeIfAbsent(pref, x->{
								try {
									return new CenteredDiskIntervalTreeStorage<>(FileUtils.insertSuffixBeforeExtension(args[inp], "."+pref),in.getType(),in.isCompressed());
								} catch (IOException e) {
									throw new RuntimeException(e);
								}
							});
							IterateIntoSink<ImmutableReferenceGenomicRegion<DefaultAlignedReadsData>> sink = sinks.computeIfAbsent(pref, x->new IterateIntoSink<>(rrr->out.fill(rrr,progress?new ConsoleProgress(System.err,pman):null)));
							
							sink.put(rr);
						} catch (InterruptedException e) {}
					});
			
		}
		for (IterateIntoSink<ImmutableReferenceGenomicRegion<DefaultAlignedReadsData>> sink : sinks.values())
			sink.finish();
		for (CenteredDiskIntervalTreeStorage<DefaultAlignedReadsData> out : outs.values())
			out.setMetaData(in.getMetaData());
	}
	
	

	private static void usage() {
		System.out.println("SplitCIT [-p] <cit>\n\n  -p shows progress");
	}
	
}
