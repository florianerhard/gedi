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
package gedi.util.io.text.tsv.formats;

import gedi.util.ArrayUtils;
import gedi.util.ReflectionUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.charsequence.MaskedCharSequence;
import gedi.util.io.text.HeaderLine;
import gedi.util.io.text.LineIterator;
import gedi.util.orm.Orm;
import gedi.util.orm.Orm.OrmInfo;
import gedi.util.parsing.BooleanParser;
import gedi.util.parsing.DoubleParser;
import gedi.util.parsing.GenomicRegionParser;
import gedi.util.parsing.IntegerParser;
import gedi.util.parsing.LongParser;
import gedi.util.parsing.Parser;
import gedi.util.parsing.QuotedStringParser;
import gedi.util.parsing.ReferenceGenomicRegionParser;
import gedi.util.parsing.StringParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import cern.colt.bitvector.BitVector;

public class CsvReaderFactory  {
	
	public CsvReader createReader(String path) {
		return createReader(Paths.get(new File(path).toURI()));
	}
	
	public CsvReader createReader(Path path) {
		try {
			if (path.toString().endsWith(".gz"))
				return createReader(path.getFileName().toString(),new GZIPInputStream(Files.newInputStream(path)));
			return createReader(path.getFileName().toString(),Files.newInputStream(path));
		} catch (IOException e) {
			throw new RuntimeException("Could not read CSV file "+path, e);
		}
	}
	
	
	public CsvReader createReader(InputStream stream) {
		return createReader(new LineIterator(stream, "#"));
	}
	public CsvReader createReader(LineIterator it) {
		return createReader(StringUtils.createRandomIdentifier(10),it);
	}
	public CsvReader createReader(String name, InputStream stream) {
		return createReader(name,new LineIterator(stream, "#"));
	}
	public CsvReader createReader(String name, LineIterator it) {
		
		for (int i=0; i<skipLines; i++)
			it.next();
		
		// first, infer the separator!
		String[] inferLines = new String[this.inferLines];
		int index = 0;
		while (it.hasNext() && index<inferLines.length) 
			inferLines[index++] = it.next();
		
		inferLines = ArrayUtils.redimPreserve(inferLines, index);
		
		if (index==0) return null;
		
		boolean maskQuotes = this.maskQuotes==null?inferMaskQuotes(inferLines,index):this.maskQuotes;
				
		char sep = separator==null?inferSeparator(inferLines,index, maskQuotes):separator;
		
		String[][] fields = new String[index][];
		for (int i=0; i<fields.length;i++) 
			fields[i] = maskQuotes?MaskedCharSequence.maskQuotes(inferLines[i], '\0').splitAndUnmask(sep):StringUtils.split(inferLines[i], sep);//StringUtils.splitQuoted(inferLines[i], sep);
		
		int cols = fields[0].length;
		maskQuotes = maskQuotes?inferMaskQuotes2(fields):false;
		
		// infer types
		int[] types = new int[cols];
		for (int i=0; i<cols; i++) 
			types[i] = inferType(fields,1,index,i); // start with 1 due to potential header!
		
		Parser[] parsers = getParsers();
		
		int minHeaderType = parsers.length-1;
		boolean oneGreater = false;
		for (int i=0; i<cols; i++) {
			int ht = inferType(fields,0,1,i);
			minHeaderType = Math.min(ht,minHeaderType);
			oneGreater |= ht>types[i];
		}
		
		
		boolean hasHeader = header==null?(minHeaderType==parsers.length-1 || oneGreater):header;
		
		String[] headerNames = new String[cols];
		if (hasHeader) {
			System.arraycopy(fields[0], 0, headerNames, 0, headerNames.length);
		} else {
			for (int i=0; i<headerNames.length; i++) 
				headerNames[i] = "C"+StringUtils.padLeft(i+1+"", 1+(int)Math.log10(headerNames.length), '0');
		}
		
		if (hasHeader) {
			if (fields.length>0)
				fields = java.util.Arrays.copyOfRange(fields, 1, fields.length);
			else 
				it.next();
		}
		
		
		// setup parseToObject
		if (getParseToClass()!=null) {
			OrmInfo info = Orm.getInfo(getParseToClass());
			for (int i=0; i<info.getNames().length; i++)
				setType(info.getNames()[i],info.getClasses()[i]);
		}
		
		Parser[] p = new Parser[types.length];
		for (int i=0; i<p.length; i++) {
			if (colToParser.get(i)!=null)
				p[i] = colToParser.get(i);
			else if (nameToParser.get(headerNames[i])!=null)
				p[i] = nameToParser.get(headerNames[i]);
			else
				p[i] = parsers[types[i]];
		}
		
		
		return new CsvReader(name, fields,it,sep,maskQuotes, new HeaderLine(headerNames),p);
		
	}

	
	private boolean inferMaskQuotes(String[] inferLines, int n) {
		for (int i=1; i<n; i++) 
			if (!MaskedCharSequence.canMaskQuotes(inferLines[i]))
				return false;
		return true;
	}
	
