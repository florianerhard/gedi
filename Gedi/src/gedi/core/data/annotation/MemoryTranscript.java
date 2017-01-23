package gedi.core.data.annotation;

@Deprecated
/**
 * For compatibility issues!
 * @author erhard
 *
 */
public class MemoryTranscript extends Transcript {

	public MemoryTranscript() {
		super();
	}
	
	public MemoryTranscript(String geneId, String transcriptId,
			int codingStart, int codingEnd) {
		super(geneId,transcriptId,codingStart,codingEnd);
	}
}
