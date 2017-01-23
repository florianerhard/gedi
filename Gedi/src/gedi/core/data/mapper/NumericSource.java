package gedi.core.data.mapper;

import gedi.core.data.numeric.BigWigGenomicNumericProvider;
import gedi.core.data.numeric.GenomicNumericProvider;
import gedi.core.data.numeric.GenomicNumericProvider.SpecialAggregators;
import gedi.core.data.numeric.diskrmq.DiskGenomicNumericProvider;
import gedi.core.reference.Chromosome;
import gedi.core.reference.ReferenceSequence;
import gedi.core.reference.Strand;
import gedi.core.region.ArrayGenomicRegion;
import gedi.core.region.GenomicRegion;
import gedi.gui.genovis.pixelMapping.PixelBlockToValuesMap;
import gedi.gui.genovis.pixelMapping.PixelLocationMapping;
import gedi.gui.genovis.pixelMapping.PixelLocationMappingBlock;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.functions.EI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;

@GenomicRegionDataMapping(fromType=Void.class,toType=PixelBlockToValuesMap.class)
public class NumericSource implements GenomicRegionDataSource<PixelBlockToValuesMap> {

	private Strand filter;
	private ArrayList<ProviderSet> providers = new ArrayList<ProviderSet>();
	private HashMap<String,String> nameMapping = new HashMap<String, String>();
	
	
	public NumericSource() {
	}
	public NumericSource(Strand filter) {
		this.filter = filter;
	}
	
	
	
	private HashMap<String,DiskGenomicNumericProvider> fileCache = new HashMap<String, DiskGenomicNumericProvider>();
	private Function<String,DiskGenomicNumericProvider> newDisk = f->{
		try {
			return new DiskGenomicNumericProvider(f);
		}catch(IOException e) {
			throw new RuntimeException(e);
		}
	};

	private HashMap<String,BigWigGenomicNumericProvider> bbCache = new HashMap<String, BigWigGenomicNumericProvider>();
	private Function<String,BigWigGenomicNumericProvider> newBw = f->{
		try {
			return new BigWigGenomicNumericProvider(f);
		}catch(IOException e) {
			throw new RuntimeException(e);
		}
	};
	
	public void map(String incoming, String here) {
		nameMapping.put(incoming, here);
	}
	

	
	public void addRmq(String file, boolean min, boolean mean, boolean sum, boolean max) throws IOException {
		if (min) providers.add(new ProviderSet(fileCache.computeIfAbsent(file, newDisk),SpecialAggregators.Min));
		if (mean) providers.add(new ProviderSet(fileCache.computeIfAbsent(file, newDisk),SpecialAggregators.Mean));
		if (sum) providers.add(new ProviderSet(fileCache.computeIfAbsent(file, newDisk),SpecialAggregators.Sum));
		if (max) providers.add(new ProviderSet(fileCache.computeIfAbsent(file, newDisk),SpecialAggregators.Max));
	}
	
	public void addRmq(String file, SpecialAggregators agg) throws IOException {
		providers.add(new ProviderSet(fileCache.computeIfAbsent(file, newDisk),agg));
	}

	public void addBigWig(String file, SpecialAggregators agg) throws IOException {
		providers.add(new ProviderSet(bbCache.computeIfAbsent(file, newBw),agg));
	}

	
	@Override
	public PixelBlockToValuesMap get(ReferenceSequence reference, GenomicRegion region,PixelLocationMapping pixelMapping) {

		if (nameMapping.containsKey(reference.getName())) 
			reference = Chromosome.obtain(nameMapping.get(reference.getName()), reference.getStrand());

		
		int size = 0;
		for (ProviderSet ps : providers) {
			size+=ps.provider.getNumDataRows();
		}
		
		ReferenceSequence[] refs = reference.getStrand()==Strand.Independent?
				new ReferenceSequence[]{reference,reference.toPlusStrand(),reference.toMinusStrand()}:
				new ReferenceSequence[]{reference};
		if (filter!=null) 
			refs = EI.wrap(refs).filter(ref->ref.getStrand().equals(filter)).toArray(ReferenceSequence.class);
		
		PixelBlockToValuesMap re = new PixelBlockToValuesMap(pixelMapping, size, NumericArrayType.Double);

		for (int b=0; b<pixelMapping.size(); b++) {
			PixelLocationMappingBlock block = pixelMapping.get(b);
			NumericArray vals = re.getValues(b);

			ArrayGenomicRegion reg = new ArrayGenomicRegion(block.getStartBp(),block.getStopBp()+1);
			
			int index = 0;
			
			for (ProviderSet ps : providers) {
				for (int r=0; r<ps.provider.getNumDataRows(); r++) {
									
					for (ReferenceSequence ref : refs)
						vals.setDouble(index, addNaNSave(vals.getDouble(index),ps.agg.getAggregatedValue(ps.provider,ref, reg, r)));
					index++;
				}
			}
		}



		return re;

	}


	private double addNaNSave(double a, double b) {
		if (Double.isNaN(a) && Double.isNaN(b)) return Double.NaN;
		if (Double.isNaN(a)) return b;
		if (Double.isNaN(b)) return a;
		return a+b;
	}

	
	private static class ProviderSet {
		private GenomicNumericProvider provider;
		private SpecialAggregators agg;
		
		public ProviderSet(GenomicNumericProvider provider, SpecialAggregators agg) {
			super();
			this.provider = provider;
			this.agg = agg;
		}
		
	}

	private String id = null;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public <T> void applyForAll(Class<T> cls, Consumer<T> consumer) {
		for (ProviderSet p : providers)
			if (cls.isInstance(p.provider))
				consumer.accept(cls.cast(p.provider));
	}

}
