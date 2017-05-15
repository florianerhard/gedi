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

package gedi.core.sequence;

import gedi.core.reference.Chromosome;
import gedi.core.region.GenomicRegion;
import gedi.util.SequenceUtils;
import gedi.util.StringUtils;
import gedi.util.io.text.fasta.index.FastaIndexFile;
import gedi.util.io.text.fasta.index.FastaIndexFile.FastaIndexEntry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

public class FastaIndexSequenceProvider implements SequenceProvider {

	
	private HashMap<String,FastaIndexEntry> index = new HashMap<String, FastaIndexEntry>();
	private ArrayList<FastaIndexFile> files = new ArrayList<FastaIndexFile>();
	
	
	public FastaIndexSequenceProvider(String... paths) throws IOException {
		for (String f : paths)
			addFastaIndex(f);
	}
	public FastaIndexSequenceProvider(FastaIndexFile... files) {
		for (FastaIndexFile f : files)
			addFastaIndex(f);
	}
	
	public void addFastaIndex(String path) throws IOException {
		addFastaIndex(new FastaIndexFile(path).open());
	}
	
	public void addFastaIndex(FastaIndexFile f) {
		files.add(f);
		for (String n : f.getEntryNames()) {
//			System.out.println(n+"\t"+f.getEntry(n).length());
			index.put(Chromosome.obtain(n).getName(), f.getEntry(n));
		}
	}
	
	private FastaIndexEntry getEntry(String name) {
		name = Chromosome.obtain(name).getName();
		FastaIndexEntry re = index.get(name);
//		if (re==null && name.indexOf(' ')!=-1)
//			re = index.get(name.substring(0,name.indexOf(' ')));
//		if (re==null && index.containsKey("chr"+name))
//			re = index.get("chr"+name);
//		if (re==null && index.containsKey(StringUtils.removeHeader(name, "chr")))
//			re = index.get(StringUtils.removeHeader(name, "chr"));
		return re;
	}
	
	public void close() throws IOException {
		for (FastaIndexFile f : files)
			f.close();
	}

	@Override
	public int getLength(String name) {
		FastaIndexEntry entry = getEntry(name);
		if (entry==null) return -1;
		return entry.length();
	}
	
	@Override
	public CharSequence getPlusSequence(String name, GenomicRegion region) {
		FastaIndexEntry entry = getEntry(name);
		if (entry==null) return null;
		if (region.getTotalLength()==0) return "";
		if (region.getStart()<0 || region.getEnd()>entry.length()) 
			throw new IndexOutOfBoundsException(name+" sequence length: "+entry.length()+" - "+region.toRegionString());
		
		try {
			return SequenceUtils.extractSequence(region, entry);
		} catch (IOException e) {
			throw new RuntimeException("Could not read sequence from file "+entry.getFile()+"!",e);
		}
	}
	
	@Override
	public char getPlusSequence(String name, int pos) {
		FastaIndexEntry entry = getEntry(name);
		if (entry==null) return '\0';
		if (pos<0 || pos+1>entry.length()) throw new IndexOutOfBoundsException();
		
		try {
			return entry.getSequence(pos, pos+1).charAt(0);
		} catch (IOException e) {
			throw new RuntimeException("Could not read sequence from file "+entry.getFile()+"!",e);
		}
	}
	
	
	@Override
	public Set<String> getSequenceNames() {
		return Collections.unmodifiableSet(index.keySet());
	}
	
}
