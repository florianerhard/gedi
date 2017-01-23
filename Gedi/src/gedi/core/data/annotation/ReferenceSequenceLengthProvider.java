package gedi.core.data.annotation;

import gedi.core.reference.ReferenceSequence;

public interface ReferenceSequenceLengthProvider {

	/**
	 * Negative numbers: length is lower bound of true length
	 * @param reference
	 * @return
	 */
	int getLength(String name);
	
}
