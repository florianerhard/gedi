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

import java.io.IOException;
import java.util.Iterator;
import java.util.function.Function;

import gedi.core.data.annotation.Gff3Element;
import gedi.core.data.annotation.ScoreNameAnnotation;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.util.functions.TriConsumer;
import gedi.util.io.text.HeaderLine;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.tsv.GenomicTsvFileReader;

public class GffFileReader<T> extends GenomicTsvFileReader<T> {

	public GffFileReader(String path, Function<Gff3Element,T> mapper, Class<T> type) {
		super(path, false, "\t", new MappedParser<T>(mapper), null,type);
	}

	protected Iterator<String> createIterator() throws IOException {
		return new LineOrientedFile(path).lineIterator("#");
	}
	
	private static class MappedParser<T> implements TriConsumer<HeaderLine, String[], MutableReferenceGenomicRegion<T>> {

		private Gff3Element.Gff3ElementParser sub = new Gff3Element.Gff3ElementParser();
		private Function<Gff3Element, T> mapper;
		private MutableReferenceGenomicRegion<Gff3Element> box = new MutableReferenceGenomicRegion<Gff3Element>();
		
		public MappedParser(Function<Gff3Element, T> mapper) {
			this.mapper = mapper;
		}

		@Override
		public void accept(HeaderLine a, String[] b,
				MutableReferenceGenomicRegion<T> c) {
			sub.accept(a, b, box );
			c.set(box.getReference(), box.getRegion(), mapper.apply(box.getData()));
		}
		
	}
}
