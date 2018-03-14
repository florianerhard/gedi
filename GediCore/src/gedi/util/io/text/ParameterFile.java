/**
 * 
 *    Copyright 2017 Florian Erhard
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 */
package gedi.util.io.text;

import gedi.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;


public class ParameterFile extends LineOrientedFile {

	private HashMap<String,String> parameters = new HashMap<String, String>();
	
	
	public ParameterFile(String path,char sep) throws IOException {
		super(path);
		loadIntoMemory(sep);
	}
	
	public ParameterFile(File dir, String name,char sep) throws IOException {
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
