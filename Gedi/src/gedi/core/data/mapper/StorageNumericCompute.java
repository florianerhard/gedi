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
