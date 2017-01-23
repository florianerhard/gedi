package gedi.util.dynamic.impl;

import gedi.util.StringUtils;
import gedi.util.dynamic.DynamicObject;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.lang.model.type.NullType;



public class ListDynamicObject implements DynamicObject {
	

	private List<?> a;
	
	public ListDynamicObject(List<?> a) {
		this.a = a;
	}

	@Override
	public Class<?> getType() {
		return Object[].class;
	}


	@Override
	public boolean hasProperty(String property) {
		return false;
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
		return DynamicObject.from(a.get(index));
	}

	@Override
	public DynamicObject getEntry(String property) {
		return DynamicObject.getEmpty();
	}

	@Override
	public Collection<String> getProperties() {
		return Collections.emptyList();
	}

	@Override
	public int length() {
		return a.size();
	}
	
}
