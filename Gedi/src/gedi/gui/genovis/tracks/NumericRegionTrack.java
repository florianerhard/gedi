package gedi.gui.genovis.tracks;

import gedi.core.region.GenomicRegion;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;

public abstract class NumericRegionTrack extends NumericTrack<IntervalTree<GenomicRegion,NumericArray>> {

	
		
	public NumericRegionTrack() {
		super((Class)IntervalTree.class);
	}
	
	
	@Override
	public boolean isEmptyData(IntervalTree<GenomicRegion, NumericArray> data) {
		return data.isEmpty();
	}
	
	@Override
	protected  double computeCurrentMin(IntervalTree<GenomicRegion,NumericArray> values) {
		double min = this.fixedMin;
		if (Double.isNaN(min) && values.size()>0) {
			for (NumericArray a : values.values()) {
				double c = rowOpMax.applyAsDouble(a);
				if (Double.isNaN(min) || c<min)
					min = c;
			}
		}
		if (isLogScale()) 
			min = Math.log(min+1)/Math.log(getLogbase());
		return min;
	}
	@Override
	protected double computeCurrentMax(IntervalTree<GenomicRegion,NumericArray> values) {
		double max = this.fixedMax;
		if (Double.isNaN(max) && values.size()>0) {
			for (NumericArray a : values.values()) {
				double c = rowOpMax.applyAsDouble(a);
				if (Double.isNaN(max) || c>max)
					max = c;
			}
		}
		if (isLogScale()) 
			max = Math.log(max+1)/Math.log(getLogbase());
		return max;
	}

	@Override
	protected void renderPass(
			TrackRenderContext<IntervalTree<GenomicRegion, NumericArray>> context,
			int pass) {
		context.data.entrySet().iterator().forEachRemaining(e->renderValue(context,e.getKey(),e.getValue(),pass));
	}

	

	protected abstract int renderValue(
			TrackRenderContext<IntervalTree<GenomicRegion,NumericArray>> context,
			GenomicRegion region, NumericArray value,
			int pass);


	
	
}
