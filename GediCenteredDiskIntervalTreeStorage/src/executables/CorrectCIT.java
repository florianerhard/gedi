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
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.util.FileUtils;
import gedi.util.FunctorUtils;
import gedi.util.FunctorUtils.ParallellIterator;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class CorrectCIT {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException, InterruptedException {
		
		boolean progress = args.length>0 && args[0].equals("-p");
		int inp = progress?1:0;

		if (inp>=args.length) {
			usage();
			System.exit(1);
		}
		
		boolean tmp = inp+1==args.length;
		
		CenteredDiskIntervalTreeStorage<DefaultAlignedReadsData> in = new CenteredDiskIntervalTreeStorage<>(args[inp]); 
		CenteredDiskIntervalTreeStorage<DefaultAlignedReadsData> out = new CenteredDiskIntervalTreeStorage<>(tmp?FileUtils.insertSuffixBeforeExtension(args[inp], ".correct"):args[inp+1],in.getType(),in.isCompressed()); 
		DynamicObject gi = in.getRandomRecord().getGlobalInfo();
		Supplier<DefaultAlignedReadsData> creator = ()->new DefaultAlignedReadsData();
		
		ProgressManager pman = new ProgressManager();
		IterateIntoSink<ImmutableReferenceGenomicRegion<DefaultAlignedReadsData>> sink = new IterateIntoSink<>(r->out.fill(r,progress?new ConsoleProgress(System.err,pman):null));
		
		ReferenceSequence[] refs = in.getReferenceSequences().toArray(new ReferenceSequence[0]);
		Arrays.sort(refs);
		for (ReferenceSequence r : refs) {
			
			if (progress)
				System.err.println("Processing "+r);
			in.ei(r)
				.iff(progress, ei->ei.progress(new ConsoleProgress(System.err,pman),(int)in.size(r),rr->rr.toLocationString()))
				.sort(new ReferenceGenomicRegionSerializer<>(r, gi, creator), FunctorUtils.naturalComparator())
				.multiplex(FunctorUtils.naturalComparator(), CorrectCIT::merge)
				.iff(progress && in.size(r)>64*1000, ei->ei.progress(new ConsoleProgress(System.err,pman),(int)in.size(r),rr->rr.toLocationString()))
				.forEachRemaining(rr->{
						try {
						sink.put(rr);
						} catch (InterruptedException e) {}
					});
			
		}
		sink.finish();
		
		if (!tmp) out.setMetaData(in.getMetaData());
		else
			new File(out.getPath()).renameTo(new File(in.getPath()));
	}
	
	private static ImmutableReferenceGenomicRegion<DefaultAlignedReadsData> merge(List<ImmutableReferenceGenomicRegion<DefaultAlignedReadsData>> l) {
		if (l.size()==1) return l.get(0);
		AlignedReadsDataFactory fac = new AlignedReadsDataFactory(l.get(0).getData().getNumConditions());
		fac.start();
		for (ImmutableReferenceGenomicRegion<DefaultAlignedReadsData> r : l) {
			for (int d=0; d<r.getData().getDistinctSequences(); d++) {
				fac.add(r.getData(), d);
			}
		}
		fac.makeDistinct();
		return new ImmutableReferenceGenomicRegion<>(l.get(0).getReference(), l.get(0).getRegion(), fac.create());
	}
	
	
	

	private static void usage() {
		System.out.println("CorrectCIT [-p] <input> [<output>]... \n\n if output is omitted, a tmp file is written and copied onto input!\n -p shows progress");
	}
	
}
