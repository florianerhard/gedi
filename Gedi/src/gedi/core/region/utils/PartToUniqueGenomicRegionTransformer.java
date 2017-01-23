package gedi.core.region.utils;

import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.GenomicRegionPart;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

public class PartToUniqueGenomicRegionTransformer implements Collection<GenomicRegionPart> {

	private HashSet<GenomicRegion> observed = new HashSet<GenomicRegion>();
	private Collection<GenomicRegion> c;
	
	public PartToUniqueGenomicRegionTransformer(Collection<GenomicRegion> c) {
		this.c = c;
	}

	@Override
	public boolean add(GenomicRegionPart e) {
		GenomicRegion g = e.getGenomicRegion();
		if (observed.add(g))
			return c.add(g);
		return false;
	}
	
	public HashSet<GenomicRegion> getObserved() {
		return observed;
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
	public Iterator<GenomicRegionPart> iterator() {
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
	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addAll(Collection<? extends GenomicRegionPart> c) {
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
