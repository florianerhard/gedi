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
