/**
 * 
 *    Copyright 2017 Florian Erhard
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */
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
