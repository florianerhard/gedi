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


import gedi.util.FileUtils;
import gedi.util.FunctorUtils;
import gedi.util.functions.ExtendedIterator;
import gedi.util.io.text.LineOrientedFile;
import gedi.util.io.text.fasta.index.FastaIndexFile;
import gedi.util.mutable.MutablePair;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;


public class FastaFile extends LineOrientedFile {

	public static class FromNameTransformer implements Function<String, FastaFile> {

		@Override
		public FastaFile apply(String name) {
			return new FastaFile(name);
		}

	}

	private FastaEntry[] entries;
	private FastaHeaderParser headerParser = DefaultFastaHeaderParser.instance();
	
	public FastaFile(File dir, String name) {
		super(dir,name);
	}
	
	public FastaFile(String path) {
		super(path);
	}
	
	public FastaIndexFile obtainDefaultIndex() {
		return new FastaIndexFile(FileUtils.getFullNameWithoutExtension(this)+".fi");
	}
	public FastaIndexFile obtainAndOpenDefaultIndex() throws IOException {
		FastaIndexFile fi = obtainDefaultIndex();
		if (fi.exists())
			return fi.open();
		else
			return fi.create(this);
	}
	
	public long countEntries() throws IOException {
		return FunctorUtils.countIterator(entryIterator(true));
	}
	
	public int getLongestSequenceLength() throws IOException {
		Iterator<FastaEntry> it = entryIterator(true);
		int l = Integer.MIN_VALUE;
		while (it.hasNext())
			l = Math.max(l, it.next().getSequence().length());
		return l;
	}
	
	@Override
	public void loadIntoMemory() throws IOException {
		entries = FunctorUtils.toArray(entryIterator(false),FastaEntry.class);
	}
	
	@Override
	public void finishWriting() throws IOException {
		super.finishWriting();
	}
	public void writeEntry(FastaEntry fe) throws IOException {
		writeLine(fe.toString());
	}
	
	
	public ExtendedIterator<FastaEntry> entryIterator() throws IOException {
		return entryIterator(false);
	}
	public ExtendedIterator<FastaEntry> entryIterator(boolean reUseEntry) throws IOException {
		if (entries==null)
			return new EntryIterator(createReader(),reUseEntry);
		else
			return FunctorUtils.arrayIterator(entries);
	}
	
	public static class EntryIterator implements ExtendedIterator<FastaEntry> {

		private FastaEntry entry;
		private boolean reUse;
		
		private FastaEntry next;
		
		private BufferedReader br;
		private String header;
		
		public EntryIterator(Reader reader, boolean reUse) throws IOException {
			this.reUse = reUse;
			br = new BufferedReader(reader);
			header = br.readLine();
		}

		@Override
		public boolean hasNext() {
			lookAhead();
			return next!=null;
		}

		@Override
		public FastaEntry next() {
			lookAhead();
			FastaEntry re = next;
			next = null;
			return re;
		}

		private void lookAhead() {
			if (next==null && header!=null) {
				next = reUse && entry !=null ? entry : new FastaEntry();
				
				if (!header.startsWith(">")) {
					split(next,header);
					try {
						header = br.readLine();
					} catch (IOException e) {
						throw new RuntimeException("Could not read file!",e);
					}
				} else {
					StringBuilder seq = new StringBuilder();
					String line = null;
					try {
						while ((line=br.readLine())!=null && !line.startsWith(">"))
							seq.append(line);
						
					} catch (IOException e) {
						throw new RuntimeException("Could not read file!",e);
					}
					
					next.setHeader(header);
					next.setSequence(seq.toString());
					header = line;
				}
			}
		}
		
		private void split(FastaEntry e, String line) {
			int index = line.lastIndexOf(':');
			e.setHeader(index>=0 ? line.substring(0,index) : "");
			e.setSequence(line.substring(index+1));
		}

		@Override
		public void remove() {}
		
	}

	public static class ExtractSequenceTransformer implements Function<FastaEntry,String> {

		@Override
		public String apply(FastaEntry e) {
			return e.getSequence();
		}
		
	}

	public FastaHeaderParser getFastaHeaderParser() {
		return headerParser;
	}
	
	public void setFastaHeaderParser(FastaHeaderParser headerParser) {
		this.headerParser = headerParser;
	}

}
