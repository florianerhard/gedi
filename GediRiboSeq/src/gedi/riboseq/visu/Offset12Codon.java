package gedi.riboseq.visu;

import gedi.core.data.mapper.GenomicRegionDataMapper;
import gedi.core.data.mapper.GenomicRegionDataMapping;
import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ContrastMapping;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.GenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelBlockToValuesMap;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.riboseq.utils.RiboUtils;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

@GenomicRegionDataMapping(fromType=IntervalTree.class,toType=PixelBlockToValuesMap.class)
public class Offset12Codon implements GenomicRegionDataMapper<IntervalTree<GenomicRegion,AlignedReadsData>,PixelBlockToValuesMap> {

	
	private ContrastMapping mapping;
	private boolean merge = true;
	
	private Strand fixedStrand;
	
	public Offset12Codon() throws IOException {
		this(null);
	}
	
	public Offset12Codon(Strand strand) throws IOException {
		this.fixedStrand = strand;
	}
	
	public void setMerge(boolean merge) {
		this.merge = merge;
	}
			
	private void setMerge(boolean merge, AlignedReadsData data){
		if (merge) {
			mapping = new ContrastMapping();
			int c = data.getNumConditions();
			for (int i=0; i<c; i++)
				mapping.addMapping(i, 0);
		} else {
			mapping = new ContrastMapping();
			int c = data.getNumConditions();
			for (int i=0; i<c; i++)
				mapping.addMapping(i, i);
		}
	}

	@Override
	public PixelBlockToValuesMap map(ReferenceSequence reference,
			GenomicRegion region, PixelLocationMapping pixelMapping,
			IntervalTree<GenomicRegion, AlignedReadsData> data) {
		
		
		if (data.isEmpty()) return new PixelBlockToValuesMap(pixelMapping, 0, NumericArrayType.Double);
		
		if (mapping==null) setMerge(merge,data.values().iterator().next());
		
		Strand strand = fixedStrand;
		if (strand==null && reference.getStrand()==Strand.Independent)
			throw new RuntimeException("Set fixed strand!");
		
		if (strand==null) strand = reference.getStrand();
		
		
		
		
		double[][] full = new double[mapping.getNumMergedConditions()][region.getTotalLength()];
		
		Iterator<Entry<GenomicRegion, AlignedReadsData>> it = data.entrySet().iterator();
		while (it.hasNext()) {
			Entry<GenomicRegion, AlignedReadsData> n = it.next();
			if (n.getKey().getTotalLength()==28 || n.getKey().getTotalLength()==29) {
				for (int c=0; c<n.getValue().getNumConditions(); c++)
					for (int d=0; d<n.getValue().getDistinctSequences(); d++) {
						int offset = RiboUtils.hasLeadingMismatch(n.getValue(), d)&&RiboUtils.isLeadingMismatchInsideGenomicRegion(n.getValue(), d)?13:12;
						if (strand==Strand.Minus && n.getKey().getTotalLength()-2-1-offset<0)
							continue; // can happen for 15bp long reads with leading mismatch (codon would be at 13,14,15)
						int pos = n.getKey().map(strand==Strand.Minus?n.getKey().getTotalLength()-2-1-offset:offset);
						if (region.contains(pos))
							full[mapping.getMappedIndex(c)][region.induce(pos)]+=n.getValue().getCount(d, c)/n.getValue().getMultiplicity(d);
							
				}
			}
			
		}
		
		
		
		PixelBlockToValuesMap re = new PixelBlockToValuesMap(pixelMapping, 3*mapping.getNumMergedConditions(), NumericArrayType.Double);
		
		for (int p=0; p<region.getTotalLength(); p++) {
			int frame = region.map(p)%3;
			int ind = re.getBlockIndex(reference, region.map(p));
			NumericArray val = re.getValues(ind);
			
			for (int c=0; c<full.length; c++) {
				val.setDouble(c*3+frame, Math.max(val.getDouble(c*3+frame),full[c][p]));
			}
		}
		for (int i=1; i<re.getBlocks().size(); i++) {
			if (re.getBlock(i).getBasePairs()==1 && re.getBlock(i-1).getBasePairs()==1) {
				int bp = re.getBlock(i).getStartBp();
				if (bp%3!=0) 
					for (int c=0; c<re.getValues(i).length(); c+=3)
						re.getValues(i).setDouble(c+0, re.getValues(i-1).getDouble(c+0));
				
				if (bp%3!=1) 
					for (int c=0; c<re.getValues(i).length(); c+=3)
						re.getValues(i).setDouble(c+1, re.getValues(i-1).getDouble(c+1));
				
				if (bp%3!=2) 
					for (int c=0; c<re.getValues(i).length(); c+=3)
						re.getValues(i).setDouble(c+2, re.getValues(i-1).getDouble(c+2));
			}
		}
		
		return re;
		
		
	}

}
