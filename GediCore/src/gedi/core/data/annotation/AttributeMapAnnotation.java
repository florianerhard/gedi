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
package gedi.core.data.annotation;

import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

public class AttributeMapAnnotation implements AttributesProvider, BinarySerializable {
	
	private HashMap<String, String> map;
	

	public AttributeMapAnnotation() {
	}
	
	public AttributeMapAnnotation(HashMap<String, String> map) {
		this.map = map;
	}

	@Override
	public Set<String> getAttributeNames() {
		return map.keySet();
	}

	@Override
	public Object getAttribute(String name) {
		return map.get(name);
	}

	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putCInt(map.size());
		for (String k : map.keySet()) {
			out.putString(k);
			out.putString(map.get(k));
		}
	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
		map = new HashMap<String, String>();
		int s = in.getCInt();
		for (int i=0; i<s; i++)
			map.put(in.getString(), in.getString());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((map == null) ? 0 : map.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AttributeMapAnnotation other = (AttributeMapAnnotation) obj;
		if (map == null) {
			if (other.map != null)
				return false;
		} else if (!map.equals(other.map))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return map.toString();
	}
	
	
	

}
