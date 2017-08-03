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
import gedi.riboseq.inference.orf.PriceOrf;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;

import java.util.Iterator;
import java.util.Map.Entry;

public abstract class PriceOrfToScore implements GenomicRegionDataMapper<IntervalTree<GenomicRegion,PriceOrf>, PixelBlockToValuesMap>{

	private Strand strand;
	
	public PriceOrfToScore(Strand strand) {
		this.strand = strand;
	}
	
	@Override
	public PixelBlockToValuesMap map(ReferenceSequence reference,
			GenomicRegion region,PixelLocationMapping pixelMapping,
			IntervalTree<GenomicRegion, PriceOrf> data) {
		if (data.isEmpty()) return new PixelBlockToValuesMap();
		
		MutableReferenceGenomicRegion<Void> rgr = new MutableReferenceGenomicRegion<Void>().setReference(reference.toStrand(strand));
		
		int rows = getRows(data.values().iterator().next());
		PixelBlockToValuesMap re = new PixelBlockToValuesMap(pixelMapping, rows, Double.NaN);

		Iterator<Entry<GenomicRegion, PriceOrf>> it = data.entrySet().iterator();
		while (it.hasNext()) {
			Entry<GenomicRegion, PriceOrf> e = it.next();
			
			rgr.setRegion(e.getKey());
			PriceOrf orf = e.getValue();

			for (int i=0; i<rgr.getRegion().getTotalLength()-3; i++){
				int bp = rgr.map(i);
				int block = pixelMapping.getBlockForBp(reference, bp);
				
				if (block>=0 && block<re.size()) {
					NumericArray vals = re.getValues(block);
					for (int r=0; r<rows; r++)
						vals.set(r, Double.isNaN(vals.getDouble(r))?getScore(orf,i/3,r):Math.max(vals.getDouble(r),getScore(orf,i/3,r)));
				}
			}
		}
		
		return re;
	}

	protected double getScore(PriceOrf orf, int p, int row){
		return getScore(orf,p);
	}
	protected abstract double getScore(PriceOrf orf, int p);
	protected int getRows(PriceOrf orf){
		return 1;
	}



}
