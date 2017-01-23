package gedi.util.io.text.tsv.formats;

import gedi.util.datastructure.dataframe.DataFrame;
import gedi.util.parsing.BooleanParser;
import gedi.util.parsing.DoubleParser;
import gedi.util.parsing.IntegerParser;
import gedi.util.parsing.Parser;
import gedi.util.parsing.StringParser;
import gedi.util.userInteraction.progress.Progress;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class Csv {
	
	public static DataFrame toDataFrame(String path) {
		return toDataFrame(path, null, 0, null);
	}
	
	public static DataFrame toDataFrame(String path, Progress progress) {
		return toDataFrame(path, null, 0, progress);
	}
	
	public static DataFrame toDataFrame(String path, Boolean header, int skip, Progress progress) {
			CsvReaderFactory fac = new CsvReaderFactory();
		fac.setParsers(new Parser[] {
				new BooleanParser(),
				new IntegerParser(),
				new DoubleParser(),
				new StringParser()
		});
		fac.setSkipLines(skip);
		fac.setHeader(header);
		CsvReader reader = fac.createReader(path);
		if (progress!=null) reader.setProgress(progress);
		
		return reader.readDataFrame();
		
	}
	
	public static DataFrame toDataFrame(URL path) throws IOException {
		return toDataFrame(path, null, true, 0, null);
	}
	
	public static DataFrame toDataFrame(URL path, Progress progress) throws IOException {
		return toDataFrame(path, null, true, 0, progress);
	}
	
	public static DataFrame toDataFrame(URL path, Boolean header, boolean stringsAsFactors, int skip, Progress progress) throws IOException {
			CsvReaderFactory fac = new CsvReaderFactory();
		fac.setParsers(new Parser[] {
				new BooleanParser(),
				new IntegerParser(),
				new DoubleParser(),
				new StringParser()
		});
		fac.setSkipLines(skip);
		fac.setHeader(header);
		try (InputStream str = path.openStream()) {
			CsvReader reader = fac.createReader(str);
			if (progress!=null) reader.setProgress(progress);
			
			return reader.readDataFrame();
		}
	}

}
