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

package gedi.ensembl;

import gedi.core.data.annotation.Transcript;
import gedi.core.data.index.MemoryArrayDataIndex;
import gedi.util.genomic.TranscriptToGenomeMapper;

import java.io.IOException;

public class BiomartTranscriptToGenomeMapper extends TranscriptToGenomeMapper<Transcript[]> {

	public BiomartTranscriptToGenomeMapper(String exonsFilePath, boolean onlyStandardChromosomes) throws IOException {
		this.transcripts = new MemoryArrayDataIndex<String,Transcript>(
				new BiomartExonFileReader(exonsFilePath,onlyStandardChromosomes).readIntoMemoryArrayCombiner(Transcript.class),
				m->m.getTranscriptId());
	}

	
}
