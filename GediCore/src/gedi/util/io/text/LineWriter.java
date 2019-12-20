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

import gedi.util.FileUtils;
import gedi.util.StringUtils;

import java.io.File;
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
	default void write(char[] a) throws IOException {
		write(String.valueOf(a));
	}
	default void write2(char[] a) {
		write2(String.valueOf(a));
	}
	
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
	
	
	public static LineWriter dummy() {
		return new LineWriter() {
			
			@Override
			public void write(String line) throws IOException {
			}
			
			@Override
			public void flush() throws IOException {
			}
			
			@Override
			public void close() throws IOException {
			}
		};
	}
	
	public static LineWriter tmp(boolean deleteOnExit) throws IOException {
		File f = File.createTempFile("tmp", ".tmp");
		if (deleteOnExit) f.deleteOnExit();
		return new LineOrientedFile(f.getPath()).write();
	}
	
	
}
