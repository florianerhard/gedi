package gedi.core.processing;

import java.util.HashMap;

public class ProcessorContext {
	public static final String OverlapMode = "OverlapMode";

	public static final String EXON_TREE = "ExonTree";
	
	private HashMap<String,Object> values;
	
	public void putValue(String key, Object value) {
		if (values==null) values = new HashMap<String,Object>();
		values.put(key, value);
	}
	
	public <T> T get(String key) {
		return (T) values.get(key);
	}

	
}
