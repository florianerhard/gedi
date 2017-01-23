package gedi.core.data.numeric;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import org.broad.igv.bbfile.BBFileReader;
import org.broad.igv.bbfile.BigWigIterator;
import org.broad.igv.bbfile.WigItem;

import gedi.core.data.numeric.GenomicNumericProvider.PositionNumericIterator;
import gedi.core.reference.ReferenceSequence;
import gedi.core.region.GenomicRegion;

public class BigWigGenomicNumericProvider implements GenomicNumericProvider {

	private BBFileReader wig;
	
	public BigWigGenomicNumericProvider(String path) throws IOException {
		wig = new BBFileReader(path);
	}
	
	
	@Override
	public int getLength(String name) {
		int id = wig.getChromosomeID(name);
		return wig.getChromosomeBounds(id, id).getEndBase();
	}

	@Override
	public int getNumDataRows() {
		return 1;
	}

	@Override
	public double getValue(ReferenceSequence reference, int pos, int row) {
		BigWigIterator it = wig.getBigWigIterator(reference.getName(), pos, reference.getName(), pos+1, true);
		if (it.hasNext()) return it.next().getWigValue();
		return Double.NaN;
	}

	@Override
	public PositionNumericIterator iterateValues(ReferenceSequence reference,
			GenomicRegion region) {
		return new PositionNumericIterator() {
			private float val;
			int p = 0;
			BigWigIterator eit = wig.getBigWigIterator(reference.getName(),region.getStart(p), reference.getName(),region.getEnd(p),true);
			
			private void checkNextPart() {
				while (!eit.hasNext() && ++p<region.getNumParts()) {
					eit = wig.getBigWigIterator(reference.getName(),region.getStart(p), reference.getName(),region.getEnd(p),true);
				}
			}
			
			@Override
			public boolean hasNext() {
				checkNextPart();
				return eit.hasNext();
			}
			
			@Override
			public int nextInt() {
				checkNextPart();
				WigItem e = eit.next();
				val = e.getWigValue();
				return e.getStartBase();
			}
			
			@Override
			public double getValue(int row) {
				return val;
			}
			
			@Override
			public double[] getValues(double[] re) {
				if (re==null || re.length!=1) 
					re = new double[1];
				re[0] = val;
				return re;
			}
		};

	}

}
