package gedi.util.io.text.fasta.header;

import gedi.util.io.text.fasta.FastaHeaderParser;

import java.util.Map;


public class ChainFastaHeaderParser implements FastaHeaderParser{

	private FastaHeaderParser[] chain;
	
	public ChainFastaHeaderParser(FastaHeaderParser... chain) {
		this.chain = chain;
	}

	@Override
	public boolean canParse(String header) {
		for (FastaHeaderParser p : chain)
			if (p.canParse(header))
				return true;
		return false;
	}

	@Override
	public String getId(String header) {
		for (FastaHeaderParser p : chain)
			if (p.canParse(header))
				return p.getId(header);
		return null;
	}

	@Override
	public boolean isReUseMap() {
		return chain[0].isReUseMap();
	}

	@Override
	public Map<String, String> parseHeader(String header) {
		for (FastaHeaderParser p : chain)
			if (p.canParse(header))
				return p.parseHeader(header);
		return null;
	}

	@Override
	public void setReUseMap(boolean reUseMap) {
		for (FastaHeaderParser p : chain)
			p.setReUseMap(reUseMap);
	}
	
	
	
}
