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
package gedi.util.io.text;

import gedi.util.ArrayUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.collections.intcollections.IntArrayList;

import java.util.Arrays;
import java.util.HashMap;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;


public class HeaderLine implements ToIntFunction<String>, Function<String,Integer> {

	private HashMap<String,Integer> header;
	private String[] fields;
	private String sep;
	
	public HeaderLine() {
	}
	
	
	
	public HeaderLine(String line) {
		this(line,'\t');
	}
	
	public HeaderLine(String line, String sep) {
		this(StringUtils.split(line, sep));
	}
	
	public HeaderLine(String line, char sep) {
		this(StringUtils.split(line, sep));
	}
	
	public HeaderLine(String[] fields) {
		set(fields);
	}
	
	
	public void set(String line) {
		set(line,'\t');
	}
	
	public void set(String line, String sep) {
		set(StringUtils.split(line, sep));
	}
	
	public void set(String line, char sep) {
		set(StringUtils.split(line, sep));
	}
	
	public void set(String[] fields) {
		header = ArrayUtils.createIndexMap(this.fields = fields);
		this.sep = "\t";
	}
	
	
	public HeaderLine trimLeft(int n) {
		return new HeaderLine(Arrays.copyOfRange(fields, n, fields.length));
	}
	
	public boolean hasField(String field) {
		return header.containsKey(field);
	}
	
	
	public boolean hasFields(String... fields) {
		for (String field : fields)
			if (!hasField(field))
				return false;
		return true;
	}
	
	public void addAlternative(String presentField, String alternative) {
		if (!hasField(presentField)) throw new IllegalArgumentException(presentField+" not found in header!");
		header.put(alternative, header.get(presentField));
	}
	
	public int get(String field, int defaultValue) {
		if (!header.containsKey(field)) return defaultValue;
		return header.get(field);
	}
	
	public int get(String field) {
		if (!header.containsKey(field)) throw new IllegalArgumentException("Field "+field+" not found in header!");
		return header.get(field);
	}
	
	public int getByName(String field) {
		if (!header.containsKey(field)) throw new IllegalArgumentException("Field "+field+" not found in header!");
		return header.get(field);
	}
	
	public String get(int index) {
		return fields[index];
	}
	
	public String getByIndex(int index) {
		return fields[index];
	}
	

	@Override
	public int applyAsInt(String value) {
		return apply(value);
	}
	
	@Override
	public Integer apply(String key) {
		return header.get(key);
	}
	
	
	@Override
	public String toString() {
		return ArrayUtils.concat(String.valueOf(sep), fields);
	}
	
	public int[] matchRegex(String regex) {
		Pattern p = Pattern.compile(regex);
		IntArrayList re = new IntArrayList(fields.length);
		for (int i=0; i<fields.length; i++)
			if (p.matcher(fields[i]).find())
				re.add(i);
		return re.toIntArray();
	}
	
	public int matchRegexFirst(String regex) {
		Pattern p = Pattern.compile(regex);
		for (int i=0; i<fields.length; i++)
			if (p.matcher(fields[i]).find())
				return i;
		return -1;
	}

	public int size() {
		return fields.length;
	}

	public String[] getFields() {
		return fields;
	}

	
}
