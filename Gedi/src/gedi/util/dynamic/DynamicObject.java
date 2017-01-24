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

import gedi.util.ParseUtils;
import gedi.util.ReflectionUtils;
import gedi.util.StringUtils;
import gedi.util.dynamic.impl.ArrayDynamicObject;
import gedi.util.dynamic.impl.BooleanDynamicObject;
import gedi.util.dynamic.impl.DoubleDynamicObject;
import gedi.util.dynamic.impl.EmptyDynamicObject;
import gedi.util.dynamic.impl.IntDynamicObject;
import gedi.util.dynamic.impl.ListDynamicObject;
import gedi.util.dynamic.impl.MapDynamicObject;
import gedi.util.dynamic.impl.StringDynamicObject;
import gedi.util.orm.Orm;
import gedi.util.properties.PropertyProvider;

import java.io.StringReader;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.Property;
import javafx.beans.property.StringProperty;

import javax.json.Json;
import javax.lang.model.type.NullType;


/**
 * None of the methods should throw exceptions (instead, return default values!)
 * @author erhard
 *
 */
public interface DynamicObject {
	

	/**
	 * One of: Boolean.class, Integer.class, Double.class, String.class,
	 * Object.class, Object[].class or NullType.class
	 * @return
	 */
	Class<?> getType();
	
	/**
	 * or 0 if not an array
	 * @return
	 */
	int length();
	/**
	 * Or empty if not an array
	 * @param index
	 * @return
	 */
	DynamicObject getEntry(int index);
	
	/**
	 * Or empty if not an object
	 * @param property
	 * @return
	 */
	DynamicObject getEntry(String property);
	boolean hasProperty(String property);
	/**
	 * Empty if not an object
	 * @return
	 */
	Collection<String> getProperties();
	
	/**
	 * Or empty string, if not a string
	 * @return
	 */
	String asString();
	/**
	 * Or 0 if not an int
	 * @return
	 */
	int asInt();
	/**
	 * Or 0 if not a double
	 * @return
	 */
	double asDouble();
	/**
	 * Or false if not a boolean
	 * @return
	 */
	boolean asBoolean();
	
	default boolean isNull() {
		return getType()==NullType.class;
	}
	
	default boolean isBoolean() {
		return getType()==Boolean.class;
	}
	
	default boolean isString() {
		return getType()==String.class;
	}
	
	default boolean isInt() {
		return getType()==Integer.class;
	}
	
	default boolean isDouble() {
		return getType()==Double.class;
	}
	
	default boolean isArray() {
		return getType()==Object[].class;
	}
	
	default boolean isObject() {
		return getType()==Object.class;
	}
	
	
	/**
	 * Or defaultValue, if not a string
	 * @return
	 */
	default String asString(String defaultValue) {
		if (!isString()) return defaultValue;
		return asString();
	}
	/**
	 * Or defaultValue if not an int
	 * @return
	 */
	default int asInt(int defaultValue) {
		if (!isInt()) return defaultValue;
		return asInt();
	}
	/**
	 * Or defaultValue if not a double
	 * @return
	 */
	default double asDouble(double defaultValue) {
		if (!isDouble() && !isInt()) return defaultValue;
		return asDouble();
	}
	/**
	 * Or defaultValue if not a boolean
	 * @return
	 */
	default boolean asBoolean(boolean defaultValue) {
		if (!isBoolean()) return defaultValue;
		return asBoolean();
	}
	
	default DynamicObject get(String expression) {
		if (expression.length()==0) return this;
		if (expression.startsWith(".")) expression = expression.substring(1);
		
		// array access
		if (expression.startsWith("[")) {
			int o = expression.indexOf(']');
			int ind = Integer.parseInt(expression.substring(1,o));
			return getEntry(ind).get(expression.substring(o+1));
		}
		
		int o1 = expression.indexOf('.');
		int o2 = expression.indexOf('[');
		
		if (o1==-1) o1 = Integer.MAX_VALUE;
		if (o2==-1) o2 = Integer.MAX_VALUE;
		
		int o = Math.min(o1, o2);
		
		String prop = o<Integer.MAX_VALUE?expression.substring(0,o):expression;
		expression = o<Integer.MAX_VALUE?expression.substring(o):"";
		return getEntry(prop).get(expression);
	}
	

