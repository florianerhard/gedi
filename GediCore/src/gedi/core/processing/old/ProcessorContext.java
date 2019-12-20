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
package gedi.core.processing.old;

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
