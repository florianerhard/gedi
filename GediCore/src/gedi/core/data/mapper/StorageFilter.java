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

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;

@GenomicRegionDataMapping(fromType=IntervalTree.class,toType=IntervalTree.class)
public class StorageFilter<D> implements GenomicRegionDataMapper<IntervalTree<GenomicRegion,D>,IntervalTree<GenomicRegion,D>>{

	private Predicate<ReferenceGenomicRegion<D>> filter = d->true;
	private UnaryOperator<ReferenceGenomicRegion<D>> operator = d->d;
	
	public void setFilter(Predicate<ReferenceGenomicRegion<D>> filter) {
		this.filter = filter;
	}
	
	public void setOperator(UnaryOperator<ReferenceGenomicRegion<D>> operator) {
		this.operator = operator;
	}
	
	@Override
	public IntervalTree<GenomicRegion, D> map(ReferenceSequence reference,
			GenomicRegion region, PixelLocationMapping pixelMapping,
			IntervalTree<GenomicRegion, D> data) {
		
		IntervalTree<GenomicRegion, D> re = new IntervalTree<GenomicRegion, D>(data.getReference());
		MutableReferenceGenomicRegion<D> rgr = new MutableReferenceGenomicRegion<D>();
		
		Iterator<Entry<GenomicRegion, D>> it = data.entrySet().iterator();
		while (it.hasNext()) {
			Entry<GenomicRegion, D> n = it.next();
			if (filter.test(rgr.set(reference, n.getKey(), n.getValue()))) {
				
				ReferenceGenomicRegion<D> a = operator.apply(rgr);
				if (a!=null)
					re.put(a.getRegion(),a.getData());
			}
		}
		
		return re;
		
		
	}
	
	

}
