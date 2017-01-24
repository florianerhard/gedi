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

import gedi.core.region.GenomicRegion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

public class CompositeSequenceProvider implements SequenceProvider {

	private HashMap<String,SequenceProvider> map = new HashMap<String, SequenceProvider>();
	private ArrayList<SequenceProvider> providers = new ArrayList<SequenceProvider>();

	public CompositeSequenceProvider add(SequenceProvider provider) {
		providers.add(provider);
		for (String seq : provider.getSequenceNames())
			map.put(seq, provider);
		return this;
	}
	
	@Override
	public CharSequence getPlusSequence(String name, GenomicRegion region) {
		SequenceProvider seq = map.get(name);
		if (seq==null) return null;
		return seq.getPlusSequence(name, region);
	}

	
	@Override
	public char getPlusSequence(String name, int pos) {
		SequenceProvider seq = map.get(name);
		if (seq==null) return '\0';
		return seq.getPlusSequence(name, pos);
	}


	@Override
	public Set<String> getSequenceNames() {
		return Collections.unmodifiableSet(map.keySet());
	}

	@Override
	public int getLength(String name) {
		SequenceProvider p = map.get(name);
		if (p==null) return -1;
		return p.getLength(name);
	}
	
	public ArrayList<SequenceProvider> getProviders() {
		return providers;
	}
	
}
