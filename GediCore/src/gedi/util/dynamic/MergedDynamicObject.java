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
package gedi.util.dynamic;

import gedi.util.ArrayUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import javax.lang.model.type.NullType;


/**
 * If all are objects, this will be an object. Otherwise, it will be an array!
 * 
 * E.g. two strings are merged as an array!
 * @author erhard
 *
 */
public class MergedDynamicObject implements DynamicObject {
	
	private DynamicObject[] m;
	private Class<?> type;
	
	MergedDynamicObject(DynamicObject[] m) {
		this.m = ArrayUtils.redimPreserve(m, ArrayUtils.remove(m, p->!p.isNull()));
	}
//
//	@Override
//	public DynamicObject get(String expression) {
//		
//		StringTokenizer tokenizer = new StringTokenizer(expression, "[.", false);
//		int pos = 0;
//		while (tokenizer.hasMoreTokens()) {
//			String t = tokenizer.nextToken();
//			if (t.length()==0 && pos++==0) continue;
//			if (t.endsWith("]")) {
//				// array access, current value must be array!
//				if (getType()!=Object[].class)
//					return DynamicObject.getEmpty();
//				String ind = t.substring(0, t.length()-1);
//				if (!StringUtils.isInt(ind))
//					return DynamicObject.getEmpty();
//				int intind = Integer.parseInt(ind);
//				int i = 0;
//				for (DynamicObject o : m) {
//					if (o.getType()==Object[].class) {
//						int ni = i+o.arraySize();
//						if (ni>intind) {
//							return o.get("["+(intind-i)+"]"+expression.substring(pos+t.length()));
//						}
//						i = ni;
//					}
//					else {
//						if (i+1==intind) {
//							return o;
//						}
//						i++;
//					}
//				}
//				return DynamicObject.getEmpty();
//			} else {
//				// member access, must be object
//				if (getType()!=Object.class)
//					return DynamicObject.getEmpty();
//				ArrayList<DynamicObject> mer = new ArrayList<DynamicObject>();
//				for (DynamicObject o : m) {
//					if (o.hasProperty(t))
//						mer.add(o.get(t));
//				}
//				return DynamicObject.merge(mer).get(expression.substring(1+pos+t.length()));
//			}
//		}
//		return DynamicObject.getEmpty();
//	}

	@Override
	public boolean hasProperty(String property) {
		for (DynamicObject o : m) 
			if (o.hasProperty(property))
				return true;
		return false;
	}
	
	@Override
	public Class<?> getType() {
		if (type==null) {
			if (m.length==0)
				return NullType.class;
			type = Object.class;
			for (DynamicObject o : m)
				if (o.getType()!=Object.class) {
					type = Object[].class;
					break;
				}
		}
		return type;
	}

//	@Override
//	public DynamicObject[] asArray() {
//		ArrayList<DynamicObject> re = new ArrayList<DynamicObject>();
//		for (DynamicObject o : m)
//			re.addAll(Arrays.asList(o.asArray()));
//		return re.toArray(new DynamicObject[0]);
//	}

	@Override
	public String asString() {
		return "";
	}

	@Override
	public int asInt() {
		return 0;
	}

	@Override
	public double asDouble() {
		return 0;
	}

	@Override
	public boolean asBoolean() {
		return false;
	}


	@Override
	public int length() {
		if (getType()!=Object[].class) return 0;
		int re = 0;
		for (DynamicObject o : m) {
			if (o.getType()==Object[].class) 
				re+=o.length();
			else
				re++;
		}
		return re;
	}

	@Override
	public DynamicObject getEntry(int index) {
		if (getType()!=Object[].class) return DynamicObject.getEmpty();
		
		int i = 0;
		for (DynamicObject o : m) {
			if (o.getType()==Object[].class) {
				int ni = i+o.length();
				if (ni>index) {
					return o.getEntry(index-i);
				}
				i = ni;
			}
			else {
				if (i+1==index) {
					return o;
				}
				i++;
			}
		}
		
		return DynamicObject.getEmpty();
	}

	@Override
	public DynamicObject getEntry(String property) {
		if (getType()!=Object.class)
			return DynamicObject.getEmpty();
		
		ArrayList<DynamicObject> mer = new ArrayList<DynamicObject>();
		for (DynamicObject o : m) {
			if (o.hasProperty(property))
				mer.add(o.getEntry(property));
		}
		return DynamicObject.merge(mer);
	}

	@Override
	public Collection<String> getProperties() {
		if (getType()!=Object.class)
			return Collections.emptySet();
		
		HashSet<String> re = new HashSet<String>();
		for (DynamicObject o : m) 
			re.addAll(o.getProperties());
		return re;
	}

	@Override
	public String toString() {
		return toJson();
	}
}
