package gedi.core.data.annotation;

import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.mutable.MutablePair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class TranscriptToGene implements UnaryOperator<MemoryIntervalTreeStorage<Transcript>> {

	
	private boolean removeIntrons = false;
	
	public TranscriptToGene() {
	}
	
	
	public TranscriptToGene(boolean removeIntrons) {
		this.removeIntrons = removeIntrons;
	}

	public boolean isRemoveIntrons() {
		return removeIntrons;
	}

	public void setRemoveIntrons(boolean removeIntrons) {
		this.removeIntrons = removeIntrons;
	}

	
	public <T> MemoryIntervalTreeStorage<T> convert(MemoryIntervalTreeStorage<Transcript> storage, Function<Transcript,T> mapper, Class<T> type) {
		MemoryIntervalTreeStorage<T> re = new MemoryIntervalTreeStorage<T>(type);
		
		for (ReferenceSequence ref : storage.getReferenceSequences()) {
			HashMap<String,ArrayList<ImmutableReferenceGenomicRegion<Transcript>>> genes = new HashMap<String, ArrayList<ImmutableReferenceGenomicRegion<Transcript>>>();
			storage.iterateMutableReferenceGenomicRegions(ref).forEachRemaining( 
					rgr ->genes.computeIfAbsent(rgr.getData().getGeneId(), g->new ArrayList<>()).add(rgr.toImmutable()) 
					);
			
			for (String g : genes.keySet()){ 
				ArrayGenomicRegion reg = new ArrayGenomicRegion();
				int start = -1;
				int stop = -1;
				for (ImmutableReferenceGenomicRegion<Transcript> t : genes.get(g)) {
					reg = reg.union(t.getRegion());
					if (t.getData().isCoding()) {
						if (start==-1) {
							start = t.getData().getCodingStart();
							stop = t.getData().getCodingEnd();
						}
						else {
							start = Math.min(start, t.getData().getCodingStart());
							stop = Math.min(stop, t.getData().getCodingEnd());
						}
					}
				}
				if (removeIntrons) 
					reg = reg.removeIntrons();
				re.add(new ImmutableReferenceGenomicRegion<T>(ref, reg, mapper.apply(new Transcript(g, g, start, stop))));
			}
		}
		
		return re;
	}


	@Override
	public MemoryIntervalTreeStorage<Transcript> apply(
			MemoryIntervalTreeStorage<Transcript> storage) {
		return convert(storage,t->t, Transcript.class);
	}

	
	public MemoryIntervalTreeStorage<String> toGenes(
			MemoryIntervalTreeStorage<Transcript> storage) {
		return convert(storage,t->t.getGeneId(), String.class);
	}

}
