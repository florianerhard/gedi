package gedi.util.io.text.fasta.header;

import gedi.util.io.text.fasta.DefaultFastaHeaderParser;

import java.util.regex.Pattern;


public class ToFirstSpaceHeaderParser extends DefaultFastaHeaderParser {

	
	public ToFirstSpaceHeaderParser() {
		super(
			Pattern.compile("^>(.*?)\\s+(.*?)$"), 
			new String[] {
				"id","description"
			}
		);
	}

	
}
