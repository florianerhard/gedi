package gedi.proteomics.digest;

import gedi.util.functions.ExtendedIterator;

public interface DigestIterator extends ExtendedIterator<String> {

	int getStartPosition();
	int getEndPosition();
	int getMissed();
}
