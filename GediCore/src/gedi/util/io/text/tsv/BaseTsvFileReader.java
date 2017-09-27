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

package gedi.util.io.text.tsv;

import gedi.core.region.intervalTree.MemoryIntervalTreeStorage;
import gedi.util.ArrayUtils;
import gedi.util.StringUtils;
import gedi.util.io.text.HeaderLine;
import gedi.util.io.text.LineWriter;
import gedi.util.parsing.Parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

public abstract class BaseTsvFileReader<D> {

	protected BiFunction<HeaderLine,String[],String> lineChecker;
	protected Class<D> type;
	

	public BaseTsvFileReader(Class<D> type) {
		this.type = type;
	}

	public MemoryIntervalTreeStorage<D> readIntoMemoryTakeFirst() throws IOException {
		return readIntoMemoryTakeFirst((LineWriter)null);
	}
	public MemoryIntervalTreeStorage<D> readIntoMemoryTakeFirst(LineWriter nonunique) throws IOException {
		return readIntoMemory((c,d)->{
			if (c!=null) {
				if (nonunique!=null)
					nonunique.writeLine2("GenomicRegion not unique (for "+c+" and "+d+")");
				return c;
			}
			return d;
		}, type);
	}
	
	public MemoryIntervalTreeStorage<D> readIntoMemoryTakeLast() throws IOException {
		return readIntoMemory((c,d)->{
			return d;
		}, type);
	}
	
	public MemoryIntervalTreeStorage<D> readIntoMemoryThrowOnNonUnique() throws IOException {
		return readIntoMemory((c,d)->{
			if (c!=null && !c.equals(d)) 
				throw new RuntimeException("GenomicRegion not unique (for "+c+" and "+d+")");
			return d;
		}, type);
	}
	
	public MemoryIntervalTreeStorage<D[]> readIntoMemoryArrayCombiner(MemoryIntervalTreeStorage<D[]> re, Class<D> cls) throws IOException {
		return readIntoMemory(re,(c,d)->c==null?ArrayUtils.getSingletonArray(d,cls):ArrayUtils.append(c,d));
	}

	public MemoryIntervalTreeStorage<D> readIntoMemoryTakeFirst(MemoryIntervalTreeStorage<D> re) throws IOException {
		return readIntoMemory(re,(c,d)->{
			if (c!=null) return c;
			return d;
		});
	}
	
	public MemoryIntervalTreeStorage<D> readIntoMemoryTakeLast(MemoryIntervalTreeStorage<D> re) throws IOException {
		return readIntoMemory(re,(c,d)->{
			return d;
		});
	}
	
	public MemoryIntervalTreeStorage<D> readIntoMemoryThrowOnNonUnique(MemoryIntervalTreeStorage<D> re) throws IOException {
		return readIntoMemory(re,(c,d)->{
			if (c!=null && !c.equals(d)) 
				throw new RuntimeException("GenomicRegion not unique (for "+c+" and "+d+")");
			return d;
		});
	}
	
	public MemoryIntervalTreeStorage<D[]> readIntoMemoryArrayCombiner(Class<D> cls) throws IOException {
		try {
			return readIntoMemory((c,d)->c==null?ArrayUtils.getSingletonArray(d,cls):ArrayUtils.append(c,d),(Class<D[]>)Class.forName("[L" + cls.getName() + ";"));
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Cannot infer array class");
		}
	}

	
	public <C> MemoryIntervalTreeStorage<C> readIntoMemory(BiFunction<C, D, C> combiner, Class<C> type) throws IOException {
		return readIntoMemory(new MemoryIntervalTreeStorage<C>(type),combiner);
	}
	
	
	public abstract <C> MemoryIntervalTreeStorage<C> readIntoMemory(MemoryIntervalTreeStorage<C> re, BiFunction<C, D, C> combiner) throws IOException;
	
	
	protected ErrorChecker check() {
		lineChecker = new ErrorChecker();
		return (ErrorChecker) lineChecker;
	}
	
	protected static class ErrorChecker implements BiFunction<HeaderLine, String[], String> {

		private ArrayList<BiFunction<HeaderLine, String[], String>> list = new ArrayList<BiFunction<HeaderLine,String[],String>>();
		
		@Override
		public String apply(HeaderLine t, String[] u) {
			
			StringBuilder sb = null;
			for (BiFunction<HeaderLine, String[], String> ch : list) {
				String re = ch.apply(t, u);
				if (re!=null) {
					if (sb==null) sb = new StringBuilder();
					sb.append(re).append("\n");
				}
			}
			return sb==null?null:sb.toString();
		}

		
		public ErrorChecker fieldCount(int c) {
			list.add(new FieldCountChecker(c, c));
			return this;
		}
		
		public ErrorChecker fieldCount(int min, int max) {
			list.add(new FieldCountChecker(min, max));
			return this;
		}
		public ErrorChecker fieldContent(int f, Pattern pattern) {
			list.add(new FieldContentChecker(f, fi->pattern.matcher(fi).find()?null:"Cannot find "+pattern.toString()+" in field "+fi));
			return this;
		}
		public ErrorChecker fieldContent(int f, Function<String,String> predicate) {
			list.add(new FieldContentChecker(f, predicate));
			return this;
		}
		public ErrorChecker fieldType(int f, Parser<?> type) {
			list.add(new FieldTypeChecker(f,type));
			return this;
		}
		
		public ErrorChecker fieldType(String f, Parser<?> type) {
			list.add(new FieldTypeChecker(f,type));
			return this;
		}
	}
	
	protected static class FieldContentChecker implements BiFunction<HeaderLine, String[], String> {

		private int field;
		private Function<String,String> predicate;

		public FieldContentChecker(int field, Function<String, String> predicate) {
			this.field = field;
			this.predicate = predicate;
		}


		@Override
		public String apply(HeaderLine t, String[] u) {
			if (field>=u.length) return "Too few fields : "+u.length+" <= "+field;
			return predicate.apply(u[field]);
		}
		
	}
	
	protected static class FieldCountChecker implements BiFunction<HeaderLine, String[], String> {

		private int minFields;
		private int maxFields;
		
		
		public FieldCountChecker(int minFields, int maxFields) {
			this.minFields = minFields;
			this.maxFields = maxFields;
		}

		@Override
		public String apply(HeaderLine t, String[] u) {
			if (u.length<minFields) return "Too few fields: "+u.length+" < "+minFields;
			if (u.length>maxFields) return "Too many fields: "+u.length+" > "+maxFields;
			return null;
		}
		
	}
	
	protected static class FieldTypeChecker implements BiFunction<HeaderLine, String[], String> {

		private int field;
		private String fieldName;
		private Parser<?> parser;
		

		public FieldTypeChecker(String fieldName, Parser<?> parser) {
			this.fieldName = fieldName;
			this.parser = parser;
		}

		public FieldTypeChecker(int field, Parser<?> parser) {
			this.field = field;
			this.parser = parser;
		}


		@Override
		public String apply(HeaderLine t, String[] u) {
			if (fieldName!=null) {
				if (!parser.canParse(u[t.get(fieldName)])) 
					return "Field "+fieldName+" must be of "+parser.getParsedType();
			}
			else {
				if (field>=u.length) return "Too few fields : "+u.length+" <= "+field;
				if (!parser.canParse(u[field])) {
					String fieldName = t!=null?t.get(field):null;
					if (fieldName==null) fieldName=field+"";
					return "Field "+fieldName+" must be of "+parser.getParsedType();
				}
			}
			return null;
		}
		
	}
	
}
