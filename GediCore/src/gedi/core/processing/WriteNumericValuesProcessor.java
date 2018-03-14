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

import java.io.IOException;

import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.sequence.SequenceProvider;
import gedi.util.io.text.LineOrientedFile;

public class WriteNumericValuesProcessor implements GenomicRegionProcessor {

	// TODO: once table framework is there, do not produce an output file but write into a table of the context! (a table output processor will then do the trick)

	private LineOrientedFile out;
	private int condition;
	private String format = "\t%.0f";
	
	public WriteNumericValuesProcessor(LineOrientedFile out, int condition) {
		this.out = out;
		this.condition = condition;
	}


	@Override
	public void begin(ProcessorContext context) throws Exception {
		out.startWriting();
	}

	
	@Override
	public void beginRegion(MutableReferenceGenomicRegion<?> region,
			ProcessorContext context) throws Exception {
		
		out.writef("%s:%s",region.getReference(),region.getRegion());
		
	}
	
	@Override
	public void value(MutableReferenceGenomicRegion<?> region,
			int position, double[] values, ProcessorContext context) throws IOException {
		out.writef(format,values[condition]);
	}
	
	@Override
	public void endRegion(MutableReferenceGenomicRegion<?> region,
			ProcessorContext context) throws Exception {
		out.writef("\n");
		
	}

	@Override
	public void end(ProcessorContext context) throws Exception {
		out.finishWriting();
	}


	




}
