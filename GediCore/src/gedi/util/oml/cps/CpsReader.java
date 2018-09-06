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
package gedi.util.oml.cps;

import gedi.app.Config;
import gedi.app.classpath.ClassPathCache;
import gedi.util.StringUtils;
import gedi.util.dynamic.DynamicObject;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.jhp.Jhp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;



public class CpsReader {

	private static final Logger log = Logger.getLogger( CpsReader.class.getName() );
	
	
	
	private ClassPathCache classCache = ClassPathCache.getInstance();
	
	private CpsList list = new CpsList();
	
	public CpsReader() {
	}
	
	public CpsReader(boolean loadDefaults) throws IOException {
		if (loadDefaults) {
			for (File f : Config.getInstance().getFiles(f->f.getName().endsWith(".cps"))) 
				parse(f);
		}
	}
	
	public CpsList getList() {
		return list;
	}
	
	
	public CpsList parse(String cpsContent) {
		return parse(new StringReader(cpsContent));
	}
	
	public CpsList parse(File file) throws IOException {
		if (file.getPath().endsWith("jhp")) {
			String src = new LineOrientedFile(file.getPath()).readAllText();
			src = new Jhp().apply(src);
			new LineOrientedFile(file.getPath()+".processed").writeAllText(src);
			return parse(src);
		}
		return parse(new FileReader(file));
	}
	
	public CpsList parse(InputStream stream) {
		return parse(new InputStreamReader(stream));
	}
	
	public CpsList parse(Reader reader) {
		
		BufferedCountReader br = new BufferedCountReader(reader);
		CpsList re = list;
		
		try {
			StringBuilder key = new StringBuilder();
			StringBuilder value = new StringBuilder();
			int c;
			
			for (;;) {
				int startOffset = br.getCount();
				
				// identify the key
				while ((c=br.read())!=-1 && c!='{')
					key.append((char)c);
				if (c==-1 && StringUtils.trim(key.toString(),' ','\t','\n').length()==0) break;
				
				if (c==-1) throw new ParseException("EOF before value!",br.getCount());
				
				// key is everything before { 
				// c = {
				
				value.append((char)c);
				
				// find matching } respecting " and '
				boolean inSingleQuote = false;
				boolean inDoubleQuote = false;
				int depth = 1;
				while (depth>0 && (c=br.read())!=-1) {
					
					value.append((char)c);
					
					if (inSingleQuote && c=='\'')
						inSingleQuote = false;
					else if (inDoubleQuote && c=='"')
						inDoubleQuote = false;
					else if (!inSingleQuote && !inDoubleQuote) {
						if (c=='\'')
							inSingleQuote = true;
						else if (c=='"')
							inDoubleQuote = true;
						else if (c=='{')
							depth++;
						else if (c=='}')
							depth--;
					}
					
				}
				if (c==-1) throw new ParseException("EOF before matching brackets found!",br.getCount());
				
				parseEntry(re, key.toString(),value.toString(), startOffset);
				key.delete(0, key.length());
				value.delete(0, value.length());
			}
			
			return re;
			
		} catch (IOException | ParseException e) {
			throw new RuntimeException("Could not parse cps!",e);
		} finally {
			try {
				br.close();
			} catch (IOException e) {
			}
		}
		
	}
	
	private void parseEntry(CpsList re, String key, String value, int offset) throws ParseException {
		log.log(Level.CONFIG,"Parsing entry at "+offset+": Key="+key+" Value="+value);
		String[] keys = StringUtils.trimAll(StringUtils.split(key, ','), '\t','\n',' ');
		DynamicObject obj = DynamicObject.parseJson(value);
		for (String k : keys) 
			re.add(parseKey(k,offset),obj);
	}

	private CpsKey parseKey(String key, int offset) throws ParseException {
		ArrayList<String> parts = new ArrayList<String>();
		int start = 0;
		for (int i=0; i<key.length(); i++) {
			if (key.charAt(i)==' ' || key.charAt(i)=='\t' || key.charAt(i)=='\n') {
				if (i>start)
					parts.add(key.substring(start, i));
				start = i+1;
			} else if ((key.charAt(i)=='#' || key.charAt(i)=='.') && (i==0||key.charAt(i-1)==' '||key.charAt(i-1)=='\n'||key.charAt(i-1)=='\t')) {
				if (i>start)
					parts.add(key.substring(start, i));
				start = i;
			}
		}
		if (start<key.length())
			parts.add(key.substring(start, key.length()));
		
		String id = null;
		HashSet<String> classes = new HashSet<String>();
		Class<?> cls = null;
		for (String s : parts) {
			if (s.startsWith(".")) {
				classes.add(s.substring(1));
			}
			else if (s.startsWith("#")) {
				if (id!=null) throw new ParseException("Only a single id allowed for "+key, offset);
				id = s.substring(1);
			}
			else {
				if (cls!=null) throw new ParseException("Only a single java class allowed for "+key, offset);
				cls = classCache.getClass(s);
			}
		}
		
		return new CpsKey(id, classes, cls);
	}

	private static class BufferedCountReader extends BufferedReader {
		public BufferedCountReader(Reader in) {
			super(in);
		}

		int c = 0;
		@Override
		public int read() throws IOException {
			c++;
			return super.read();
		}
		
		public int getCount() {
			return c;
		}
	}
	
	
	
}
