package gedi.util.io.text.fasta;

import java.util.Map;


public interface FastaHeaderParser {

	public abstract boolean isReUseMap();
	public abstract void setReUseMap(boolean reUseMap);
	
	public abstract Map<String,String> parseHeader(String header);
	public abstract boolean canParse(String header);
	public abstract String getId(String header);
	
}
