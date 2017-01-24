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

package gedi.core.region.utils;

import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * A group consists of overlapping intervals. GenomicRegions are treated as intervals here (i.e. even if one is in the intron of another, it belongs to the same group!)
 * Input spliterator must be sorted!
 * @author erhard
 *
 * @param <D>
 * @param <C>
 */
public class GenomicRegionGroupIterator<D,C extends Collection<O>,O> implements Spliterator<C>{

	private Spliterator<? extends ReferenceGenomicRegion<D>> it;
	private Advance advance = new Advance();
	private Supplier<C> creator;
	private Function<ReferenceGenomicRegion<D>,O> mapper;
	
	public GenomicRegionGroupIterator(Supplier<C> creator, Function<ReferenceGenomicRegion<D>,O> mapper,
			Spliterator<? extends ReferenceGenomicRegion<D>> it) {
		this.creator = creator;
		this.mapper = mapper;
		this.it = it;
	}

	@Override
	public boolean tryAdvance(Consumer<? super C> action) {
		if (advance==null) return false;
		
		// first call
		if (advance.t==null) it.tryAdvance(advance);
		
		// read next group
		ReferenceSequence ref = advance.t.getReference();
		GenomicRegion reg = advance.t.getRegion();
		C re = creator.get();
		re.add(mapper.apply(advance.t));
		
		while (it.tryAdvance(advance)) {
			if (advance.isSameGroup(ref,reg)) {
				reg = reg.union(advance.t.getRegion());
				re.add(mapper.apply(advance.t));
			} else {
				action.accept(re);
				return true;
			}
		}
		
		advance = null;
		action.accept(re);
		return true;
		
	}

	@Override
	public Spliterator<C> trySplit() {
		return null;
	}

	@Override
	public long estimateSize() {
		return it.estimateSize();
	}

	@Override
	public int characteristics() {
		return IMMUTABLE|DISTINCT|ORDERED;
	}

	
	private class Advance implements Consumer<ReferenceGenomicRegion<D>> {

		
		private ReferenceSequence current = null;
		private HashSet<ReferenceSequence> all = new HashSet<ReferenceSequence>();
		
		ReferenceGenomicRegion<D> t;
		
		@Override
		public void accept(ReferenceGenomicRegion<D> t) {
			this.t = t;
		}

		public boolean isSameGroup(ReferenceSequence reference, GenomicRegion reg) {
			
			// check sorting
			if (!all.add(t.getReference()) && current.equals(t.getReference())) {
				if (t.getRegion().compareTo(reg)<0)
					throw new RuntimeException("Not sorted!");
			}
			current = reference;
			
			return t.getReference().equals(reference) && t.getRegion().getStart()<reg.getEnd();
		}
		
	}
	
}
