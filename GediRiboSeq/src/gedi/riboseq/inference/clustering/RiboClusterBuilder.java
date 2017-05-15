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

package gedi.riboseq.inference.clustering;

import gedi.app.extension.ExtensionContext;
import gedi.core.data.annotation.Transcript;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.GenomicRegionStorageCapabilities;
import gedi.core.region.GenomicRegionStorageExtensionPoint;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.feature.GenomicRegionFeatureProgram;
import gedi.core.region.feature.cluster.ClusterReads;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.core.workspace.loader.WorkspaceItemLoaderExtensionPoint;
import gedi.util.FunctorUtils;
import gedi.util.FunctorUtils.MergeIterator;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.userInteraction.progress.Progress;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RiboClusterBuilder {
	
	public static final Logger log = Logger.getLogger( RiboClusterBuilder.class.getName() );
	
	private GenomicRegionStorage<AlignedReadsData> reads;
	private Predicate<ReferenceGenomicRegion<AlignedReadsData>> filter;
	private MemoryIntervalTreeStorage<Transcript> annotation;
	private int minRegionCount;
	private int minReadCount;
	private String prefix;
	private Progress progress;
	private int closeGaps = 50;
	private int nthreads;
	
	public RiboClusterBuilder(String prefix,
			GenomicRegionStorage<AlignedReadsData> reads,
			Predicate<ReferenceGenomicRegion<AlignedReadsData>> filter,
			MemoryIntervalTreeStorage<Transcript> annotation,
			int minRegionCount, int minReadCount, Progress progress, int nthreads) {
		this.prefix = prefix;
		this.reads = reads;
		this.filter = filter;
		this.annotation = annotation;
		this.minRegionCount = minRegionCount;
		this.minReadCount = minReadCount;
		this.progress = progress;
		this.nthreads = nthreads;
	}


	@SuppressWarnings({ "rawtypes", "unchecked" })
	public MemoryIntervalTreeStorage<RiboClusterInfo> build() throws IOException {
		
		if (new File(prefix+".clusters.cit").exists()) {
			log.log(Level.INFO, "Using saved clusters: "+prefix+".clusters.cit");
			Path p = Paths.get(prefix+".clusters.cit");
			return ((GenomicRegionStorage<RiboClusterInfo>) WorkspaceItemLoaderExtensionPoint.getInstance().get(p).load(p)).toMemory();
			
//			return new CenteredDiskIntervalTreeStorage(prefix+".clusters.cit").toMemory();
		}
		
		log.log(Level.INFO, "Clustering reads");
		
		GenomicRegionFeatureProgram<AlignedReadsData> program = new GenomicRegionFeatureProgram<AlignedReadsData>();
		program.setCheckSorting(true);
		program.setThreads(nthreads);
		ClusterReads clustering = new ClusterReads(prefix+".clusters.csv");
		clustering.setDataToCounts((ard,a)->{
			if (a==null) a = NumericArray.createMemory(4, NumericArrayType.Double);
			a.add(0,1);
			AlignedReadsData aa = (AlignedReadsData) ard;
			double totalDiv = 0;
			double totalSum = 0;
			double totalUnique = 0;
			for (int i=0; i<aa.getDistinctSequences(); i++) {
				totalUnique += aa.getTotalCountForDistinct(i, ReadCountMode.Unique);
				totalDiv += aa.getTotalCountForDistinct(i, ReadCountMode.Weight);
				totalSum += aa.getTotalCountForDistinct(i, ReadCountMode.All);
			}
			
			a.add(1,totalDiv);
			a.add(2,totalSum);
			a.add(3,totalUnique);
			return a;
		});
		program.add(clustering);
		
		program.begin();
		progress.init();
		reads.iterateReferenceGenomicRegions().forEachRemaining(r->{
			if (filter==null || filter.test(r))
				program.accept(r);
			progress.incrementProgress();
		});
		progress.finish();
		program.end();
		
		MemoryIntervalTreeStorage<RiboClusterInfo> re = new MemoryIntervalTreeStorage<RiboClusterInfo>(RiboClusterInfo.class);
		re.fill(new LineOrientedFile(prefix+".clusters.csv").lineIterator()
				.skip(1)
				.map(l->StringUtils.split(l, '\t'))
				.map(a->new MutableReferenceGenomicRegion<RiboClusterInfo>().parse(a[0], new RiboClusterInfo(
						(int)Double.parseDouble(a[1]),
						Double.parseDouble(a[2]),
						(int)Double.parseDouble(a[3]),
						(int)Double.parseDouble(a[4])
						)))
				);
		
		log.log(Level.INFO, String.format("Found %d clusters without looking at annotation",re.size()));
		
		
		GenomicRegionFeatureProgram<Object> merger = new GenomicRegionFeatureProgram<Object>();
		merger.setCheckSorting(true);
		merger.setThreads(nthreads);
		clustering = new ClusterReads(prefix+".clusters.csv");
		clustering.setTolerance(closeGaps);
		clustering.setDataToCounts((d,a)->{
			if (a==null) a = NumericArray.createMemory(4, NumericArrayType.Double);
			if (d instanceof RiboClusterInfo) { 
				a.add(0,((RiboClusterInfo)d).getRegionCount());
				a.add(1,((RiboClusterInfo)d).getTotalReadCountDivided());
				a.add(2,((RiboClusterInfo)d).getTotalReadCountSum());
				a.add(3,((RiboClusterInfo)d).getTotalUniqueMappingReadCount());
			}
			return a;
		});
		merger.add(clustering);
		
		merger.begin();
		progress.init();
		progress.setCount((int) re.size());
		
		MergeIterator bothit = EI.wrap(re.iterateReferenceGenomicRegions())
					.merge(FunctorUtils.naturalComparator(),(ExtendedIterator)EI.wrap(annotation.iterateReferenceGenomicRegions()));
		
		bothit.forEachRemaining(r->{
			merger.accept((ReferenceGenomicRegion<Object>) r);
			if (((ReferenceGenomicRegion<Object>) r).getData() instanceof RiboClusterInfo)
				progress.incrementProgress();
		});
		
		progress.finish();
		merger.end();
		
		
		re = new MemoryIntervalTreeStorage<RiboClusterInfo>(RiboClusterInfo.class);
		re.fill(new LineOrientedFile(prefix+".clusters.csv").lineIterator()
				.skip(1)
				.map(l->StringUtils.split(l, '\t'))
				.map(a->new MutableReferenceGenomicRegion<RiboClusterInfo>().parse(a[0], new RiboClusterInfo(
						(int)Double.parseDouble(a[1]),
						Double.parseDouble(a[2]),
						(int)Double.parseDouble(a[3]),
						(int)Double.parseDouble(a[4])
						)))
				.filter(rgr->rgr.getData().getRegionCount()>0)
				);
		
		log.log(Level.INFO, String.format("Found %d clusters after looking at annotation",re.size()));
		
		
		re = re.ei()
				.filter(rgr->rgr.getData().getRegionCount()>=minRegionCount && rgr.getData().getTotalReadCountDivided()>=minReadCount)
				.reduce(new MemoryIntervalTreeStorage<RiboClusterInfo>(RiboClusterInfo.class), (c,s)->{s.add(c);return s;});
		
		log.log(Level.INFO, String.format("Found %d clusters after filtering with read/region count",re.size()));
		
		
		GenomicRegionStorage cl = GenomicRegionStorageExtensionPoint.getInstance().get(new ExtensionContext().add(String.class, prefix+".clusters").add(Class.class, RiboClusterInfo.class), GenomicRegionStorageCapabilities.Disk, GenomicRegionStorageCapabilities.Fill);
		cl.fill(re);
//		new CenteredDiskIntervalTreeStorage(prefix+".clusters.cit", RiboClusterInfo.class).fill(re);
		
		return re;
	}

}
