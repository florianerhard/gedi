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

package gedi.core.data.mapper;

import gedi.core.data.numeric.GenomicNumericProvider.SpecialAggregators;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelBlockToValuesMap;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.util.SequenceUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;

import java.util.Iterator;
import java.util.Map.Entry;

@GenomicRegionDataMapping(fromType=IntervalTree.class,toType=PixelBlockToValuesMap.class)
public class AlignedReadsDataToMismatchesMapper implements GenomicRegionDataMapper<IntervalTree<GenomicRegion,AlignedReadsData>, PixelBlockToValuesMap>{

	private ReadCountMode mode = ReadCountMode.Divide;
	
	private SpecialAggregators aggregator = SpecialAggregators.Sum; 
	private MismatchOutputType type = MismatchOutputType.Read;
	
	public enum MismatchOutputType {
		Conditions{

			@Override
			public int getIndex(int condition, char genomic, char read) {
				return condition;
			}

			@Override
			public int getTotal(int conditions) {
				return conditions;
			}
			
		}, Read {

			@Override
			public int getIndex(int condition, char genomic, char read) {
				return SequenceUtils.inv_nucleotides[read];
			}
			@Override
			public int getTotal(int conditions) {
				return 4;
			}
		};

		public abstract int getIndex(int condition, char genomic, char read);

		public abstract int getTotal(int conditions);
	}

	public void setType(MismatchOutputType type) {
		this.type = type;
	}


	public void setAggregator(SpecialAggregators aggregator) {
		this.aggregator = aggregator;
	}
	
	private GenomicRegionDataMappingJob<IntervalTree<GenomicRegion,AlignedReadsData>, PixelBlockToValuesMap> job;
	
	@Override
	public void setJob(GenomicRegionDataMappingJob<IntervalTree<GenomicRegion,AlignedReadsData>, PixelBlockToValuesMap> job) {
		this.job = job;
	}
	
	public void setReadCountMode(ReadCountMode mode) {
		this.mode = mode;
	}

	
	@Override
	public PixelBlockToValuesMap map(ReferenceSequence reference,
			GenomicRegion region,PixelLocationMapping pixelMapping,
			IntervalTree<GenomicRegion, AlignedReadsData> data) {
		if (data.isEmpty()) return new PixelBlockToValuesMap();
		
		int numCond = data.firstEntry().getValue().getNumConditions();

		PixelBlockToValuesMap re = new PixelBlockToValuesMap(pixelMapping, type.getTotal(numCond), Double.NaN);
		MutableReferenceGenomicRegion rgr = new MutableReferenceGenomicRegion().setReference(data.getReference());
		int[] count = new int[re.size()];
		
		Iterator<Entry<GenomicRegion, AlignedReadsData>> it = data.entrySet().iterator();
		while (it.hasNext()) {
			Entry<GenomicRegion, AlignedReadsData> e = it.next();
			
			AlignedReadsData ard = e.getValue();
			rgr.setRegion(e.getKey());
					
			for (int d=0; d<ard.getDistinctSequences(); d++) {
				
				for (int v=0; v<ard.getVariationCount(d); v++) {
					
					if (ard.isMismatch(d, v)) {
//						System.out.println(rgr.map(ard.getMismatchPos(d, v))+"\t"+rgr+"\t"+ard.getVariation(d,v));
						int block = pixelMapping.getBlockForBp(reference, rgr.map(ard.getMismatchPos(d, v)));
						if (block==-1 || block>=pixelMapping.size()) continue;
						
						NumericArray vals = re.getValues(block);
						for (int c=0; c<numCond; c++) {
							
							int t = type.getIndex(c,ard.getMismatchGenomic(d, v).charAt(0),ard.getMismatchRead(d, v).charAt(0));
							
							double p = vals.getDouble(t);
							if (Double.isNaN(p))
								vals.set(t, ard.getCount(d,c, mode));
							else
								vals.set(t, aggregator.increment(p, ard.getCount(d,c,mode)));
						}
						count[block]++;
						
					}
					
				}
				
			}

		}
		
		for (int i=0; i<pixelMapping.size(); i++) {
			NumericArray vals = re.getValues(i);
			for (int j=0; j<vals.length(); j++) {
				double res = aggregator.getIncrementalResult(vals.getDouble(j), count[i]);
				vals.set(j, res);
			}
		}



		return re;
	}


	//
	//	@Override
	//	public GenomicNumericProvider map(ReferenceSequence reference,
	//			GenomicRegion region,PixelLocationMapping pixelMapping,
	//			IntervalTree<GenomicRegion, AlignedReadsData> data) {
	//		if (data.isEmpty()) return new EmptyGenomicNumericProvider();
	//		
	//		int numCond=data.firstEntry().getValue().getNumConditions();
	//		double dens = data.size()/(double)region.getTotalLength();
	//		
	//		if (dens<MAX_DENSE) {
	//			TreeMap<Integer,NumericArray> re = new TreeMap<Integer,NumericArray>();
	//			data.getIntervalsIntersecting(region, e->{
	//				int pos = referencePosition.position(reference, e.getKey(),offset);
	//				if (region.contains(pos)) {
	//					NumericArray pr = re.get(pos);
	//					if (pr==null) re.put(pos, pr = NumericArray.createMemory(numCond,NumericArrayType.Integer));
	//					e.getValue().addTotalCount(pr);
	//				}
	//			});
	//			return new SparseGenomicNumericProvider(reference, region, re);
	//		}
	//		
	//		NumericArray[] re = new NumericArray[numCond];
	//		for (int i=0; i<re.length; i++)
	//			re[i] = NumericArray.createMemory(region.getTotalLength(),NumericArrayType.Integer);
	//		
	//		data.getIntervalsIntersecting(region, e->{
	//			int pos = referencePosition.position(reference, e.getKey(),offset);
	//			if (region.contains(pos)) {
	//				int ind = region.induce(pos);
	//				for (int i=0; i<re.length; i++)
	//					re[i].add(ind, e.getValue().getTotalCount(i));
	//			}
	//		});
	//		
	//		return new DenseGenomicNumericProvider(reference,region,re);
	//	}


}
