package gedi.util.dynamic.impl;

import gedi.util.dynamic.DynamicObject;

import java.util.Collection;
import java.util.Collections;

import javax.lang.model.type.NullType;



public class DoubleDynamicObject implements DynamicObject {
	

	private double a;
	
	public DoubleDynamicObject(double a) {
		this.a = a;
	}
	
	

	@Override
	public Class<?> getType() {
		return Double.class;
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
		return a;
	}

	@Override
	public boolean asBoolean() {
		return false;
	}


	@Override
	public String toString() {
		return a+"";
	}


	@Override
	public DynamicObject getEntry(int index) {
		return index==0?this:DynamicObject.getEmpty();
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
		return 0;
	}
	
}
