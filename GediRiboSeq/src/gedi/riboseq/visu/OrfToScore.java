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

package gedi.riboseq.visu;


import gedi.core.data.mapper.GenomicRegionDataMapper;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.GenomicRegion;
import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelBlockToValuesMap;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.riboseq.inference.orf.Orf;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;

import java.util.Iterator;
import java.util.Map.Entry;

public abstract class OrfToScore implements GenomicRegionDataMapper<IntervalTree<GenomicRegion,Orf>, PixelBlockToValuesMap>{

	private Strand strand;
	
	public OrfToScore(Strand strand) {
		this.strand = strand;
	}
	
	@Override
	public PixelBlockToValuesMap map(ReferenceSequence reference,
			GenomicRegion region,PixelLocationMapping pixelMapping,
			IntervalTree<GenomicRegion, Orf> data) {
		if (data.isEmpty()) return new PixelBlockToValuesMap();
		
		MutableReferenceGenomicRegion<Void> rgr = new MutableReferenceGenomicRegion<Void>().setReference(reference.toStrand(strand));
		
		PixelBlockToValuesMap re = new PixelBlockToValuesMap(pixelMapping, data.size(), Double.NaN);

		Iterator<Entry<GenomicRegion, Orf>> it = data.entrySet().iterator();
		int ele = 0;
		while (it.hasNext()) {
			Entry<GenomicRegion, Orf> e = it.next();
			
			rgr.setRegion(e.getKey());
			Orf orf = e.getValue();

			for (int i=0; i<rgr.getRegion().getTotalLength()-3; i++){
				int bp = rgr.map(i);
				int block = pixelMapping.getBlockForBp(reference, bp);
				
				if (block>=0 && block<re.size()) {
					NumericArray vals = re.getValues(block);
					vals.set(ele, Double.isNaN(vals.getDouble(ele))?getScore(orf,i/3):Math.max(vals.getDouble(ele),getScore(orf,i/3)));
				}
			}
		}
		
		return re;
	}

	protected abstract double getScore(Orf orf, int p);



}
