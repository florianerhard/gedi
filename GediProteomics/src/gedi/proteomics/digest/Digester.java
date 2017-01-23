package gedi.proteomics.digest;


public interface Digester {

	DigestIterator iteratePeptides(String protein);

	int getPeptideCount(String protein);
}
