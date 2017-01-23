package gedi.util.io.text;

import gedi.util.StringUtils;

import java.io.IOException;
import java.util.Locale;

public interface LineWriter extends AutoCloseable {

	
	default LineWriter writeLine() throws IOException {
		writeLine("");
		return this;
	}
	default LineWriter writeTsv(Object... val) throws IOException {
		if (val.length>0) {
			write(StringUtils.toString(val[0]));
			for (int i=1; i<val.length; i++) {
				write("\t");
				write(StringUtils.toString(val[i]));
			}
		}
		write("\n");
		return this;
	}
	default LineWriter writeLine(String line) throws IOException {
		write(line);
		write("\n");
		return this;
	}
	default LineWriter writef(String line, Object...args) throws IOException {
		write(String.format(Locale.US,line,args));
		return this;
	}
	void write(String line) throws IOException;
	
	default LineWriter writeLine2() {
		try {
			writeLine("");
			return this;
		} catch (IOException e) {
			throw new RuntimeException("Could not write!",e);
		}
	}
	default LineWriter writeTsv2(Object... val) {
		try {
			if (val.length>0) {
				write(StringUtils.toString(val[0]));
				for (int i=1; i<val.length; i++) {
					write("\t");
					write(StringUtils.toString(val[i]));
				}
			}
			write("\n");
			return this;
		} catch (IOException e) {
			throw new RuntimeException("Could not write!",e);
		}
	}
	default LineWriter writeLine2(String line) {
		try {
			write(line);
			write("\n");
			return this;
		} catch (IOException e) {
			throw new RuntimeException("Could not write!",e);
		}
	}
	default LineWriter writef2(String line, Object...args)  {
		try {
			write(String.format(Locale.US,line,args));
		} catch (IOException e) {
			throw new RuntimeException("Could not write!",e);
		}
		return this;
	}
	default void write2(String line) {
		try {
			write(line);
		} catch (IOException e) {
			throw new RuntimeException("Could not write!",e);
		}
	}
	
	
	void flush() throws IOException;
	void close() throws IOException;
	
	
}
