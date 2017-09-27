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

package gedi.core.data.numeric;

import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;

public class EmptyGenomicNumericProvider implements GenomicNumericProvider {

	@Override
	public int getNumDataRows() {
		return 0;
	}

	@Override
	public double getValue(ReferenceSequence reference, int pos, int row) {
		return 0;
	}
	
	@Override
	public int getLength(String name) {
		return -1;
	}

	@Override
	public PositionNumericIterator iterateValues(ReferenceSequence reference,
			GenomicRegion region) {
		return new PositionNumericIterator() {
			
			@Override
			public boolean hasNext() {
				return false;
			}
			
			@Override
			public int nextInt() {
				throw new RuntimeException();
			}
			
			@Override
			public double getValue(int row) {
				return 0;
			}
			
			@Override
			public double[] getValues(double[] re) {
				return re;
			}
		};
	}

}
