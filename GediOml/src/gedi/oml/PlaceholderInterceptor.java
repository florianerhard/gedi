package gedi.oml;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class PlaceholderInterceptor implements OmlInterceptor {

	private HashMap<String,String> placeholder = new HashMap<String, String>();
	
	public PlaceholderInterceptor() {
	}
	
	public PlaceholderInterceptor(PlaceholderInterceptor pi) {
		this.placeholder.putAll(pi.placeholder);
	}
	
	
	public PlaceholderInterceptor addPlaceHolder(String key, String value) {
		placeholder.put("${"+key+"}", value);
		return this;
	}
	
	public PlaceholderInterceptor addPlaceHolders(Map<String,?> map) {
		for (String k : map.keySet())
			addPlaceHolder(k, map.get(k).toString());
		return this;
	}
	
	@Override
	public LinkedHashMap<String, String> getAttributes(OmlNode node,
			LinkedHashMap<String, String> attributes,
			HashMap<String, Object> context) {
		
		for (String k : attributes.keySet()) {
			String v = attributes.get(k);
			for (String p : placeholder.keySet()) {
				if (placeholder.get(p)==null) throw new RuntimeException("Cannot replace "+p);
				v = v.replace(p, placeholder.get(p));
			}
			attributes.put(k, v);
		}
		
		return attributes;
	}


}
