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
