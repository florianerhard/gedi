package gedi.util.io.text.fasta.header;

import gedi.util.io.text.fasta.DefaultFastaHeaderParser;

import java.util.regex.Pattern;


public class MirBaseHeaderParser extends DefaultFastaHeaderParser {

	private boolean cutOrganism;
	
	public MirBaseHeaderParser() {
		this(false);
	}
	
	public MirBaseHeaderParser(boolean cutOrganism) {
		super(
			Pattern.compile("^>(.*?)\\s+(MI(?:MAT|)\\d+)\\s+(.*)\\s.*?$"), 
			new String[] {
				"mirBase id","accession","organism"
			}
		);
		this.cutOrganism = cutOrganism;
	}

	@Override
	public String getId(String header) {
		String id = super.getId(header);
		if (cutOrganism)
			id = id.substring(id.indexOf('-')+1);
		return id;
	}
	
}
