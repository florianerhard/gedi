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


import gedi.util.FunctorUtils;
import gedi.util.io.text.LineOrientedFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Function;


/**
 * The transformer gets an array, its 0 element is the header (w/o the >), and then successively all lines of the entry
 * @author erhard
 *
 * @param <T>
 */
public class FastaLikeFile<T> extends LineOrientedFile {

	private Class<T> cls;
	private T[] entries;
	private Function<String[],T> parser;
	
	public FastaLikeFile(File dir, String name, Function<String[],T> parser, Class<T> cls) {
		super(dir,name);
		this.parser = parser;
		this.cls = cls;
	}
	
	public FastaLikeFile(String path, Function<String[],T> parser, Class<T> cls) {
		super(path);
		this.parser = parser;
		this.cls = cls;
	}
	
	
	@Override
	public void loadIntoMemory() throws IOException {
		entries = FunctorUtils.toArray(entryIterator(),cls);
	}
	
	public void writeEntry(T fe) throws IOException {
		writeLine(fe.toString());
	}
	
	
	public Iterator<T> entryIterator() throws IOException {
		if (entries==null)
			return new EntryIterator(createReader());
		else
			return FunctorUtils.arrayIterator(entries);
	}
	
	private class EntryIterator implements Iterator<T> {

		private T entry;
		
		private T next;
		
		private BufferedReader br;
		private String header;
		ArrayList<String>  seq = new ArrayList<String>();
		
		public EntryIterator(Reader reader) throws IOException {
			br = new BufferedReader(reader);
			header = br.readLine();
		}

		@Override
		public boolean hasNext() {
			lookAhead();
			return next!=null;
		}

		@Override
		public T next() {
			lookAhead();
			T re = next;
			next = null;
			return re;
		}

		private void lookAhead() {
			if (next==null && header!=null) {
				String line = null;
				try {
					while ((line=br.readLine())!=null && !line.startsWith(">"))
						seq.add(line);
					
				} catch (IOException e) {
					throw new RuntimeException("Could not read file!",e);
				}
				
				String[] a = new String[seq.size()+1];
				a[0] = header.substring(1);
				for (int i=0; i<seq.size(); i++)
					a[i+1] = seq.get(i);
				
				next = parser.apply(a);
				header = line;
				
				seq.clear();
			}
		}
		
		@Override
		public void remove() {}
		
	}

	

}
