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
package gedi.core.processing.transformer;

import gedi.core.processing.GenomicRegionProcessor;
import gedi.core.processing.ProcessorContext;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegionPosition;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.sequence.SequenceProvider;
import gedi.util.io.text.LineOrientedFile;

public class TransformToPositionProcessor implements GenomicRegionProcessor {

	private GenomicRegionPosition position;
	private int offset;
	

	public TransformToPositionProcessor(GenomicRegionPosition position, int offset) {
		this.position = position;
		this.offset = offset;
	}

	
	public TransformToPositionProcessor(GenomicRegionPosition position) {
		this.position = position;
		this.offset = 0;
	}


	
	
	@Override
	public void beginRegion(MutableReferenceGenomicRegion<?> region, ProcessorContext context)
			throws Exception {
		int pos = position.position(region.getReference(), region.getRegion(), offset);
		region.setRegion(new ArrayGenomicRegion(pos, pos+1));
	}




}
