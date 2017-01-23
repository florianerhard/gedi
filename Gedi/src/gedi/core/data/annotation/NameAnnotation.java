package gedi.core.data.annotation;


import java.io.IOException;

import gedi.util.io.randomaccess.BinaryReader;
import gedi.util.io.randomaccess.BinaryWriter;
import gedi.util.io.randomaccess.serialization.BinarySerializable;

public class NameAnnotation implements BinarySerializable, NameProvider {
	
	private String name;
	
	public NameAnnotation() {
	}
	
	public NameAnnotation(String name) {
		this.name = name;
	}

	@Override
	public void serialize(BinaryWriter out) throws IOException {
		out.putString(name);
	}

	@Override
	public void deserialize(BinaryReader in) throws IOException {
		name = in.getString();
	}
	
	@Override
	public String toString() {
		return name;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}

}
