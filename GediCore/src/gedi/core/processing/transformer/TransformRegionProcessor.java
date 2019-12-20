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


/**
 * Keeps introns!
 * @author erhard
 *
 */
public class TransformRegionProcessor implements GenomicRegionProcessor {

	private GenomicRegionPosition startPosition;
	private int startOffset;
	
	private GenomicRegionPosition endPosition;
	private int endOffset;

	public TransformRegionProcessor(GenomicRegionPosition startPosition,
			int startOffset, GenomicRegionPosition endPosition, int endOffset) {
		this.startPosition = startPosition;
		this.startOffset = startOffset;
		this.endPosition = endPosition;
		this.endOffset = endOffset;
	}
	
	public TransformRegionProcessor(GenomicRegionPosition startPosition,
			GenomicRegionPosition endPosition) {
		this.startPosition = startPosition;
		this.endPosition = endPosition;
	}


	
	@Override
	public void beginRegion(MutableReferenceGenomicRegion<?> region, ProcessorContext context)
			throws Exception {
		int start = startPosition.position(region.getReference(), region.getRegion(), startOffset);
		int end = endPosition.position(region.getReference(), region.getRegion(), endOffset);
		
		if (start>end) {
			int t = start;
			start = end;
			end = t;
		}
		
		ArrayGenomicRegion re = new ArrayGenomicRegion(start,end);
		re = re.intersect(region.getRegion()).union(re.subtract(region.getRegion().removeIntrons()));
		
		region.setRegion(re);
	}




}
