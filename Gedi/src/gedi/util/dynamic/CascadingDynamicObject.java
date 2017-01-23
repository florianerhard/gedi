package gedi.util.dynamic;

import gedi.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.StringTokenizer;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue.ValueType;


/**
 * All objects should be pure.
 * 
 * Object properties: all objects that have this property are cascaded further
 * Array entries: all array that are long enough are cascaded
 * Atomics: the last Atomic is used
 * 
 * E.g. two strings are cascaded as the second string!
 * @author erhard
 *
 */
public class CascadingDynamicObject implements DynamicObject {
	
	private DynamicObject[] m;
	
	CascadingDynamicObject(DynamicObject[] m) {
		this.m = m;
	}

	@Override
	public boolean hasProperty(String property) {
		for (DynamicObject o : m) 
			if (o.hasProperty(property))
				return true;
		return false;
	}
	
	@Override
	public Class<?> getType() {
		return m[m.length-1].getType();
	}


	@Override
	public String asString() {
		return m[m.length-1].asString();
	}

	@Override
	public int asInt() {
		return m[m.length-1].asInt();
	}

	@Override
	public double asDouble() {
		return m[m.length-1].asDouble();
	}

	@Override
	public boolean asBoolean() {
		return m[m.length-1].asBoolean();
	}


	@Override
	public int length() {
		int re = 0;
		for (DynamicObject o : m) {
			re=Math.max(re,o.length());
		}
		return re;
	}

	@Override
	public DynamicObject getEntry(int index) {
		
		int n = 0;
		for (DynamicObject o : m) 
			if (index<o.length())
				n++;
		
		if (n==1)
			for (DynamicObject o : m) 
				if (index<o.length())
					return o.getEntry(index);
		
		DynamicObject[] re = new DynamicObject[n];
		int i=0;
		for (DynamicObject o : m) 
			if (index<o.length())
				re[i++] = o.getEntry(index);
		
		return new CascadingDynamicObject(re);
	}

	@Override
	public DynamicObject getEntry(String property) {
		
		int n = 0;
		for (DynamicObject o : m) 
			if (o.hasProperty(property))
				n++;
		
		if (n==1)
			for (DynamicObject o : m) 
				if (o.hasProperty(property))
					return o.getEntry(property);
		
		DynamicObject[] re = new DynamicObject[n];
		int i=0;
		for (DynamicObject o : m) 
			if (o.hasProperty(property))
				re[i++] = o.getEntry(property);
		
		return new CascadingDynamicObject(re);
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
