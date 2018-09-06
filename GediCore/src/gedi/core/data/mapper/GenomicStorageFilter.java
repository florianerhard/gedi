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
package gedi.core.data.mapper;

import gedi.core.data.annotation.Transcript;
import gedi.core.genomic.Genomic;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;

import java.util.function.Function;

@GenomicRegionDataMapping(fromType=IntervalTree.class,toType=IntervalTree.class)
public class GenomicStorageFilter<D> extends StorageFilter<D >{

	private Genomic genomic;
	
	public GenomicStorageFilter(Genomic genomic) {
		this.genomic = genomic;
	}

	public void addTranscriptTable(String col) {
		addFunction(genomic.getTranscriptTable(col));
	}
	
	public void addGeneTable(String col) {
		addFunction(genomic.getGeneTable(col));
	}
	
	public void addFunction(Function<String, String> map) {
		if (map!=null)
		addOperator(rgr->rgr.toMutable().transformData(d->{
			if (d==null) return (D)map.apply("");
			if (d instanceof Transcript) return (D)map.apply(((Transcript)d).getTranscriptId());
			return (D)map.apply(d.toString());
		}));		
	}

}