	default String toJson() {
		StringBuilder sb = new StringBuilder();
		toJson(sb);
		return sb.toString();
	}

	default void toJson(StringBuilder sb) {
		if (isNull()) sb.append("null");
		else if (isBoolean()) sb.append(asBoolean());
		else if (isInt()) sb.append(asInt());
		else if (isDouble()) sb.append(asDouble());
		else if (isString()) sb.append("\"").append(asString()).append("\"");
		else if (isArray()) {
			sb.append("[");
			for (int i=0; i<length(); i++) {
				if (i>0) sb.append(",");
				getEntry(i).toJson(sb);
			}
			sb.append("]");
		} else if (isObject()) {
			sb.append("{");
			int e = 0;
			for (String p : getProperties()) {
				if (e++>0) sb.append(",");
				sb.append("\"").append(p).append("\"").append(":");
				getEntry(p).toJson(sb);
			}
			sb.append("}");
		}
	}
	
	default DynamicObject[] asArray() {
		DynamicObject[] re = new DynamicObject[length()];
		for (int i=0; i<re.length; i++)
			re[i] = getEntry(i);
		return re;
	}
	
	public static DynamicObject from(Map<String,?> map) {
		return new MapDynamicObject(map);
	}
	
	public static DynamicObject from(Object[] arr) {
		return new ArrayDynamicObject(arr);
	}
	
	public static DynamicObject from(List<?> list) {
		return new ListDynamicObject(list);
	}
	
	public static DynamicObject from(String a) {
		return new StringDynamicObject(a);
	}
	
	public static DynamicObject from(String key, Object value) {
		HashMap<String,Object> m = new HashMap<>();
		m.put(key, value);
		return from(m);
	}
	
	public static DynamicObject from(int a) {
		return new IntDynamicObject(a);
	}

	public static DynamicObject from(double a) {
		return new DoubleDynamicObject(a);
	}

	public static DynamicObject from(boolean a) {
		return new BooleanDynamicObject(a);
	}

	public static DynamicObject from() {
		return getEmpty();
	}
	
	public static DynamicObject from(Object o) {
		if (o==null) return getEmpty();
		if (o instanceof DynamicObject) return (DynamicObject) o;
		if (o instanceof String) return from((String)o);
		if (o instanceof Integer) return from(((Integer)o).intValue());
		if (o instanceof Double) return from(((Double)o).doubleValue());
		if (o instanceof Boolean) return from(((Boolean)o).booleanValue());
		if (o instanceof List) return from(((List)o).toArray());
		if (o.getClass().isArray()) return from((Object[])o);
		if (o instanceof Map) return from((Map)o);
		throw new RuntimeException("Cannot create Dynamic object!");
	}


	

	public static DynamicObject parseJson(String json) {
		if (json==null || json.length()==0 || json.equals("null")) return getEmpty();
		if (!json.startsWith("[") && !json.startsWith("{")) {
			if (json.toLowerCase().equals("false") || json.equals("F"))
				return new BooleanDynamicObject(false);
			if (json.toLowerCase().equals("true") || json.equals("T"))
				return new BooleanDynamicObject(true);
			if (StringUtils.isInt(json))
				return new IntDynamicObject(Integer.parseInt(json));
			if (StringUtils.isNumeric(json))
				return new DoubleDynamicObject(Double.parseDouble(json));
			if (json.startsWith("\"") && json.endsWith("\""))
				return new StringDynamicObject(json.substring(1, json.length()-1));
		}
		return new JsonDynamicObject(Json.createReader(new StringReader(json)).read());
	}
	
