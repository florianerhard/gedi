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
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;

import java.util.HashMap;
import java.util.function.Function;
import java.util.function.UnaryOperator;

@GenomicRegionDataMapping(fromType=IntervalTree.class,toType=IntervalTree.class)
public class ToGeneMapper implements GenomicRegionDataMapper<IntervalTree<GenomicRegion,Transcript>,IntervalTree<GenomicRegion,String>>{

	private boolean removeIntrons = false;
	private Function<String,String> nameMap = t->t;

	
	public void setRemoveIntrons(boolean removeIntrons) {
		this.removeIntrons = removeIntrons;
	}
	
	public void setNameMap(UnaryOperator<String> nameMap) {
		this.nameMap = nameMap;
	}
	
	public void setSymbol(Genomic g) {
		this.nameMap = g.getGeneTable("symbol");
	}
	
	@Override
	public IntervalTree<GenomicRegion, String> map(ReferenceSequence reference,
			GenomicRegion region, PixelLocationMapping pixelMapping,
			IntervalTree<GenomicRegion, Transcript> data) {
		
		HashMap<String,GenomicRegion> union = new HashMap<String, GenomicRegion>();
		
		for (GenomicRegion r : data.keySet()) {
			String g = data.get(r).getGeneId();
			if (union.containsKey(g))
				union.put(g, union.get(g).union(r));
			else union.put(g,r);
		}
		
		IntervalTree<GenomicRegion,String> re = new IntervalTree<GenomicRegion, String>(reference);
		for (String g : union.keySet()) {
			String n = nameMap==null?g:nameMap.apply(g);
			if (n==null) n=g;
			re.put(removeIntrons?union.get(g).removeIntrons():union.get(g), n);
		}
				
		
		return re;
		
		
	}
	
	

}
