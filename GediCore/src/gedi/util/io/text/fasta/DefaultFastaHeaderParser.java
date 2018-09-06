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
package gedi.util.io.text.fasta;

import gedi.util.StringUtils;
import gedi.util.mutable.MutablePair;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DefaultFastaHeaderParser extends AbstractFastaHeaderParser{

	private MutablePair<Integer,Integer>[] segments;
	private Pattern pattern;
	private String[] keys;
	private Character splitChar;
	
	
	public DefaultFastaHeaderParser() {
		super(1);
	}
	
	public DefaultFastaHeaderParser(Pattern pattern, String[] keys) {
		super(keys.length);
		this.pattern = pattern;
		this.keys = keys;
	}
	
	public DefaultFastaHeaderParser(Pattern pattern) {
		super(-1);
		this.pattern = pattern;
	}
	
	/**
	 * Segments specifies the start (inclusive) and stop (exclusive) position within the header
	 * @param segments
	 * @param keys
	 */
	public DefaultFastaHeaderParser(MutablePair<Integer,Integer>[] segments, String[] keys) {
		super(keys.length);
		this.segments = segments;
		this.keys = keys;
	}
	
	/**
	 * Segments specifies the start (inclusive) and stop (exclusive) position within the header
	 * @param segments
	 * @param keys
	 */
	public DefaultFastaHeaderParser(char splitChar, String[] keys) {
		super(keys.length);
		this.splitChar = splitChar;
		this.keys = keys;
	}
	
	/**
	 * Segments specifies the start (inclusive) and stop (exclusive) position within the header
	 * @param segments
	 * @param keys
	 */
	public DefaultFastaHeaderParser(char splitChar) {
		super(-1);
		this.splitChar = splitChar;
	}
	
	@Override
	public String getId(String header) {
		if (splitChar!=null) {
			String[] splitted = StringUtils.split(header.substring(1), splitChar);
			if (splitted.length==0) return "";
			return splitted[0];
		}
		else if (segments!=null) {
			return header.substring(segments[0].Item1,segments[0].Item2);
		} else if (pattern!=null){
			Matcher m = pattern.matcher(header);
			if (m.find())
				return m.group(1);
		}
		return header.substring(1);
	}

	@Override
	public Map<String, String> parseHeader(String header) {
		Map<String,String> re = getMap();

		if (splitChar!=null) {
			String[] splitted = StringUtils.split(header.substring(1), splitChar);
			for (int i=0; i<keys.length; i++) 
				re.put(keys==null?i+"":keys[i], splitted.length>=i?splitted[i]:null);
		}
		else if (segments!=null) {
			for (int i=0; i<keys.length; i++) 
				re.put(keys[i], header.substring(segments[i].Item1,segments[i].Item2));
		} else if (pattern!=null) {
			Matcher m = pattern.matcher(header);
			if (m.find())
				for (int i=0; i<keys.length; i++) 
					if (keys==null || keys[i]!=null)
						re.put(keys==null?i+"":keys[i], m.group(i+1));
		} else {
			re.put("header", header);
		}
				
		return re;
	}
	
	
	private static DefaultFastaHeaderParser instance = null;
	public static DefaultFastaHeaderParser instance() {
		if (instance==null) instance = new DefaultFastaHeaderParser();
		return instance;
	}
	
}