	public static DynamicObject parseJsonOrString(String json) {
		if (json==null || json.length()==0 || json.equals("null")) return getEmpty();
		if (!json.startsWith("[") && !json.startsWith("{")) {
			if (json.toLowerCase().equals("false") || json.equals("F"))
				return new BooleanDynamicObject(false);
			if (json.toLowerCase().equals("true") || json.equals("T"))
				return new BooleanDynamicObject(true);
			if (StringUtils.isInt(json))
				return new IntDynamicObject(Integer.parseInt(json));
			if (StringUtils.isNumeric(json))
				return new DoubleDynamicObject(Double.parseDouble(json));
			if (json.startsWith("\"") && json.endsWith("\""))
				json = json.substring(1, json.length()-1);
			return new StringDynamicObject(json);
		}
		return new JsonDynamicObject(Json.createReader(new StringReader(json)).read());
	}
	
	
	public static DynamicObject getEmpty() {
		return EmptyDynamicObject.instance;
	}
	
	public static DynamicObject merge(DynamicObject...objects) {
		if (objects.length==0) return getEmpty();
		if (objects.length==1) return objects[0];
		return new MergedDynamicObject(objects);
	}
	
	public static DynamicObject merge(Collection<DynamicObject> objects) {
		if (objects.size()==0) return getEmpty();
		if (objects.size()==1) return objects.iterator().next();
		return new MergedDynamicObject(objects.toArray(new DynamicObject[0]));
	}
	
	public static DynamicObject cascade(DynamicObject...objects) {
		if (objects.length==0) return getEmpty();
		if (objects.length==1) return objects[0];
		return new CascadingDynamicObject(objects);
	}
	
	public static DynamicObject cascade(Collection<DynamicObject> objects) {
		if (objects.size()==0) return getEmpty();
		if (objects.size()==1) return objects.iterator().next();
		return new CascadingDynamicObject(objects.toArray(new DynamicObject[0]));
	}

