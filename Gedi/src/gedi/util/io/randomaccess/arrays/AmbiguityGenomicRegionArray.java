package gedi.util.io.randomaccess.arrays;


import java.io.IOException;

import gedi.core.region.MutableReferenceGenomicRegion;
import gedi.util.FileUtils;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializableArray;

public class AmbiguityGenomicRegionArray extends BinarySerializableArray<MutableReferenceGenomicRegion<Integer>> {

	
	private double[] weights;
	
	public AmbiguityGenomicRegionArray() {
		this(new MutableReferenceGenomicRegion[0]);
	}
	
	public AmbiguityGenomicRegionArray(MutableReferenceGenomicRegion<Integer>[] a) {
		super((Class<MutableReferenceGenomicRegion<Integer>>) a.getClass().getComponentType(), ()->new MutableReferenceGenomicRegion<Integer>().setData(0));
		setArray(a);
		weights = new double[a.length];
	}
	
	public double getWeight(int index) {
		return weights[index];
	}
	
	public void setWeight(int index, double weight) {
		weights[index] = weight;
	}
	
	@Override
	public void serialize(BinaryWriter out) throws IOException {
		super.serialize(out);
		for (int i=0; i<weights.length; i++)
			out.putDouble(weights[i]);
	}
	
	@Override
	public void deserialize(BinaryReader in) throws IOException {
		super.deserialize(in);
		weights = new double[getArray().length];
		for (int i=0; i<weights.length; i++)
			weights[i] = in.getDouble();
	}

}
