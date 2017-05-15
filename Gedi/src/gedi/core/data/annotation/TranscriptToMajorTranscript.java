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

import java.util.Comparator;
import java.util.function.UnaryOperator;

import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;

public class TranscriptToMajorTranscript implements UnaryOperator<MemoryIntervalTreeStorage<Transcript>> {

	


	@Override
	public MemoryIntervalTreeStorage<Transcript> apply(
			MemoryIntervalTreeStorage<Transcript> storage) {
		
		Comparator<ImmutableReferenceGenomicRegion<Transcript>> cmp = (a,b)->a.getData().getGeneId().compareTo(b.getData().getGeneId());
		cmp = cmp.thenComparing((a,b)->Boolean.compare(b.getData().isCoding(), a.getData().isCoding()));
		cmp = cmp.thenComparing((a,b)->Integer.compare(b.getRegion().getTotalLength(), a.getRegion().getTotalLength()));
		
		return storage.ei().sort(cmp).multiplex(cmp, ImmutableReferenceGenomicRegion.class).map(a->a[0]).add(new MemoryIntervalTreeStorage<>(Transcript.class));
	}


}