	/**
	 * Tries to set properties of o (via setter or {@link PropertyProvider} if this is a Object, via array access if this is an array) from this object 
	 * @param o
	 */
	default <T> T applyTo(T o) {
		if (o==null) return o;

		if (isArray() && o.getClass().isArray()) {
			int to = Math.min(length(), Array.getLength(o));
			for (int i=0; i<to; i++) {
				DynamicObject en = getEntry(i);
				if (en.isBoolean() && o instanceof boolean[])
					Array.setBoolean(o, i, en.asBoolean());
				else if (en.isDouble() && o instanceof double[])
					Array.setDouble(o, i, en.asDouble());
				else if (en.isInt() && o instanceof int[])
					Array.setInt(o, i, en.asInt());
				else if (en.isString() && o instanceof String[])
					Array.set(o, i, en.asString());
				else if (en.isArray() || en.isObject()) {
					Object too = Array.get(o, i);
					if (too==null) {
						too = Orm.create(o.getClass().getComponentType());
						Array.set(o, i, too);
					}
					en.applyTo(too);
				}
			}
		}
		
		else if (isObject() && !o.getClass().isPrimitive() && !o.getClass().isArray()) {
			
			PropertyProvider pp = (o instanceof PropertyProvider)?(PropertyProvider)o:null;
			
			for (String propName : getProperties()) {

				DynamicObject en = getEntry(propName);
				
				if (pp!=null && pp.getProperties().containsKey(propName)) {
					
					Property p = pp.getProperty(propName);
					
					if (pp.getPropertyClass(propName)==(Class)DynamicObject.class)
						p.setValue(en);
					else if (en.isBoolean() && p instanceof BooleanProperty)
						((BooleanProperty)p).set(en.asBoolean());
					else if (en.isDouble() && p instanceof DoubleProperty)
						((DoubleProperty)p).set(en.asDouble());
					else if (en.isInt() && p instanceof IntegerProperty)
						((IntegerProperty)p).set(en.asInt());
					else if (en.isString() && p instanceof StringProperty)
						((StringProperty)p).set(en.asString());
					else 
						en.applyTo(p.getValue());
				}
				
				else {
					
					try {
						if (en.isBoolean()) {
							Method m = ReflectionUtils.findMethodIgnoreCase(o.getClass(), "set"+propName, Boolean.TYPE);
							if (m==null)
								m = ReflectionUtils.findMethodIgnoreCase(o.getClass(), "set"+propName, Boolean.class);
	//						if (m==null) throw new RuntimeException("Cannot apply "+propName+" to class "+o.getClass().getName());
							if (m!=null)
								m.invoke(o, en.asBoolean());
						}
						else if (en.isInt()) {
							Method m = ReflectionUtils.findMethodIgnoreCase(o.getClass(), "set"+propName, Integer.TYPE);
							if (m==null)
								m = ReflectionUtils.findMethodIgnoreCase(o.getClass(), "set"+propName, Integer.class);
	//						if (m==null) throw new RuntimeException("Cannot apply "+propName+" to class "+o.getClass().getName());
							if (m!=null)
								m.invoke(o, en.asInt());
						}
						else if (en.isDouble()) {
							Method m = ReflectionUtils.findMethodIgnoreCase(o.getClass(), "set"+propName, Double.TYPE);
							if (m==null)
								m = ReflectionUtils.findMethodIgnoreCase(o.getClass(), "set"+propName, Double.class);
	//						if (m==null) throw new RuntimeException("Cannot apply "+propName+" to class "+o.getClass().getName());
							if (m!=null)
								m.invoke(o, en.asDouble());
						}
						else if (en.isString()) {
							Method m = ReflectionUtils.findMethodIgnoreCase(o.getClass(), "set"+propName, String.class);
	//						if (m==null) throw new RuntimeException("Cannot apply "+propName+" to class "+o.getClass().getName());
							if (m!=null)
								m.invoke(o, en.asString());
						}
							
						
						Method setter = null;
						Method getter = null;
						for (Method m : o.getClass().getMethods()){
							if (m.getName().equalsIgnoreCase("set"+propName) && m.getParameterTypes().length==1)
								setter = m;
							else if ((m.getName().equalsIgnoreCase("get"+propName) || m.getName().equalsIgnoreCase("is"+propName)) && m.getParameterTypes().length==0)
								getter = m;
						}
						if (getter!=null || setter!=null) {
							
							Class type = setter!=null ? setter.getParameterTypes()[0] : getter.getReturnType();
							
							if (type==DynamicObject.class && setter!=null)
								setter.invoke(o, en);
							else if (en.isBoolean() && type==boolean.class && setter!=null)
								setter.invoke(o, en.asBoolean());
							else if (en.isDouble() && type==double.class && setter!=null)
								setter.invoke(o, en.asDouble());
							else if (en.isInt() && type==int.class && setter!=null)
								setter.invoke(o, en.asInt());
							else if (en.isString() && type==String.class && setter!=null)
								setter.invoke(o, en.asString());
							else if (getter!=null)
								en.applyTo(getter.invoke(o));
							
							
						}
					} catch (Exception e) {}						
				}
				
				
			}
		}
		return o;
		
	}

	/**
	 * Handles a.b[4].c = value
	 * @param accessor
	 * @param value
	 * @return
	 */
	public static DynamicObject parseExpression(String expression, DynamicObject value) {
		if (expression.length()==0) return value;
		if (expression.startsWith(".")) expression = expression.substring(1);
		
		// array access
		if (expression.startsWith("[")) {
			int o = expression.indexOf(']');
			int ind = Integer.parseInt(expression.substring(1,o));
			Object[] a = new Object[ind+1];
			for (int i=0; i<ind; i++) a[i] = DynamicObject.getEmpty();
			a[ind] = parseExpression(expression.substring(o+1), value);
			return new ArrayDynamicObject(a);
		}
		
		int o1 = expression.indexOf('.');
		int o2 = expression.indexOf('[');
		
		if (o1==-1) o1 = Integer.MAX_VALUE;
		if (o2==-1) o2 = Integer.MAX_VALUE;
		
		int o = Math.min(o1, o2);
		
		String prop = o<Integer.MAX_VALUE?expression.substring(0,o):expression;
		expression = o<Integer.MAX_VALUE?expression.substring(o):"";
		
		HashMap<String, Object> map = new HashMap<>();
		map.put(prop, parseExpression(expression, value));
		return new MapDynamicObject(map);
	}
	
}
