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

package gedi.virology.hsv1annotation.kinetics.provider;

import gedi.core.data.annotation.NameAnnotation;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.functions.ExtendedIterator;
import gedi.util.mutable.MutableInteger;

public class TssProvider implements KineticRegionProvider {

	private MemoryIntervalTreeStorage<NameAnnotation> mrna;
	
	private int tssRegion = 5;

	public TssProvider(MemoryIntervalTreeStorage<NameAnnotation> mrna) {
		this.mrna = mrna;
	}
	
	@Override
	public String getType() {
		return "TSS";
	}
	

	@Override
	public ExtendedIterator<ImmutableReferenceGenomicRegion<NameAnnotation>> ei(
			ReferenceGenomicRegion<?> region) {
		
		MutableInteger n = new MutableInteger(1);
		return mrna.ei(region)
			.map(mr->new ImmutableReferenceGenomicRegion<Void>(mr.getReference(), mr.map(new ArrayGenomicRegion(0,tssRegion))))
			.unique(true)
			.map(r->new ImmutableReferenceGenomicRegion<NameAnnotation>(r.getReference(), r.getRegion(), new NameAnnotation("TSS "+(n.N++))));
	}
	
	
	
}
