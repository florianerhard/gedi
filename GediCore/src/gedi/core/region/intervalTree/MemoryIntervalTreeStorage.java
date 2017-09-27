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

package gedi.core.region.intervalTree;

import gedi.core.data.annotation.ReferenceSequenceLengthProvider;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionStorage;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;
import gedi.util.dynamic.DynamicObject;
import gedi.util.functions.MappedSpliterator;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.Supplier;

public class MemoryIntervalTreeStorage<D> implements GenomicRegionStorage<D>, ReferenceSequenceLengthProvider {

	private HashMap<ReferenceSequence, IntervalTree<GenomicRegion,D>> map = new HashMap<>();
	private DynamicObject meta;
	private Class<D> type;
	
	public MemoryIntervalTreeStorage(Class<D> type) {
		this.type = type;
	}
	

	private IntervalTree<GenomicRegion, D> ensureMap(ReferenceSequence reference) {
		IntervalTree<GenomicRegion, D> re = map.get(reference);
		if (re==null) 
			map.put(reference, re = new IntervalTree<GenomicRegion, D>(reference));
		return re;
	}
	
	@Override
	public MemoryIntervalTreeStorage<D> toMemory() {
		return this;
	}
	
	@Override
	public DynamicObject getMetaData() {
		if (meta==null) meta = DynamicObject.getEmpty();
		return meta;
	}
	
	public void setMetaData(DynamicObject meta) {
		this.meta = meta;
	}
	
	public <O> MemoryIntervalTreeStorage<O> convert(Function<MutableReferenceGenomicRegion<D>,MutableReferenceGenomicRegion<O>> mapper, Class<O> type) {
		MemoryIntervalTreeStorage<O> re = new MemoryIntervalTreeStorage<O>(type);
		iterateMutableReferenceGenomicRegions().forEachRemaining(rgr->{
			MutableReferenceGenomicRegion<O> a = mapper.apply(rgr);
			if (a!=null)
				re.add(a.getReference(), a.getRegion(), a.getData());
		});
		return re;
	}
	
	@Override
	public Class<D> getType() {
		return type;
	}
	
	@Override
	public int getLength(String name) {
		Chromosome reference = Chromosome.obtain(name);
		IntervalTree<GenomicRegion, D> it = map.get(reference);
		if (it==null) {
			// test all three strands
			it = map.get(reference.toStrandIndependent());
			if (it==null)
				it = map.get(reference.toPlusStrand());
			if (it==null)
				it = map.get(reference.toMinusStrand());
			if (it==null)
				return -1;
		}
		return it.isEmpty()?-1:-it.getStop()-1;
	}
	
	/**
	 * If reference not present here, is is added and an empty tree is returned.
	 * @param reference
	 * @return
	 */
	public IntervalTree<GenomicRegion, D> getTree(
			ReferenceSequence reference) {
		return ensureMap(reference);
	}
	
	@Override
	public Set<ReferenceSequence> getReferenceSequences() {
		return map.keySet();
	}

	@Override
	public Spliterator<MutableReferenceGenomicRegion<D>> iterateMutableReferenceGenomicRegions(ReferenceSequence reference) {
		IntervalTree<GenomicRegion,D> tree = map.get(reference);
		if (tree==null) return Spliterators.emptySpliterator();
		
		Supplier<Function<Entry<GenomicRegion,D>, MutableReferenceGenomicRegion<D>>> supp = 
				()->{
					MutableReferenceGenomicRegion<D> mut = new MutableReferenceGenomicRegion<D>();
					return (e)->mut.set(reference, e.getKey(), e.getValue());
				};
		
		return new MappedSpliterator<Entry<GenomicRegion,D>, MutableReferenceGenomicRegion<D>>(tree.entrySet().spliterator(),supp);
	}
	
	@Override
	public Spliterator<MutableReferenceGenomicRegion<D>> iterateIntersectingMutableReferenceGenomicRegions(
			ReferenceSequence reference, GenomicRegion region) {
		IntervalTree<GenomicRegion,D> tree = map.get(reference);
		if (tree==null) return Spliterators.emptySpliterator();
		
		Supplier<Function<Entry<GenomicRegion,D>, MutableReferenceGenomicRegion<D>>> supp = 
				()->{
					MutableReferenceGenomicRegion<D> mut = new MutableReferenceGenomicRegion<D>();
					return (e)->mut.set(reference, e.getKey(), e.getValue());
				};
		
		return new MappedSpliterator<Entry<GenomicRegion,D>, MutableReferenceGenomicRegion<D>>(tree.iterateIntervalsIntersecting(region,r->region.intersects(r)),supp);
	}


	@Override
	public boolean add(ReferenceSequence reference, GenomicRegion region, D data) {
		IntervalTree<GenomicRegion,D> tree = ensureMap(reference);
		boolean present = tree.containsKey(region);
		tree.put(region, data);
		return !present;
	}

	

	@Override
	public boolean remove(ReferenceSequence reference, GenomicRegion region) {
		IntervalTree<GenomicRegion,D> tree = map.get(reference);
		if (tree==null || !tree.containsKey(region)) return false;
		tree.remove(region);
		return true;
	}

	@Override
	public boolean contains(ReferenceSequence reference, GenomicRegion region) {
		IntervalTree<GenomicRegion,D> tree = map.get(reference);
		return tree!=null && tree.containsKey(region);
	}

	@Override
	public D getData(ReferenceSequence reference,GenomicRegion region) {
		IntervalTree<GenomicRegion,D> tree = map.get(reference);
		if (tree==null) return null;
		return tree.get(region);
	}

	
	@Override
	public long size(ReferenceSequence reference) {
		IntervalTree<GenomicRegion,D> tree = map.get(reference);
		if (tree==null) return 0;
		return tree.size();
	}

	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public String toString() {
		return ei().toString(500);
	}
	
	
}
