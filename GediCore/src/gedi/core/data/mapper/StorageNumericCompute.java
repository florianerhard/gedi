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

import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.tree.redblacktree.Interval;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;

import java.util.Map.Entry;
import java.util.function.UnaryOperator;

@GenomicRegionDataMapping(fromType=IntervalTree.class,toType=IntervalTree.class)
public class StorageNumericCompute implements GenomicRegionDataMapper<IntervalTree<?,NumericArray>, IntervalTree<?,NumericArray>>{

	
	private UnaryOperator<NumericArray> computer = t->t;
	
	

	public StorageNumericCompute(UnaryOperator<NumericArray> computer) {
		this.computer = computer;
	}

	@Override
	public IntervalTree<?,NumericArray> map(ReferenceSequence reference,
			GenomicRegion region,PixelLocationMapping pixelMapping,
			IntervalTree<?,NumericArray> data) {
		
		IntervalTree<Interval,NumericArray> re = new IntervalTree<Interval, NumericArray>(data.getReference());
		for (Entry<? extends Interval, NumericArray> e : data.entrySet()) {
			NumericArray a = computer.apply(e.getValue());
			re.put(e.getKey(), a);
		}
		return re;
		
	}


	
}
