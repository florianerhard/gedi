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

package gedi.core.region;

import gedi.core.reference.ReferenceSequence;
import gedi.util.GeneralUtils;

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.Spliterators;

public class GenomicRegionStorageCollection<D> extends AbstractCollection<ImmutableReferenceGenomicRegion<D>> {
	
	private GenomicRegionStorage<D> storage;
	

	public GenomicRegionStorageCollection(GenomicRegionStorage<D> storage) {
		this.storage = storage;
	}

	@Override
	public int size() {
		long re = 0;
		for (ReferenceSequence reference : storage.getReferenceSequences())
			re+=storage.size(reference);
		return GeneralUtils.checkedLongToInt(re);
	}

	@Override
	public boolean isEmpty() {
		for (ReferenceSequence reference : storage.getReferenceSequences())
			if (storage.size(reference)>0) return false;
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean contains(Object o) {
		if (o instanceof ImmutableReferenceGenomicRegion) {
			ImmutableReferenceGenomicRegion<D> r = (ImmutableReferenceGenomicRegion<D>) o;
			return storage.contains(r.getReference(), r.getRegion());
		}
		if (o instanceof MutableReferenceGenomicRegion) {
			MutableReferenceGenomicRegion<D> r = (MutableReferenceGenomicRegion<D>) o;
			return storage.contains(r.getReference(), r.getRegion());
		}
		return false;
	}

	@Override
	public Iterator<ImmutableReferenceGenomicRegion<D>> iterator() {
		return Spliterators.iterator(storage.iterateReferenceGenomicRegions());
	}


	@Override
	public boolean add(ImmutableReferenceGenomicRegion<D> e) {
		return storage.add(e.getReference(), e.getRegion(), e.getData());
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean remove(Object o) {
		if (o instanceof ImmutableReferenceGenomicRegion) {
			ImmutableReferenceGenomicRegion<D> e = (ImmutableReferenceGenomicRegion<D>) o;
			return storage.remove(e.getReference(), e.getRegion());
		}
		if (o instanceof MutableReferenceGenomicRegion) {
			MutableReferenceGenomicRegion<D> r = (MutableReferenceGenomicRegion<D>) o;
			return storage.remove(r.getReference(), r.getRegion());
		}
		return false;
	}

	@Override
	public void clear() {
		storage.clear();
	}

}
