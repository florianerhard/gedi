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
package gedi.core.processing;

import gedi.core.reference.ReferenceSequence;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.sequence.SequenceProvider;
import gedi.util.io.text.LineOrientedFile;

public class SequenceExtractingProcessor implements GenomicRegionProcessor {

	// TODO: once table framework is there, do not produce an output file but write into a table of the context! (a table output processor will then do the trick)

	private SequenceProvider sequence;
	private LineOrientedFile out;
	private boolean strandspecific;
	
	
	public SequenceExtractingProcessor(LineOrientedFile out, SequenceProvider sequence, boolean strandspecific) {
		this.sequence = sequence;
		this.out = out;
		this.strandspecific = strandspecific;
	}


	@Override
	public void begin(ProcessorContext context) throws Exception {
		out.startWriting();
	}
	
	
	@Override
	public void beginRegion(MutableReferenceGenomicRegion<?> region, ProcessorContext context)
			throws Exception {
		CharSequence seq = strandspecific?sequence.getSequence(region.getReference(), region.getRegion()):sequence.getPlusSequence(region.getReference().getName(), region.getRegion());
		ReferenceSequence ref = region.getReference();
		if (!strandspecific)
			ref = ref.toStrandIndependent();
		out.writef(">%s\n%s\n", ref+":"+region.getRegion(),seq);
	}

	@Override
	public void end(ProcessorContext context) throws Exception {
		out.finishWriting();
	}




}
