package gedi.util.io.text.tsv.formats;

import gedi.core.data.annotation.ScoreNameAnnotation;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.util.functions.TriConsumer;
import gedi.util.io.text.HeaderLine;
import gedi.util.io.text.tsv.GenomicTsvFileReader;

public class ScoreNameBedFileReader extends GenomicTsvFileReader<ScoreNameAnnotation> {

	public ScoreNameBedFileReader(String path) {
		super(path, false, "\t", new ScoreNameBedElementParser(), null,ScoreNameAnnotation.class);
	}

	
	
	public static class ScoreNameBedElementParser implements TriConsumer<HeaderLine, String[], MutableReferenceGenomicRegion<ScoreNameAnnotation>> {

		@Override
		public void accept(HeaderLine a, String[] fields,
				MutableReferenceGenomicRegion<ScoreNameAnnotation> box) {
			
			BedEntry bed = BedEntry.parseValues(fields);
			
			ScoreNameAnnotation e = new ScoreNameAnnotation(bed.getName(),bed.getDoubleScore());
			box.set(bed.getReferenceSequence(), bed.getGenomicRegion(), e);
		}
		
	}
	
}
