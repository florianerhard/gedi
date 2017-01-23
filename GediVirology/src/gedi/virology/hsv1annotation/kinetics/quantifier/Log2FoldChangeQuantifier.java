package gedi.virology.hsv1annotation.kinetics.quantifier;

import gedi.core.data.annotation.NameAnnotation;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.lfc.Log2FoldChange;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.functions.EI;

public class Log2FoldChangeQuantifier extends KineticQuantifier {

	private QuantifierSet q1;
	private int index1;
	private QuantifierSet q2;
	private int index2;
	
	private Log2FoldChange foldChange;
	
	public Log2FoldChangeQuantifier(String label,
			String repl, QuantifierSet q1, int index1, QuantifierSet q2, int index2, Log2FoldChange foldChange) {
		super(label,repl, null);
		this.q1 = q1;
		this.index1 = index1;
		this.q2 = q2;
		this.index2 = index2;
		this.foldChange = foldChange;
	}

	@Override
	public NumericArray[] quantify(ReferenceGenomicRegion<?> fullRegion, ReferenceGenomicRegion<NameAnnotation>[] regions) {
		if (regions.length==0) return new NumericArray[0];
		
		NumericArray[] ad = q1.getQuantifiers().get(index1).quantify(fullRegion,q1.getRegionProvider().arr(fullRegion));
		NumericArray[] bd = q2.getQuantifiers().get(index2).quantify(fullRegion,q2.getRegionProvider().arr(fullRegion));
		
		return EI.along(ad).map(i->ratio(ad[i],bd[i])).toArray(NumericArray.class);
	}

	private NumericArray ratio(NumericArray a, NumericArray b) {
		for (int i=0; i<a.length(); i++)
			a.set(i, foldChange.computeFoldChange(a.getDouble(i), b.getDouble(i)));
		return a;
	}
	
	@Override
	public NumericArray normalize(NumericArray data) {
		return data;
	}

}
