package gedi.virology.hsv1annotation.kinetics.provider;

import gedi.core.data.annotation.NameAnnotation;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.datastructure.tree.redblacktree.IntervalTreeSet;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;

import java.util.ArrayList;

public class ExonBinProvider implements KineticRegionProvider {

	private String type;
	private MemoryIntervalTreeStorage<NameAnnotation> storage;
	
	
	public ExonBinProvider(String type, MemoryIntervalTreeStorage<NameAnnotation> storage) {
		this.type = type;
		this.storage = storage;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public ExtendedIterator<ImmutableReferenceGenomicRegion<NameAnnotation>> ei(
			ReferenceGenomicRegion<?> region) {
		
		IntervalTreeSet<GenomicRegion> it = storage.ei(region).filter(r->region.contains(r)).map(r->r.getRegion()).toCollection(new IntervalTreeSet<GenomicRegion>(null));
		IntArrayList cuts = new IntArrayList();
		for (GenomicRegion r : it) 
			cuts.addAll(r.getBoundaries());
		cuts.sort();
		cuts.unique();
		
		return new ExtendedIterator<ImmutableReferenceGenomicRegion<NameAnnotation>>() {

			int index = 0;
			int n = 1;
			
			@Override
			public boolean hasNext() {
				return index<cuts.size();
			}

			@Override
			public ImmutableReferenceGenomicRegion<NameAnnotation> next() {
				ImmutableReferenceGenomicRegion<NameAnnotation> re = new ImmutableReferenceGenomicRegion<NameAnnotation>(
						region.getReference(), 
						new ArrayGenomicRegion(cuts.getInt(index),cuts.getInt(++index)), 
						new NameAnnotation("Exonbin "+n++));
				if (index<cuts.size() && EI.wrap(it.getIntervalsSpanning(cuts.getInt(index), new ArrayList<>())).filter(r->r.contains(cuts.getInt(index))).count()==0)
					index++;
				return re;
			}
			
		};
	}

}
