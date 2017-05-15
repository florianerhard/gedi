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

package gedi.ambiguity.clustering;

import gedi.centeredDiskIntervalTree.CenteredDiskIntervalTreeStorage;
import gedi.core.data.annotation.Transcript;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.feature.GenomicRegionFeatureProgram;
import gedi.core.region.feature.cluster.ClusterReads;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.ArrayUtils;
import gedi.util.FunctorUtils;
import gedi.util.FunctorUtils.MergeIterator;
import gedi.util.StringUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.collections.doublecollections.DoubleArrayList;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.mutable.MutableDouble;
import gedi.util.mutable.MutableInteger;
import gedi.util.userInteraction.progress.Progress;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReadClusterBuilder {
	
	public static final Logger log = Logger.getLogger( ReadClusterBuilder.class.getName() );
	
	private CenteredDiskIntervalTreeStorage<AlignedReadsData> reads;
	private MemoryIntervalTreeStorage<Transcript> annotation;
	private int minRegionCount;
	private int minReadCount;
	private int context;
	private Progress progress;
	private boolean useCit;
	
	public ReadClusterBuilder(
			CenteredDiskIntervalTreeStorage<AlignedReadsData> reads,
			MemoryIntervalTreeStorage<Transcript> annotation,
			int minRegionCount, int minReadCount, int context, boolean useCit, Progress progress) {
		this.reads = reads;
		this.annotation = annotation;
		this.minRegionCount = minRegionCount;
		this.minReadCount = minReadCount;
		this.context = context;
		this.useCit = useCit;
		this.progress = progress;
	}


	@SuppressWarnings({ "rawtypes", "unchecked" })
	public MemoryIntervalTreeStorage<ClusterInfo> build(String outputFile) throws IOException {
		
		if (useCit && new File(reads.getPath()+".clusters.cit").exists()) {
			log.log(Level.INFO, "Using saved clusters: "+reads.getPath()+".clusters.cit");
			return new CenteredDiskIntervalTreeStorage(reads.getPath()+".clusters.cit").toMemory();
		}
		
		GenomicRegionFeatureProgram<AlignedReadsData> program = new GenomicRegionFeatureProgram<AlignedReadsData>();
		program.setCheckSorting(true);
		ClusterReads clustering = new ClusterReads(outputFile);
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
//				if (aa.getMultiplicity(i)==1)
//					totalUnique += aa.getSumCount(i);
//				totalDiv += aa.getSumCount(i, true);
//				totalSum += aa.getSumCount(i, false);
			}
			
			a.add(1,totalDiv);
			a.add(2,totalSum);
			a.add(3,totalUnique);
			return a;
		});
		program.add(clustering);
		
		program.begin();
		progress.init();
		progress.setCount((int) reads.size());
		reads.iterateReferenceGenomicRegions().forEachRemaining(r->{
			program.accept(r);
			progress.incrementProgress();
		});
		progress.finish();
		program.end();
		
		
		MemoryIntervalTreeStorage<ClusterInfo> re = new MemoryIntervalTreeStorage<ClusterInfo>(ClusterInfo.class);
		re.fill(new LineOrientedFile(outputFile).lineIterator()
				.skip(1)
				.map(l->StringUtils.split(l, '\t'))
				.map(a->new MutableReferenceGenomicRegion<ClusterInfo>().parse(a[0], new ClusterInfo(
						(int)Double.parseDouble(a[1]),
						Double.parseDouble(a[2]),
						(int)Double.parseDouble(a[3]),
						(int)Double.parseDouble(a[4])
						)))
				);
		
		log.log(Level.INFO, String.format("Found %d clusters without looking at annotation",re.size()));
		
		
		GenomicRegionFeatureProgram<Object> merger = new GenomicRegionFeatureProgram<Object>();
		merger.setCheckSorting(true);
		clustering = new ClusterReads(outputFile);
		clustering.setDataToCounts((d,a)->{
			if (a==null) a = NumericArray.createMemory(4, NumericArrayType.Double);
			if (d instanceof ClusterInfo) { 
				a.add(0,((ClusterInfo)d).getRegionCount());
				a.add(1,((ClusterInfo)d).getTotalReadCountDivided());
				a.add(2,((ClusterInfo)d).getTotalReadCountSum());
				a.add(3,((ClusterInfo)d).getTotalUniqueMappingReadCount());
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
			if (((ReferenceGenomicRegion<Object>) r).getData() instanceof ClusterInfo)
				progress.incrementProgress();
		});
		
		progress.finish();
		merger.end();
		
		
		re = new MemoryIntervalTreeStorage<ClusterInfo>(ClusterInfo.class);
		re.fill(new LineOrientedFile(outputFile).lineIterator()
				.skip(1)
				.map(l->StringUtils.split(l, '\t'))
				.map(a->new MutableReferenceGenomicRegion<ClusterInfo>().parse(a[0], new ClusterInfo(
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
				.reduce(new MemoryIntervalTreeStorage<ClusterInfo>(ClusterInfo.class), (c,s)->{s.add(c);return s;});
		
		log.log(Level.INFO, String.format("Found %d clusters after filtering with read/region count",re.size()));
		
		
		CountBuilder cb = null;
		
		progress.init();
		progress.setCount((int) reads.size());
		ExtendedIterator<ImmutableReferenceGenomicRegion<AlignedReadsData>> it = reads.ei();
		MutableInteger totalDataBytes = new MutableInteger();
		while (it.hasNext()) {
			ImmutableReferenceGenomicRegion<AlignedReadsData> n = it.next();
			
			if (cb==null || !cb.cluster.contains(n)) {
				if (cb!=null)
					totalDataBytes.N += cb.build(context);
				cb = null;
				
				ImmutableReferenceGenomicRegion<ClusterInfo> cl = checkOne(re.getReferenceRegionsIntersecting(n.getReference(), n.getRegion()));
				if (cl!=null)
					cb = new CountBuilder(cl);
			}
			
			if (cb!=null)
				cb.add(n);
			
			progress.incrementProgress().setDescription(()->n.toLocationString()+" Data bytes="+totalDataBytes.N+" Mem="+StringUtils.getHumanReadableMemory(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()));
		}
		if (cb!=null)
			cb.build(context);
		progress.finish();
		
		if (useCit)
			new CenteredDiskIntervalTreeStorage(reads.getPath()+".clusters.cit", ClusterInfo.class).fill(re);
		
		return re;
	}

	private static <T> T checkOne(List<T> l) {
		if (l.size()==0) return null;
		if (l.size()!=1) throw new RuntimeException();
		return l.get(0);
	}
	
	private static class CountBuilder {
		ImmutableReferenceGenomicRegion<ClusterInfo> cluster;
		
		GenomicRegion ambi = null;
		
		double[] neighbors;
		TreeMap<Integer,MutableDouble>[] splits;
		
		public CountBuilder(ImmutableReferenceGenomicRegion<ClusterInfo> cluster) {
			int length = cluster.getRegion().getTotalLength();
			this.cluster = cluster;
			neighbors = new double[length];
			splits = new TreeMap[length];
		}

		public void add(ImmutableReferenceGenomicRegion<AlignedReadsData> n) {
			
			ArrayGenomicRegion ind = cluster.getRegion().induce(n.getRegion());
			if (n.getData().isAnyAmbigousMapping()) 
				ambi = ambi==null?ind:ambi.union(ind);
			
			double count = n.getData().getTotalCountOverall(ReadCountMode.Weight);//n.getData().getSumTotalCountDivide();
			if (count==0) return;
			
			
			for (int p=0; p<ind.getNumParts(); p++) {
				neighbors[ind.getStart(p)]+=count;
				neighbors[ind.getStop(p)]-=count;
				if (p>0) {
					// split
					int intronStart = ind.getStop(p-1);
					int intronEnd = ind.getStart(p);
					if (splits[intronStart]==null) splits[intronStart] = new TreeMap<Integer, MutableDouble>();
					splits[intronStart].computeIfAbsent(intronEnd, l->new MutableDouble()).N+=count;
				}
			}
		}

		public int build(int context) {
			ArrayUtils.cumSumInPlace(neighbors, 1);
			
			if (ambi!=null)
				ambi = ambi.extendAll(context, context).intersect(new ArrayGenomicRegion(0,cluster.getRegion().getTotalLength()));
			
			IntArrayList neiIndex = new IntArrayList();
			DoubleArrayList neiCount = new DoubleArrayList();
			
			double last = -1;
			if (ambi!=null)
				for (int i=0; i<neighbors.length; i++) {
					if (ambi.contains(i)) {
						if (neighbors[i]!=last) {
							neiIndex.add(i);
							neiCount.add(neighbors[i]);
						}
						
						last = neighbors[i];
					}
					else 
						last = -1;
//					else {if (i>0 && ambi.contains(i-1))
//						nei.add(new int[] {i,0});
//					last = -1;}
					// unneccessary, as a position right of this i will never be queried
				}			
			
			IntArrayList splitIndex1 = new IntArrayList();
			IntArrayList splitIndex2 = new IntArrayList();
			DoubleArrayList splitCount = new DoubleArrayList();
			ArrayList<double[]> split = new ArrayList<double[]>();
			if (ambi!=null)
				for (int i=0; i<splits.length; i++)
					if (ambi.contains(i) && splits[i]!=null) {
						for (Integer p : splits[i].keySet()) {
							if (ambi.contains(p)) {
								splitIndex1.add(i);
								splitIndex2.add(p);
								splitCount.add(splits[i].get(p).N);
							}
						}
					}
						
			
			cluster.getData().setCounts(neiIndex.toIntArray(),neiCount.toDoubleArray(),splitIndex1.toIntArray(),splitIndex2.toIntArray(),splitCount.toDoubleArray());
			return (neiIndex.size()*3+split.size()*4)*4;
		}


		
	}

}
