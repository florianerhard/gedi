package gedi.core.data.annotation;

import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.util.functions.ExtendedIterator;

import java.util.function.UnaryOperator;

public class MappedReferenceSequencesProvider implements ReferenceSequencesProvider {

	private ReferenceSequencesProvider parent;
	private UnaryOperator<String> mapper;

	
	public MappedReferenceSequencesProvider(ReferenceSequencesProvider parent,
			UnaryOperator<String> mapper) {
		this.parent = parent;
		this.mapper = mapper;
	}



	@Override
	public ExtendedIterator<ReferenceSequence> iterateReferenceSequences() {
		return parent.iterateReferenceSequences().map(r->(ReferenceSequence)Chromosome.obtain(mapper.apply(r.getName()),r.getStrand())).unique(false);
	}
	
	
}
