package gedi.core.data.mapper;


import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelBlockToValuesMap;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;

import java.util.Iterator;
import java.util.Map.Entry;

@GenomicRegionDataMapping(fromType=IntervalTree.class,toType=PixelBlockToValuesMap.class)
public class AlignedReadsDataToCoverageMapper implements GenomicRegionDataMapper<IntervalTree<GenomicRegion,AlignedReadsData>, PixelBlockToValuesMap>{

	private ReadCountMode mode = ReadCountMode.Weight;
	
	public void setReadCountMode(ReadCountMode mode) {
		this.mode = mode;
	}
	
	
//	private GenomicRegionDataMappingJob<IntervalTree<GenomicRegion,AlignedReadsData>, PixelBlockToValuesMap> job;
//	
//	@Override
//	public void setJob(GenomicRegionDataMappingJob<IntervalTree<GenomicRegion,AlignedReadsData>, PixelBlockToValuesMap> job) {
//		this.job = job;
//	}
//	
//	long[] nanos = new long[2];
//	double[] c = new double[2];
//	
//Random rnd = new Random();
	@Override
	public PixelBlockToValuesMap map(ReferenceSequence reference,
			GenomicRegion region,PixelLocationMapping pixelMapping,
			IntervalTree<GenomicRegion, AlignedReadsData> data) {
		if (data.isEmpty()) return new PixelBlockToValuesMap();
		
		int numCond = data.firstEntry().getValue().getNumConditions();
		
		PixelBlockToValuesMap re = new PixelBlockToValuesMap(pixelMapping, numCond, NumericArrayType.Double);

//		int index;
//		long start = System.nanoTime();
		
		
//		if (rnd.nextBoolean()) {
			Iterator<Entry<GenomicRegion, AlignedReadsData>> it = data.entrySet().iterator();
			while (it.hasNext()) {
				Entry<GenomicRegion, AlignedReadsData> e = it.next();
				
				GenomicRegion reg = e.getKey();
				AlignedReadsData ard = e.getValue();
				
				for (int p=0; p<reg.getNumParts(); p++) {
					
					int block = pixelMapping.getBlockForBp(reference, reg.getStart(p));
					if (block==-1) block = 0;
					if (block<re.size()) {
						NumericArray vals = re.getValues(block);
						for (int c=0; c<numCond; c++)
							vals.add(c, ard.getTotalCountForCondition(c, mode));
					}
					
					block = pixelMapping.getBlockForBp(reference, reg.getEnd(p));
					if (block==-1) block = 0;
					if (block<re.size()) {
						NumericArray vals = re.getValues(block);
						for (int c=0; c<numCond; c++)
							vals.add(c, -ard.getTotalCountForCondition(c, mode));
					}
				}
			}
			
			for (int i=1; i<pixelMapping.size(); i++) 
				re.getValues(i).add(re.getValues(i-1));

//			This is about 10 times slower!
			
//			index = 0;
//		} else {
//		
//			for (int b=0; b<pixelMapping.size(); b++) {
//				PixelLocationMappingBlock block = pixelMapping.get(b);
//				NumericArray vals = re.getValues(b);
//				
//				data.getIntervalsIntersecting(block.getStartBp(), block.getStopBp(), e->{
//					if (e.getKey().intersects(block.getStartBp(), block.getStopBp()+1)) {
//						AlignedReadsData ard = e.getValue();
//						for (int c=0; c<numCond; c++)
//							vals.add(c, ard.getTotalCount(c));
//					}
//				});
//			}
//			index = 1;
//		}
//		nanos[index]+=System.nanoTime()-start;
//		c[index]++;
//		
//		if (rnd.nextDouble()<0.1)
//			System.out.println(job.getId()+"\t"+(nanos[0]/c[0])+"\t"+(nanos[1]/c[1])+"\t"+((nanos[1]/c[1])/(nanos[0]/c[0])));
		
		return re;
	}
//
//	
//	@Override
//	public GenomicNumericProvider map(ReferenceSequence reference,
//			GenomicRegion region,PixelLocationMapping pixelMapping,
//			IntervalTree<GenomicRegion, AlignedReadsData> data) {
//		if (data.isEmpty()) return new EmptyGenomicNumericProvider();
//		
//		int numCond = data.firstEntry().getValue().getNumConditions();
//		NumericArray[] re = new NumericArray[numCond];
//		
//		for (int i=0; i<re.length; i++) {
//			Coverage<GenomicRegion> c = new Coverage<GenomicRegion>();
//			int condition = i;
//			c.setCounter(r->data.get(r).getTotalCount(condition));
//			double[] total = new double[region.getTotalLength()];
//			for (int p=0; p<region.getNumParts(); p++) {
//				double[] cov = c.compute(data.keySet(), region.getStart(i), region.getEnd(i));
//				System.arraycopy(cov, 0, total, region.induce(region.getStart(p)), region.getLength(p));
//			}
//			re[i] = NumericArray.wrap(total);
//		}
//		
//		
//		return new DenseGenomicNumericProvider(reference, region, re);
//	}
//



}
