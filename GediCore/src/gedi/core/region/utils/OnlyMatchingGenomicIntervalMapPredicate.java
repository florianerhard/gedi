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
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;


public class OnlyMatchingGenomicIntervalMapPredicate<C extends Map<GenomicRegion,D>,D> implements Predicate<GenomicRegion>, Map<GenomicRegion,D> {

	private C c;
	private int start;
	private int end;
	private GenomicRegion region;
	
	
	public OnlyMatchingGenomicIntervalMapPredicate(C c, GenomicRegion region) {
		this.c = c;
		this.region = region;
	}
	
	public OnlyMatchingGenomicIntervalMapPredicate(C c, int start, int end) {
		this.c = c;
		this.start = start;
		this.end = end;
	}
	

	public C getParentMap() {
		return c;
	}

	@Override
	public boolean test(GenomicRegion object) {
		return region==null?object.intersects(start, end):object.intersects(region);
//		if (region==null)
//			return object.getNumParts()==1 && object.getStart()>=start && object.getEnd()<=end;
//			
//		return region.containsUnspliced(object);
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
	public boolean containsKey(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public D get(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public D put(GenomicRegion key, D value) {
		if (test(key))
			return c.put(key, value);
		return null;
	}

	@Override
	public D remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends GenomicRegion, ? extends D> m) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<GenomicRegion> keySet() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<D> values() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<java.util.Map.Entry<GenomicRegion, D>> entrySet() {
		throw new UnsupportedOperationException();
	}

}
