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
package gedi.core.genomic;

import gedi.core.data.annotation.Transcript;
import gedi.core.region.ImmutableReferenceGenomicRegion;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.datastructure.tree.Trie;
import java.util.function.Function;

public class NameIndex {

	private Genomic genomic;
	private Trie<ReferenceGenomicRegion<?>> trie;
	
	public NameIndex(String a) {} // dummy constructor for compatibility reasons
	
	
	public NameIndex(Genomic g) {
		this.genomic = g;
	}
	
	
	public Trie<ReferenceGenomicRegion<?>> getIndex() {
		if (trie==null)
			synchronized (this) {
				if (trie==null) {
					trie = new Trie<>();
					Function<String,String> fun = genomic.getGeneTableColumns().contains("symbol")?genomic.getGeneTable("symbol"):null;
					for (ImmutableReferenceGenomicRegion<String> g : genomic.getGenes().ei().loop()) {
						trie.put(g.getData(), g);
						if (fun!=null && fun.apply(g.getData())!=null)
							trie.put(fun.apply(g.getData()), g);
					}
					for (ImmutableReferenceGenomicRegion<Transcript> t : genomic.getTranscripts().ei().loop()) {
						trie.put(t.getData().getTranscriptId(), t);
					}
				}
			}
		return trie;
	}


	
}
