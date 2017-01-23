package gedi.util.io.text.tsv.formats;

import gedi.core.data.annotation.NameAnnotation;
import gedi.core.data.annotation.ScoreNameAnnotation;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.functions.TriConsumer;
import gedi.util.io.text.HeaderLine;
import gedi.util.io.text.tsv.GenomicTsvFileReader;

public class LocationFileReader extends GenomicTsvFileReader<NameAnnotation> {

	public LocationFileReader(String path) {
		super(path, false, "\t", new ScoreNameBedElementParser(), null, NameAnnotation.class);
	}

	
	
	public static class ScoreNameBedElementParser implements TriConsumer<HeaderLine, String[], MutableReferenceGenomicRegion<NameAnnotation>> {

		@Override
		public void accept(HeaderLine a, String[] fields,
				MutableReferenceGenomicRegion<NameAnnotation> box) {
			
			MutableReferenceGenomicRegion<Object> rgr = new MutableReferenceGenomicRegion<Object>().parse(fields[0]);
			if (rgr==null) throw new RuntimeException("Cannot parse "+fields[0]);
			box.setReference(rgr.getReference());
			box.setRegion(rgr.getRegion());
			
			box.setData(new NameAnnotation(fields.length>1?fields[1]:""));
		}
		
	}
	
}
