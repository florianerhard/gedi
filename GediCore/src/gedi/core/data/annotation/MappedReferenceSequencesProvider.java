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
