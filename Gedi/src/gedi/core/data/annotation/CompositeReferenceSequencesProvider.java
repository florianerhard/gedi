package gedi.core.data.annotation;

import gedi.core.reference.ReferenceSequence;
import gedi.util.functions.EI;
import gedi.util.functions.ExtendedIterator;

import java.util.ArrayList;

public class CompositeReferenceSequencesProvider extends ArrayList<ReferenceSequencesProvider> implements ReferenceSequencesProvider {

	@Override
	public ExtendedIterator<ReferenceSequence> iterateReferenceSequences() {
		return EI.wrap(iterator()).demultiplex(r->r.iterateReferenceSequences()).unique(false);
	}
	
}
