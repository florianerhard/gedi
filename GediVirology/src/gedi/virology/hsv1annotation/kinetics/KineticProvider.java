package gedi.virology.hsv1annotation.kinetics;

import gedi.core.region.ReferenceGenomicRegion;
import gedi.lfc.Downsampling;
import gedi.lfc.downsampling.LogscDownsampling;
import gedi.util.datastructure.array.NumericArray;
import gedi.util.datastructure.array.NumericArray.NumericArrayType;
import gedi.util.datastructure.array.functions.NumericArrayFunction;
import gedi.util.functions.ExtendedIterator;

public abstract class KineticProvider {
	
	protected NumericArray sizeFactors;
	protected double medSf;
	protected Downsampling downsampling = new LogscDownsampling();
	protected String type;
	protected String replicate;
	
	public KineticProvider(String type, String repl, NumericArray sizeFactors) {
		this.type = type;
		this.replicate = repl;
		this.sizeFactors = sizeFactors;
		this.medSf = NumericArrayFunction.Median.applyAsDouble(sizeFactors);
	}
	
	public String getType() {
		return type;
	}
	public String getReplicate() {
		return replicate;
	}

	public NumericArray getSizeFactors() {
		return sizeFactors;
	}
	
	public abstract ExtendedIterator<ReferenceGenomicRegion<? extends NumericArray>> ei(ReferenceGenomicRegion<?> region);

	public NumericArray normalize(NumericArray data) {
		NumericArray re = NumericArray.createMemory(data.length(), NumericArrayType.Double);
		for (int i=0; i<re.length(); i++)
			re.setDouble(i, data.getDouble(i)/sizeFactors.getDouble(i)*medSf);
		return re;
	}
	
}