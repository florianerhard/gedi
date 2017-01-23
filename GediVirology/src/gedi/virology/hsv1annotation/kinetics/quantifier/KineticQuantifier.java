package gedi.virology.hsv1annotation.kinetics.quantifier;

import gedi.core.data.annotation.NameAnnotation;
import gedi.core.region.ReferenceGenomicRegion;
import gedi.lfc.Downsampling;
import gedi.lfc.downsampling.LogscDownsampling;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.functions.EI;

import java.util.logging.Logger;

public abstract class KineticQuantifier {
	
	protected static final Logger log = Logger.getLogger( KineticQuantifier.class.getName() );
	
	protected String label;
	protected NumericArray sizeFactors;
	protected double medSf;
	protected Downsampling downsampling = new LogscDownsampling();
	protected String replicate;
	
	public KineticQuantifier(String label, String repl, NumericArray sizeFactors) {
		this.label = label;
		this.replicate = repl;
		this.sizeFactors = sizeFactors;
		this.medSf = sizeFactors!=null?NumericArrayFunction.Median.applyAsDouble(sizeFactors):Double.NaN;
	}
	
	public String getLabel() {
		return label;
	}
	
	public String getReplicate() {
		return replicate;
	}
	
	public NumericArray getSizeFactors() {
		return sizeFactors;
	}
	
	public abstract NumericArray[] quantify(ReferenceGenomicRegion<?> fullRegion, ReferenceGenomicRegion<NameAnnotation>[] regions);

	public NumericArray normalize(NumericArray data) {
		NumericArray re = NumericArray.createMemory(data.length(), NumericArrayType.Double);
		for (int i=0; i<re.length(); i++)
			re.setDouble(i, data.getDouble(i)/sizeFactors.getDouble(i)*medSf);
		return re;
	}
	
	public NumericArray[] normalize(NumericArray[] data) {
		return EI.wrap(data).map(d->normalize(d)).toArray(NumericArray.class);
	}
	
}
