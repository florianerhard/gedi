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
package gedi.util.dynamic.impl;

import gedi.util.ReflectionUtils;
import gedi.util.dynamic.DynamicObject;
import gedi.util.orm.Orm;
import gedi.util.orm.Orm.OrmInfo;

import java.util.Arrays;
import java.util.Collection;



public class ObjectDynamicObject implements DynamicObject {
	

	private Object o;
	private OrmInfo orm;
	
	public ObjectDynamicObject(Object o) {
		this.o = o;
		this.orm = Orm.getInfo(o.getClass());
	}

	@Override
	public Class<?> getType() {
		return Object.class;
	}


	@Override
	public boolean hasProperty(String property) {
		return orm.getIndex(property)!=-1;
	}
	
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
	public String toString() {
		return o.toString();
	}


	@Override
	public DynamicObject getEntry(int index) {
		return DynamicObject.getEmpty();
	}

	@Override
	public DynamicObject getEntry(String property) {
		try {
			return DynamicObject.from((Object)ReflectionUtils.get(o, property));
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException("Cannot wrap object in DynamicObject!",e);
		}
	}

	@Override
	public Collection<String> getProperties() {
		return Arrays.asList(orm.getDeclaredNames());
	}

	@Override
	public int length() {
		return 0;
	}
	
}
