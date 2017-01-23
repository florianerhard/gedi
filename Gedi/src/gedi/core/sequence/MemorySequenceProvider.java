package gedi.core.sequence;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import gedi.core.region.GenomicRegion;
import gedi.util.SequenceUtils;

public class MemorySequenceProvider implements SequenceProvider {

	private HashMap<String,CharSequence> map = new HashMap<String, CharSequence>();


	public MemorySequenceProvider add(String name, CharSequence sequence) {
		map.put(name,sequence);
		return this;
	}
	
	@Override
	public CharSequence getPlusSequence(String name, GenomicRegion region) {
		CharSequence seq = map.get(name);
		if (seq==null) return null;
		if (region.getStart()<0 || region.getEnd()>seq.length()) throw new IndexOutOfBoundsException();
		
		return SequenceUtils.extractSequence(region, seq);
	}
	@Override
	public char getPlusSequence(String name, int pos) {
		CharSequence seq = map.get(name);
		if (seq==null) return '\0';
		if (pos<0 || pos+1>seq.length()) throw new IndexOutOfBoundsException();
		
		return seq.charAt(pos);
	}
	
	
	@Override
	public int getLength(String name) {
		CharSequence seq = map.get(name);
		if (seq==null) return -1;
		return seq.length();
	}

	@Override
	public Set<String> getSequenceNames() {
		return Collections.unmodifiableSet(map.keySet());
	}

	
}
