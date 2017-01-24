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

package gedi.core.data.reads;

import java.util.function.BinaryOperator;

public class AlignedReadsDataSumCountOperator implements BinaryOperator<AlignedReadsData> {

	private AlignedReadsDataFactory fac;
	
	@Override
	public AlignedReadsData apply(AlignedReadsData t, AlignedReadsData u) {
		if (fac==null) fac = new AlignedReadsDataFactory(t.getNumConditions());
		if (fac.getNumConditions()!=t.getNumConditions() || fac.getNumConditions()!=u.getNumConditions())
			throw new RuntimeException("Number of conditions inconsistent!");
		
		fac.start().newDistinctSequence();
		for (int c=0; c<t.getNumConditions(); c++) {
			for (int d=0;d<t.getDistinctSequences(); d++)
				fac.incrementCount(c, t.getCount(d, c));
			for (int d=0;d<u.getDistinctSequences(); d++)
				fac.incrementCount(c, u.getCount(d, c));
		}
		return fac.create();
	}
		
}
