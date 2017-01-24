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

import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;

import java.util.function.UnaryOperator;

public class TranscriptToCds implements UnaryOperator<MemoryIntervalTreeStorage<Transcript>> {

	@Override
	public MemoryIntervalTreeStorage<Transcript> apply(
			MemoryIntervalTreeStorage<Transcript> storage) {
		return storage.convert(rgr->rgr.getData().isCoding()?rgr.setRegion(rgr.getData().getCds(rgr.getReference(),rgr.getRegion())):null, Transcript.class);
	}

}
