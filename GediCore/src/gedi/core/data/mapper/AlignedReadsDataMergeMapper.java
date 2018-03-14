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

import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.AlignedReadsDataMerger;
import gedi.core.data.reads.ConditionMappedAlignedReadsData;
import gedi.core.data.reads.ContrastMapping;
import gedi.core.data.reads.DefaultAlignedReadsData;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.util.ArrayUtils;
import gedi.util.FunctorUtils;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.functions.EI;
import gedi.util.mutable.MutableTriple;
import gedi.util.mutable.MutableTuple;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

@GenomicRegionDataMapping(fromType=MutableTuple.class,toType=IntervalTree.class)
public class AlignedReadsDataMergeMapper implements GenomicRegionDataMapper<MutableTuple, IntervalTree<GenomicRegion,AlignedReadsData>>{

	private ArrayList<int[]> mapping = new ArrayList<>();
	private int[] numConditions;
	private ContrastMapping contrast;
	
	
	
	public AlignedReadsDataMergeMapper(int[] numConditions) {
		this.numConditions = numConditions;
	}
	
	
	public void map(int file, int condition, int name) {
		mapping.add(new int[] {file,condition,name});
	}

	
	@Override
	public IntervalTree<GenomicRegion,AlignedReadsData> map(ReferenceSequence reference,
			GenomicRegion region,PixelLocationMapping pixelMapping,
			MutableTuple data) {
		
		if (contrast==null){
			synchronized (this) {
				if (contrast==null) {
					contrast = new ContrastMapping();
					int[] cumNumConditions = ArrayUtils.cumSum(numConditions, 1);
					for (int[] tr : mapping) {
						contrast.addMapping((tr[0]>0?cumNumConditions[tr[0]-1]:0)+tr[1], tr[2]);
					}
					contrast.build();
				}
			}
		}
		
		Iterator<ImmutableReferenceGenomicRegion<AlignedReadsData>>[] its = new Iterator[data.size()];
		for (int i=0; i<its.length; i++) {
			IntervalTree<GenomicRegion, AlignedReadsData> itree = data.<IntervalTree<GenomicRegion,AlignedReadsData>>get(i);
			if (itree==null || itree.isEmpty()) {
				its[i] = EI.empty();
			} else {
				its[i] = itree.ei();
			}
		}
		
		AlignedReadsDataMerger merger = new AlignedReadsDataMerger(numConditions);
		IntervalTree<GenomicRegion,AlignedReadsData> re = new IntervalTree<>(reference);
		
		Comparator<ImmutableReferenceGenomicRegion<AlignedReadsData>> comp = FunctorUtils.<ImmutableReferenceGenomicRegion<AlignedReadsData>>naturalComparator();
		Class<ImmutableReferenceGenomicRegion<AlignedReadsData>> cls = (Class)ImmutableReferenceGenomicRegion.class;
		for (ImmutableReferenceGenomicRegion<AlignedReadsData>[] a : FunctorUtils.parallellIterator(its, comp, cls).loop()) {
			ImmutableReferenceGenomicRegion<DefaultAlignedReadsData> r = merger.merge(a);
			re.put(r.getRegion(), new ConditionMappedAlignedReadsData(r.getData(), contrast));
		}
			
		
		return re;
	}



}