	private boolean inferMaskQuotes2(String[][] fields) {
		BitVector bv = new BitVector(fields[0].length);
		for (int i=1; i<fields.length; i++) 
			for (int j=0; j<fields[i].length; j++)
				if (!(fields[i][j].startsWith("'") && fields[i][j].endsWith("'")) && !(fields[i][j].startsWith("\"") && fields[i][j].endsWith("\"")))
					bv.putQuick(j, false);
					
		return bv.cardinality()>0;
	}


	private Boolean header = null;
	private int inferLines = 1000;
	private Boolean maskQuotes = null;
	private int skipLines = 0;
	private Character separator = null;
	private Parser[] parsers = DEFAULT_PARSERS;
	private Class<?> parseToClass = null;
	
	private HashMap<String,Parser> nameToParser = new HashMap<>();
	private HashMap<Integer,Parser> colToParser = new HashMap<>();
	
	
	public void setType(int col, Parser parser) {
		this.colToParser.put(col, parser);
	}
	public void setType(String name, Parser parser) {
		this.nameToParser.put(name, parser);
	}
	
	public void setType(int col, Class<?> type) {
		Parser[] parsers = getParsers();
		for (int i=0; i<parsers.length; i++)
			if (parsers[i].getParsedType()==type || ReflectionUtils.toPrimitveClass(parsers[i].getParsedType())==type) {
				this.colToParser.put(col, parsers[i]);
				return;
			}
		throw new IllegalArgumentException("No parser for "+type);
	}
	public void setType(String name, Class<?> type) {
		Parser[] parsers = getParsers();
		for (int i=0; i<parsers.length; i++)
			if (parsers[i].getParsedType()==type || ReflectionUtils.toPrimitveClass(parsers[i].getParsedType())==type) {
				this.nameToParser.put(name, parsers[i]);
				return;
			}
		throw new IllegalArgumentException("No parser for "+type);
	}
	
	
	public static final char[] separators = {'\t',';',',',' ','|','#','&'};
//	private static final Class[] classOrder = {boolean.class,int.class,long.class,double.class,MutableReferenceGenomicRegion.class,GenomicRegion.class,String.class,String.class};
	private static final Parser[] DEFAULT_PARSERS = {new BooleanParser(),new IntegerParser(),new LongParser(),new DoubleParser(), new ReferenceGenomicRegionParser<>(), new GenomicRegionParser(), new QuotedStringParser(), new StringParser()};
	private int inferType(String[][] fields, int start, int end, int c) {
		int p = 0;
		Parser[] parsers = getParsers();
		for (int i=start; i<end; i++) {
			while (!parsers[p].canParse(fields[i][c]))
				p++;
			if (p==parsers.length-1)
				return p;
		}
		return p;
	}
	

	public Boolean getHeader() {
		return header;
	}

	public CsvReaderFactory setHeader(Boolean header) {
		this.header = header;
		return this;
	}

	public int getInferLines() {
		return inferLines;
	}

	public CsvReaderFactory setInferLines(int inferLines) {
		this.inferLines = inferLines;
		return this;
	}

	public Boolean getMaskQuotes() {
		return maskQuotes;
	}

	public CsvReaderFactory setMaskQuotes(Boolean maskQuotes) {
		this.maskQuotes = maskQuotes;
		return this;
	}

	public int getSkipLines() {
		return skipLines;
	}

	public CsvReaderFactory setSkipLines(int skipLines) {
		this.skipLines = skipLines;
		return this;
	}

	public Character getSeparator() {
		return separator;
	}

	public CsvReaderFactory setSeparator(Character separator) {
		this.separator = separator;
		return this;
	}

	public Parser[] getParsers() {
		return parsers;
	}

	public CsvReaderFactory setParsers(Parser[] parsers) {
		this.parsers = parsers;
		return this;
	}

	public Class<?> getParseToClass() {
		return parseToClass;
	}

	public CsvReaderFactory setParseToClass(Class<?> parseToClass) {
		this.parseToClass = parseToClass;
		return this;
	}

	private char inferSeparator(String[] lines, int n, boolean maskQuotes) {
		if (n==0) return separators[0];
		int[] c = new int[separators.length];
		for (int i=0; i<separators.length; i++) {
			char sep = separators[i];
			c[i] = StringUtils.countChar(maskQuotes?MaskedCharSequence.maskQuotes(lines[0],'\0'):lines[0], sep);
			for (int j=1; j<n; j++) {
				int tc = StringUtils.countChar(maskQuotes?MaskedCharSequence.maskQuotes(lines[j],'\0'):lines[j], sep);
				if (tc!=c[i]) {
					c[i]=-1;
					break;
				}
			}
		}
		
		for (int i=0; i<c.length; i++) 
			if (c[i]>0) return separators[i];
		
		for (int i=0; i<c.length; i++) 
			if (c[i]==0) return separators[i];
		
		throw new RuntimeException("Cannot determine separator!");
	}


	
	
}
