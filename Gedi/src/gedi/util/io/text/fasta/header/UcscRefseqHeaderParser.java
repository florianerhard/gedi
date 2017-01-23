package gedi.util.io.text.fasta.header;

import gedi.util.io.text.fasta.DefaultFastaHeaderParser;

import java.util.regex.Pattern;


public class UcscRefseqHeaderParser extends DefaultFastaHeaderParser {

	public UcscRefseqHeaderParser() {
		super(Pattern.compile(".*?_refGene_(.+?) "),new String[] {"RefSeq"});
	}

}
