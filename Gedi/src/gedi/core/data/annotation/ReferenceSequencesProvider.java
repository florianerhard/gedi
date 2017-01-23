package gedi.core.data.annotation;

import gedi.core.reference.ReferenceSequence;
import gedi.util.functions.ExtendedIterator;

public interface ReferenceSequencesProvider {

	
	ExtendedIterator<ReferenceSequence> iterateReferenceSequences();
	
}
