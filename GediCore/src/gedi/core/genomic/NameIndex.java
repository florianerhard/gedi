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

import gedi.core.region.ReferenceGenomicRegion;
import gedi.util.datastructure.tree.Trie;
import gedi.util.orm.Orm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class NameIndex {

	private ArrayList<String> paths = new ArrayList<String>();
	private Trie<ReferenceGenomicRegion<?>> trie;
	
	
	public NameIndex(String... path) {
		this.paths.addAll(Arrays.asList(path));
	}
	
	
	public Trie<ReferenceGenomicRegion<?>> getIndex() {
		if (trie==null)
			try {
				for (String path : paths) 
					if (trie==null)
						trie = Orm.deserialize(path);
					else
						trie.putAll(Orm.deserialize(path));
			} catch (IOException e) {
				throw new RuntimeException("Could not deserialize name index!",e);
			}
		return trie;
	}


	public void merge(NameIndex other) {
		trie = null;
		paths.addAll(other.paths);
	}
	
	public NameIndex clone() {
		return new NameIndex(paths.toArray(new String[0]));
	}
	
}
