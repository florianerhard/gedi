package gedi.util.io.text.tsv.formats;

import gedi.core.data.annotation.NameAnnotation;
import gedi.core.data.annotation.NarrowPeakAnnotation;
import gedi.core.data.annotation.ScoreNameAnnotation;
import gedi.core.reference.Chromosome;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.util.functions.TriConsumer;
import gedi.util.io.text.HeaderLine;
import gedi.util.io.text.tsv.GenomicTsvFileReader;

public class NarrowPeakFileReader extends GenomicTsvFileReader<NarrowPeakAnnotation> {

	public NarrowPeakFileReader(String path) {
		super(path, false, "\t", new NarrowPeakAnnotationElementParser(), null, NarrowPeakAnnotation.class);
	}

	
	
	public static class NarrowPeakAnnotationElementParser implements TriConsumer<HeaderLine, String[], MutableReferenceGenomicRegion<NarrowPeakAnnotation>> {

		@Override
		public void accept(HeaderLine a, String[] fields,
				MutableReferenceGenomicRegion<NarrowPeakAnnotation> box) {
			
			NarrowPeakAnnotation anno = new NarrowPeakAnnotation(
					fields[3],
					Double.parseDouble(fields[4]),
					Double.parseDouble(fields[6]),
					Double.parseDouble(fields[7]),
					Double.parseDouble(fields[8]),
					Integer.parseInt(fields[9])
					);
			
			
			box.set(Chromosome.obtain(fields[0],fields[5]), new ArrayGenomicRegion(Integer.parseInt(fields[1]),Integer.parseInt(fields[2])), anno);
		}
		
	}
	
}
