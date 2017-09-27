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

package gedi.util.program;

import java.util.ArrayList;
import java.util.HashMap;

public class GediParameterSet {
	
	private HashMap<String,GediParameter<?>> map = new HashMap<>();
	
	public void add(GediParameter<?> p) {
		map.put(p.getName(), p);
	}

	public <T> GediParameter<T> get(String name) {
		return (GediParameter<T>) map.get(name);
	}

}
