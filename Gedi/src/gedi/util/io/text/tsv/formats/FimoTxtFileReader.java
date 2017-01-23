package gedi.util.io.text.tsv.formats;

import java.util.Arrays;
import java.util.Iterator;

import gedi.core.data.annotation.ScoreNameAnnotation;
import gedi.core.reference.Chromosome;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.util.functions.TriConsumer;
import gedi.util.io.text.HeaderLine;
import gedi.util.io.text.tsv.GenomicTsvFileReader;

public class FimoTxtFileReader extends GenomicTsvFileReader<ScoreNameAnnotation> {

	public FimoTxtFileReader(String path) {
		super(path, true, "\t", new FimoTxtParser(), null, ScoreNameAnnotation.class);
	}

	/**
	 * Header must already be skipped!
	 * @param it
	 */
	public FimoTxtFileReader(Iterator<String> it) {
		super(it, false, "\t", new FimoTxtParser(), null,ScoreNameAnnotation.class);
	}
	
	
	public static class FimoTxtParser implements TriConsumer<HeaderLine, String[], MutableReferenceGenomicRegion<ScoreNameAnnotation>> {

		@Override
		public void accept(HeaderLine a, String[] fields,
				MutableReferenceGenomicRegion<ScoreNameAnnotation> box) {
			
			ScoreNameAnnotation anno = new ScoreNameAnnotation(
					fields[0],
					Double.parseDouble(fields[4])
					);
			
			int start = Integer.parseInt(fields[2]);
			int stop = Integer.parseInt(fields[3]);
			if (start>stop) {
				int t = start;
				start = stop;
				stop = t;
			}
			box.set(Chromosome.obtain(fields[1]), new ArrayGenomicRegion(start,stop+1), anno);
		}
		
	}
	
}
