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
	
}
