package gedi.proteomics.digest;

import java.util.function.Predicate;

import gedi.proteomics.molecules.Polymer;
import gedi.util.FunctorUtils;




public class RemovePrematureStopPeptidesFilter implements Digester,Predicate<String> {

	private Digester digest;
	
	public RemovePrematureStopPeptidesFilter(Digester digest) {
		this.digest = digest;
	}
	
	@Override
	public DigestIterator iteratePeptides(String protein) {
		return new FilteredDigestIterator(this, digest.iteratePeptides(protein));
	}

	@Override
	public boolean test(String seq) {
		return Polymer.isPeptide(seq);
	}
	
	@Override
	/**
	 * For the sake of runtime efficiency: do not count the iterator but use the unfiltered count!
	 */
	public int getPeptideCount(String seq) {
		return (int) FunctorUtils.countIterator(iteratePeptides(seq));
	}


}
