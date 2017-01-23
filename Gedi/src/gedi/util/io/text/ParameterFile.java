package gedi.util.io.text;

import gedi.util.StringUtils;
import gedi.util.io.Directory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;


public class ParameterFile extends LineOrientedFile {

	private HashMap<String,String> parameters = new HashMap<String, String>();
	
	
	public ParameterFile(String path,char sep) throws IOException {
		super(path);
		loadIntoMemory(sep);
	}
	
	public ParameterFile(Directory dir, String name,char sep) throws IOException {
		super(dir, name);
		loadIntoMemory(sep);
	}


	public void loadIntoMemory(char sep) throws IOException {
		Iterator<String> it = lineIterator();
		while (it.hasNext()) {
			String[] f = StringUtils.split(it.next(), sep);
			if (f.length!=2) throw new IOException("Malformed parameter file!");
			parameters.put(f[0].toLowerCase(), f[1]);
		}
	}
	

	public String getStringParameter(String key) {
		return parameters.get(key.toLowerCase());
	}
	public int getIntParameter(String key) {
		return Integer.parseInt(getStringParameter(key));
	}
	public double getDoubleParameter(String key) {
		return Double.parseDouble(getStringParameter(key));
	}
	public String[] getStringArrayParameter(String key,char sep) {
		return StringUtils.split(getStringParameter(key), sep);
	}
	
}
