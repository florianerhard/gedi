package gedi.util.io.text.fasta;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractFastaHeaderParser implements FastaHeaderParser {

	private boolean reUseMap = true;
	private Map<String,String> map;
	private int numFields;
	
	public AbstractFastaHeaderParser(int numFields) {
		this.numFields = numFields;
	}

	public boolean isReUseMap() {
		return reUseMap;
	}

	public void setReUseMap(boolean reUseMap) {
		this.reUseMap = reUseMap;
	}

	@Override
	public boolean canParse(String header) {
		return numFields<0 || parseHeader(header).size()==numFields;
	}
	
	protected Map<String, String> getMap() {
		Map<String,String> re = 
			 reUseMap && map!=null ? map : new HashMap<String, String>();
		re.clear();
		return re;
	}
	
	
}
