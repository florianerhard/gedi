package gedi.util.io.text.tsv.formats;

import gedi.core.data.annotation.NameAnnotation;
import gedi.core.data.annotation.ScoreNameAnnotation;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.util.functions.TriConsumer;
import gedi.util.io.text.HeaderLine;
import gedi.util.io.text.tsv.GenomicTsvFileReader;

public class NameBedFileReader extends GenomicTsvFileReader<NameAnnotation> {

	public NameBedFileReader(String path) {
		super(path, false, "\t", new NameBedElementParser(), null, NameAnnotation.class);
	}

	
	
	public static class NameBedElementParser implements TriConsumer<HeaderLine, String[], MutableReferenceGenomicRegion<NameAnnotation>> {

		@Override
		public void accept(HeaderLine a, String[] fields,
				MutableReferenceGenomicRegion<NameAnnotation> box) {
			
			BedEntry bed = BedEntry.parseValues(fields);
			
			NameAnnotation e = new NameAnnotation(bed.getName());
			box.set(bed.getReferenceSequence(), bed.getGenomicRegion(), e);
		}
		
	}
	
}
