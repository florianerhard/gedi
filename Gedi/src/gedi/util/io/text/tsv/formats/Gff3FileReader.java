package gedi.util.io.text.tsv.formats;

import java.io.IOException;
import java.util.Iterator;

import gedi.core.data.annotation.Gff3Element;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.tsv.GenomicTsvFileReader;

public class Gff3FileReader extends GenomicTsvFileReader<Gff3Element> {

	public Gff3FileReader(String path) {
		super(path, false, "\t", new Gff3Element.Gff3ElementParser(), null, Gff3Element.class);
	}
	
	public Gff3FileReader(String path, String...features) {
		super(path, false, "\t", new Gff3Element.Gff3ElementParser(features), null, Gff3Element.class);
	}

	protected Iterator<String> createIterator() throws IOException {
		return new LineOrientedFile(path).lineIterator("#");
	}
	
	
}
