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


import gedi.core.data.mapper.GenomicRegionDataMapping;
import gedi.core.reference.Strand;
import gedi.gui.genovis.pixelMapping.PixelBlockToValuesMap;
import gedi.riboseq.inference.orf.Orf;
import gedi.riboseq.inference.orf.PriceOrf;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;

@GenomicRegionDataMapping(fromType=IntervalTree.class,toType=PixelBlockToValuesMap.class)
public class PriceOrfChangePoint extends PriceOrfToScore {

	int cond;
	public PriceOrfChangePoint(Strand strand, double cond) {
		super(strand);
		this.cond = (int)cond;
	}

	@Override
	protected double getScore(PriceOrf orf, int p) {
		double psum = 0;
		double asum = 0;
		for (int x=p+10; x>=p-10; x--) {
			if (x>=0 && x<orf.getNumPositions())
				asum = asum+orf.getProfile(cond,x)+0.1;
			if (x==p) psum = asum;
		}
		return psum/asum;
	}

	@Override
	protected int getRows(PriceOrf orf) {
		return 1;
	}

}
