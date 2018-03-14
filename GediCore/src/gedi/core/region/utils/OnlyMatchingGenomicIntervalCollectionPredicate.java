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

import gedi.core.region.GenomicRegion;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Predicate;


public class OnlyMatchingGenomicIntervalCollectionPredicate<C extends Collection<GenomicRegion>> implements Predicate<GenomicRegion>, Collection<GenomicRegion> {

	private C c;
	private int start;
	private int end;
	private GenomicRegion region;
	
	
	public OnlyMatchingGenomicIntervalCollectionPredicate(C c, GenomicRegion region) {
		this.c = c;
		this.region = region;
	}
	
	public OnlyMatchingGenomicIntervalCollectionPredicate(C c, int start, int end) {
		this.c = c;
		this.start = start;
		this.end = end;
	}
	

	public C getParentCollection() {
		return c;
	}

	@Override
	public boolean test(GenomicRegion object) {
		return region==null?object.intersects(start, end):object.intersects(region);
	}

	@Override
	public int size() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEmpty() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean contains(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Iterator<GenomicRegion> iterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object[] toArray() {
		throw new UnsupportedOperationException();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean add(GenomicRegion e) {
		if (test(e))
			return c.add(e);
		return false;
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends GenomicRegion> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

}
