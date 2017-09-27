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
