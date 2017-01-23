package gedi.virology.hsv1annotation.kinetics.quantifier;

import gedi.core.data.annotation.NameAnnotation;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.riboseq.inference.codon.Codon;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.functions.ExtendedIterator;

import java.util.function.Function;

public class AverageRiboSeqCoverageQuantifier extends KineticQuantifier {

	private Function<ReferenceGenomicRegion<?>,ExtendedIterator<Codon>> codons;
	
	public AverageRiboSeqCoverageQuantifier(String label,
			String repl, NumericArray sizeFactors,Function<ReferenceGenomicRegion<?>,ExtendedIterator<Codon>> codons) {
		super(label,repl, sizeFactors);
		this.codons = codons;
	}

	@Override
	public NumericArray[] quantify(ReferenceGenomicRegion<?> fullRegion, ReferenceGenomicRegion<NameAnnotation>[] regions) {
		if (regions.length==0) return new NumericArray[0];
		
		NumericArray[] re = new NumericArray[regions.length];
		
		double[] buff = null;
		
		for (Codon c : codons.apply(fullRegion).loop()) {
			if (re[0]==null)
				for (int i=0; i<re.length; i++)
					re[i] = NumericArray.createMemory(c.getActivity().length, NumericArrayType.Double);
			
			buff = downsampling.getDownsampled(c.getActivity(), buff);
			
			// identify the Orfs consistent with c
			for (int i=0; i<regions.length; i++) {
				if (regions[i].getRegion().containsUnspliced(c) && regions[i].induce(c).getStart()%3==0) {
					for (int j=0; j<buff.length; j++)
						re[i].add(j, buff[j]);
				}
			}
		}

		if (buff==null) {
			for (int i=0; i<re.length; i++) {
				re[i] = NumericArray.createMemory(sizeFactors.length(), NumericArrayType.Double);
				for (int c=0; c<re[i].length(); c++)
					re[i].setDouble(c, Double.NaN);
			}
					
		} else {
			for (int c=0; c<buff.length; c++) {
				for (int i=0; i<re.length; i++)
					re[i].setDouble(c, re[i].getDouble(c)/regions[i].getRegion().getTotalLength()*3);
			}
		}
		
		return re;
	}
	

}
