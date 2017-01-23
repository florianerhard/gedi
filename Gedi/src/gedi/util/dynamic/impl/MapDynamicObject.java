package gedi.util.dynamic.impl;

import gedi.util.dynamic.DynamicObject;

import java.util.Collection;
import java.util.Map;



public class MapDynamicObject implements DynamicObject {
	

	private Map<String,?> a;
	
	public MapDynamicObject(Map<String,?> a) {
		this.a = a;
	}

	@Override
	public Class<?> getType() {
		return Object.class;
	}


	@Override
	public boolean hasProperty(String property) {
		return a.containsKey(property);
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
		return a.toString();
	}


	@Override
	public DynamicObject getEntry(int index) {
		return DynamicObject.getEmpty();
	}

	@Override
	public DynamicObject getEntry(String property) {
		return DynamicObject.from(a.get(property));
	}

	@Override
	public Collection<String> getProperties() {
		return a.keySet();
	}

	@Override
	public int length() {
		return 0;
	}
	
}
