package gedi.util.parsing;

import java.awt.Color;
import java.awt.Paint;
import java.util.HashMap;


public class ParserCache {

	private static ParserCache instance;
	
	public static ParserCache getInstance() {
		if (instance==null) {
			instance = new ParserCache();
			instance.map.put(String.class.getName(), new StringParser());
			instance.map.put(Integer.class.getName(), new IntegerParser());
			instance.map.put(Integer.TYPE.getName(), new IntegerParser());
			instance.map.put(Double.class.getName(), new DoubleParser());
			instance.map.put(Double.TYPE.getName(), new DoubleParser());
			instance.map.put(Float.class.getName(), new FloatParser());
			instance.map.put(Float.TYPE.getName(), new FloatParser());
			instance.map.put(Boolean.class.getName(), new BooleanParser());
			instance.map.put(Boolean.TYPE.getName(), new BooleanParser());
			instance.map.put(Class.class.getName(), new ClassParser());
			instance.map.put(int[].class.getName(), new IntArrayParser());
			instance.map.put(double[].class.getName(), new DoubleArrayParser());
			instance.map.put(Color.class.getName(), new ColorParser());
			instance.map.put(Paint.class.getName(), new PaintParser());
		}
		
		return instance;
	}
	
	private HashMap<String,Parser> map = new HashMap<String, Parser>();
	
	private ParserCache() {}
	
	public <T> Parser<T> get(Class<T> cls) {
		 if (cls.isEnum())
			 return new EnumParser<T>(cls);
		return map.get(cls.getName());
	}
	
}
