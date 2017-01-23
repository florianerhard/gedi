package gedi.riboseq.visu;


import gedi.core.data.mapper.GenomicRegionDataMapping;
import gedi.core.reference.Strand;
import gedi.gui.genovis.pixelMapping.PixelBlockToValuesMap;
import gedi.riboseq.inference.orf.Orf;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;

@GenomicRegionDataMapping(fromType=IntervalTree.class,toType=PixelBlockToValuesMap.class)
public class OrfEstimatedCodons extends OrfToScore {

	public OrfEstimatedCodons(Strand strand) {
		super(strand);
	}

	@Override
	protected double getScore(Orf orf, int p) {
		double re = 0;
		double[][] m = orf.getEstimatedCodons();
		for (int c=0; c<m.length; c++)
			re+=m[c][p];
		return re;
	}


}
