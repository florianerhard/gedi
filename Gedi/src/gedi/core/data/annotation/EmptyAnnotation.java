package gedi.core.data.annotation;


import java.io.IOException;

import cern.colt.Arrays;
import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;

public class EmptyAnnotation implements BinarySerializable {
	
	public static EmptyAnnotation instance = new EmptyAnnotation();
	
	
	public EmptyAnnotation() {
	}

	@Override
	public void serialize(BinaryWriter out) throws IOException {
	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
	}
	
	@Override
	public String toString() {
		return "";
	}

}
