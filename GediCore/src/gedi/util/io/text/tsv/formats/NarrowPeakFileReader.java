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
