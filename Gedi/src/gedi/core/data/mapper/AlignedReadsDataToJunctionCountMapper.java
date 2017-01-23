package gedi.core.data.mapper;


import gedi.core.data.reads.AlignedReadsData;
import gedi.core.data.reads.ReadCountMode;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.core.region.MissingInformationIntronInformation;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.tree.redblacktree.IntervalTree;

@GenomicRegionDataMapping(fromType=IntervalTree.class,toType=IntervalTree.class)
public class AlignedReadsDataToJunctionCountMapper implements GenomicRegionDataMapper<IntervalTree<GenomicRegion,AlignedReadsData>, IntervalTree<GenomicRegion,NumericArray>>{

	
	private ReadCountMode readCountMode = ReadCountMode.Weight;
	
	public void setReadCountMode(ReadCountMode readCountMode) {
		this.readCountMode = readCountMode;
	}

	@Override
	public IntervalTree<GenomicRegion,NumericArray> map(ReferenceSequence reference,
			GenomicRegion region,PixelLocationMapping pixelMapping,
			IntervalTree<GenomicRegion, AlignedReadsData> data) {
		if (data.isEmpty()) return new IntervalTree<GenomicRegion,NumericArray>(data.getReference());
		
		int numCond = data.firstEntry().getValue().getNumConditions();
		
		IntervalTree<GenomicRegion,NumericArray> re = new IntervalTree<GenomicRegion,NumericArray>(data.getReference());

		data.entrySet().iterator().forEachRemaining(e->{
			if (e.getKey().getNumParts()>1) {
				for (int i=0; i<e.getKey().getNumParts()-1; i++) {
					if (e.getKey() instanceof MissingInformationIntronInformation && ((MissingInformationIntronInformation)e.getKey()).isMissingInformationIntron(i)){}
					else {
						ArrayGenomicRegion reg = new ArrayGenomicRegion(e.getKey().getEnd(i),e.getKey().getStart(i+1));
						
						NumericArray a = re.computeIfAbsent(reg, k->NumericArray.createMemory(numCond, NumericArrayType.Double));
						AlignedReadsData val = e.getValue();
						val.addTotalCountsForConditions(a, readCountMode);
						
					}
				}
			}
		});
		
		
		return re;
	}



}
