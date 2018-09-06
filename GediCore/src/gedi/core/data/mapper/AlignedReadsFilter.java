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
import gedi.core.data.reads.SelectDistinctSequenceAlignedReadsData;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;

@GenomicRegionDataMapping(fromType=IntervalTree.class,toType=IntervalTree.class)
public class AlignedReadsFilter extends StorageFilter<AlignedReadsData>{

	

	public void unique() {
		addFilter(r->r.getData().isAnyUniqueMapping());
		addOperator(r->{
			if (!r.getData().isAnyAmbigousMapping()) return r;
			IntArrayList ds = new IntArrayList(r.getData().getDistinctSequences());
			for (int d=0; d<r.getData().getDistinctSequences(); d++)
				if (r.getData().isUniqueMapping(d))
					ds.add(d);
			ReferenceGenomicRegion<AlignedReadsData> re = new ImmutableReferenceGenomicRegion<>(r.getReference(),r.getRegion(),new SelectDistinctSequenceAlignedReadsData(r.getData(), ds.toIntArray()));
			return re;
		});
	}
	

}
