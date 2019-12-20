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
package gedi.core.data.reads;

import gedi.util.ArrayUtils;
import gedi.util.ParseUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.collections.intcollections.IntArrayList;
import gedi.util.io.text.HeaderLine;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.mutable.MutableInteger;
import gedi.util.mutable.MutablePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

public class BarcodeMapping {
	
	private int prefixLength = 0;
	
	private String[] conditions;
	
	private LinkedHashMap<String,Integer> mappedNames = new LinkedHashMap<>();
	private LinkedHashMap<String,LinkedHashMap<String,MutablePair<String,Integer>>> map = new LinkedHashMap<>();
	
	public BarcodeMapping(String[] conditions) {
		this.conditions = conditions;
	}
	
	public BarcodeMapping(String[] conditions, String file) throws IOException {
		this.conditions = conditions;
		
		HeaderLine h = new HeaderLine();
		new LineOrientedFile(file).lineIterator()
			.header(h)
			.split('\t')
			.sideEffect(a->{
				if (prefixLength==0) prefixLength = a[h.get("Barcode")].length();
				if (a[h.get("Barcode")].length()!=prefixLength)
					throw new RuntimeException("All barcodes must have the same length!");
			})
			.forEachRemaining(a->addMapping(a[h.get("Condition")],a[h.get("Barcode")],a[h.get("Name")]));
	}
	
	public int getPrefixLength() {
		return prefixLength;
	}
	
	
	public void addMapping(String condition, String barcode, String name) {
		map.computeIfAbsent(condition, x->new LinkedHashMap<>()).put(barcode, new MutablePair<>(name,mappedNames.computeIfAbsent(name,x->mappedNames.size())));
	}
	
	public int getMappedNumConditions() {
		return mappedNames.size();
	}
	
	public void clear() {
		map.clear();
	}
	
	public String getName(String condition, String barcode) {
		return map.get(condition).get(barcode).Item1;
	}
	
	public int getIndex(String condition, String barcode) {
		Integer re = map.get(condition).get(barcode).Item2;
		if (re==null) return -1;
		return re;
	}
	
	public int getIndex(int condition, String barcode) {
		MutablePair<String, Integer> re = map.get(conditions[condition]).get(barcode);
		if (re==null) return -1;
		return re.Item2;
	}
	
	
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Condition\tBarcode\tName\n");
		for (String c : map.keySet())
			for (String bc : map.get(c).keySet())
				sb.append(c).append("\t").append(bc).append("\t").append(map.get(c).get(bc)).append("\n");
		return sb.toString();
	}
	
}
