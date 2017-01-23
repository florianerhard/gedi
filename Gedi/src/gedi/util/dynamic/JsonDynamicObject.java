package gedi.util.dynamic;

import gedi.util.StringUtils;

import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.StringTokenizer;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.lang.model.type.NullType;

public class JsonDynamicObject implements DynamicObject {

	private JsonValue j;
	
	JsonDynamicObject(JsonValue j) {
		this.j = j;
	}
	
	
	@Override
	public boolean hasProperty(String property) {
		return j.getValueType()==ValueType.OBJECT && ((JsonObject)j).containsKey(property);
	}

//	@Override
//	public DynamicObject get(String expression) {
//		JsonValue c = j;
//		StringTokenizer tokenizer = new StringTokenizer(expression, "[.", false);
//		int pos = 0;
//		while (tokenizer.hasMoreTokens()) {
//			String t = tokenizer.nextToken();
//			if (t.length()==0 && pos++==0) continue;
//			if (t.endsWith("]")) {
//				// array access, current value must be array!
//				if (c.getValueType()!=ValueType.ARRAY)
//					return DynamicObject.getEmpty();
//				JsonArray a = (JsonArray) c;
//				String ind = t.substring(0, t.length()-1);
//				if (!StringUtils.isInt(ind))
//					return DynamicObject.getEmpty();
//				int intind = Integer.parseInt(ind);
//				if (intind<0||intind>=a.size())
//					return DynamicObject.getEmpty();
//				
//				c = a.get(intind);
//			} else {
//				// member access, must be object
//				if (c.getValueType()!=ValueType.OBJECT)
//					return DynamicObject.getEmpty();
//				JsonObject o = (JsonObject) c;
//				c = o.get(t);
//				if (c==null)
//					return DynamicObject.getEmpty();
//			}
//			pos+=t.length();
//		}
//		return new JsonDynamicObject(c);
//	}

	@Override
	public Class<?> getType() {
		switch (j.getValueType()) {
		case NULL: return NullType.class;
		case FALSE: case TRUE: return Boolean.class;
		case NUMBER:
			JsonNumber n = (JsonNumber) j;
			if (n.isIntegral()) return Integer.class;
			return Double.class;
		case STRING: return String.class;
		case ARRAY: return Object[].class;
		case OBJECT: return Object.class;
		default: return Object.class;
		}
	}
	
	@Override
	public String asString() {
		if (j.getValueType()!=ValueType.STRING) return "";
		return ((JsonString)j).getString();
	}

	@Override
	public int asInt() {
		if (j.getValueType()!=ValueType.NUMBER) return 0;
		return ((JsonNumber)j).intValue();
	}

	@Override
	public double asDouble() {
		if (j.getValueType()!=ValueType.NUMBER) return 0;
		return ((JsonNumber)j).doubleValue();
	}

	@Override
	public boolean asBoolean() {
		return j.getValueType()==ValueType.TRUE;
	}



//	@Override
//	public DynamicObject[] asArray() {
//		if (j.getValueType()!=ValueType.ARRAY) return new DynamicObject[0];
//		JsonArray a = (JsonArray) j;
//		DynamicObject[] re = new DynamicObject[a.size()];
//		for (int i=0; i<re.length; i++) 
//			re[i] = new JsonDynamicObject(a.get(i));
//		return re;
//	}

	@Override
	public String toString() {
		return j.toString();
	}

	@Override
	public int length() {
		if (j.getValueType()==ValueType.ARRAY) return ((JsonArray)j).size();
		return 0;
	}

	@Override
	public DynamicObject getEntry(int index) {
		if (j.getValueType()==ValueType.ARRAY) {
			JsonArray a = ((JsonArray)j);
			if (index<0 || index>=a.size())
				return DynamicObject.getEmpty();
			return new JsonDynamicObject(a.get(index));
		}
		return DynamicObject.getEmpty();
	}

	@Override
	public DynamicObject getEntry(String property) {
		if (j.getValueType()==ValueType.OBJECT) {
			JsonValue re = ((JsonObject)j).get(property);
			if (re==null) return DynamicObject.getEmpty();
			return new JsonDynamicObject(re);
		}
		return DynamicObject.getEmpty();
	}

	@Override
	public Collection<String> getProperties() {
		if (j.getValueType()==ValueType.OBJECT) return ((JsonObject)j).keySet();
		return Collections.emptyList();
	}

}
