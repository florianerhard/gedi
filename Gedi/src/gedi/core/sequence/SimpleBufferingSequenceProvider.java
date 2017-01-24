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

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.util.SequenceUtils;

public class SimpleBufferingSequenceProvider implements SequenceProvider {


	private SequenceProvider parent;
	private String buffer = "";
	private String name;
	private int start;
	private int l;

	
	public SimpleBufferingSequenceProvider(SequenceProvider parent) {
		this(parent,3000);
	}
	
	public SimpleBufferingSequenceProvider(SequenceProvider parent, int l) {
		this.parent = parent;
		this.l = l;
	}
	@Override
	public CharSequence getPlusSequence(String name, GenomicRegion region) {
		
		if (region.getEnd()-region.getStart()>2*l) return parent.getPlusSequence(name, region);
		
		if (!name.equals(this.name) || region.getStart()<start || region.getEnd()>start+buffer.length()) rebuffer(name,region.getStart()-l,region.getEnd()+l);
		
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<region.getNumParts(); i++)
			sb.append(buffer.substring(region.getStart(i)-start, region.getEnd(i)-start));
		return sb.toString();
	}
	@Override
	public char getPlusSequence(String name, int pos) {
		if (!name.equals(this.name) || pos<start || pos>=start+buffer.length()) rebuffer(name,pos-l,pos+l);
		return buffer.charAt(pos-start);
	}
	
	private void rebuffer(String name, int start, int end) {		
		start = Math.max(start, 0);
		end = Math.min(end,parent.getLength(name));
		buffer = parent.getPlusSequence(name, new ArrayGenomicRegion(start,end)).toString();
		this.start = start;
		this.name = name;
	}

	@Override
	public int getLength(String name) {
		return parent.getLength(name);
	}

	@Override
	public Set<String> getSequenceNames() {
		return parent.getSequenceNames();
	}

	
}
