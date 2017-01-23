package gedi.util.io.text.tsv.formats;

import gedi.util.ArrayUtils;
import gedi.util.StringUtils;
import gedi.util.datastructure.charsequence.MaskedCharSequence;
import gedi.util.io.text.HeaderLine;
import gedi.util.io.text.LineIterator;
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
import java.util.zip.GZIPInputStream;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
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
		
		for (int i=0; i<skipLines.get(); i++)
			it.next();
		
		// first, infer the separator!
		String[] inferLines = new String[this.inferLines.get()];
		int index = 0;
		while (it.hasNext() && index<inferLines.length) 
			inferLines[index++] = it.next();
		
		inferLines = ArrayUtils.redimPreserve(inferLines, index);
		
		if (index==0) return null;
		
		boolean maskQuotes = this.maskQuotes.get()==null?inferMaskQuotes(inferLines,index):this.maskQuotes.get();
				
		char sep = separator.get()==null?inferSeparator(inferLines,index, maskQuotes):separator.get();
		
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
		
		
		Parser[] p = new Parser[types.length];
		for (int i=0; i<p.length; i++)
			p[i] = parsers[types[i]];
		
		boolean hasHeader = header.get()==null?(minHeaderType==parsers.length-1 || oneGreater):header.get();
		
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


	private ObjectProperty<Boolean> header = new SimpleObjectProperty<Boolean>(this, "header", null);
	private IntegerProperty inferLines = new SimpleIntegerProperty(this,"inferLines",1000);
	private ObjectProperty<Boolean> maskQuotes = new SimpleObjectProperty<Boolean>(this,"maskQuotes",null);
	private IntegerProperty skipLines = new SimpleIntegerProperty(this,"skipLines",0);
	private ObjectProperty<Character> separator = new SimpleObjectProperty<Character>(this,"separator",null);
	private ObjectProperty<Parser[]> parsers = new  SimpleObjectProperty<Parser[]>(this,"parsers",DEFAULT_PARSERS);
	
	
	
	
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

	public final ObjectProperty<Boolean> headerProperty() {
		return this.header;
	}

	public final java.lang.Boolean getHeader() {
		return this.headerProperty().get();
	}

	public final void setHeader(final java.lang.Boolean header) {
		this.headerProperty().set(header);
	}

	public final IntegerProperty inferLinesProperty() {
		return this.inferLines;
	}

	public final int getInferLines() {
		return this.inferLinesProperty().get();
	}

	public final void setInferLines(final int inferLines) {
		this.inferLinesProperty().set(inferLines);
	}

	public final IntegerProperty skipLinesProperty() {
		return this.skipLines;
	}

	public final int getSkipLines() {
		return this.skipLinesProperty().get();
	}

	public final void setSkipLines(final int skipLines) {
		this.skipLinesProperty().set(skipLines);
	}

	public final ObjectProperty<Character> separatorProperty() {
		return this.separator;
	}

	public final java.lang.Character getSeparator() {
		return this.separatorProperty().get();
	}

	public final void setSeparator(final java.lang.Character separator) {
		this.separatorProperty().set(separator);
	}
	
	public final ObjectProperty<Parser[]> parsersProperty() {
		return this.parsers;
	}

	public final Parser[] getParsers() {
		return this.parsersProperty().get();
	}

	public final void setParsers(final Parser[] parsers) {
		this.parsersProperty().set(parsers);
	}

	public final ObjectProperty<Boolean> maskQuotesProperty() {
		return this.maskQuotes;
	}

	public final java.lang.Boolean getMaskQuotes() {
		return this.maskQuotesProperty().get();
	}

	public final void setMaskQuotes(final java.lang.Boolean maskQuotes) {
		this.maskQuotesProperty().set(maskQuotes);
	}

	
	
}
